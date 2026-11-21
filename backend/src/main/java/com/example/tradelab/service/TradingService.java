package com.example.tradelab.service;

import com.example.tradelab.dto.OrderDtos.*;
import com.example.tradelab.entity.*;
import com.example.tradelab.exception.*;
import com.example.tradelab.marketdata.MarketDataService;
import com.example.tradelab.marketdata.StockQuote;
import com.example.tradelab.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TradingService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final StockRepository stockRepository;
    private final OrderRepository orderRepository;
    private final TradeRepository tradeRepository;
    private final HoldingRepository holdingRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final MarketDataService marketDataService;

    @Transactional
    public OrderResponse placeOrder(UUID userId, OrderRequest request) {
        // 1. Idempotency Check
        if (request.idempotencyKey() != null && !request.idempotencyKey().isBlank()) {
            Optional<OrderEntity> existing = orderRepository.findByUserIdAndIdempotencyKey(userId, request.idempotencyKey());
            if (existing.isPresent()) {
                log.info("Duplicate order request detected with key: {}", request.idempotencyKey());
                return mapOrderToResponse(existing.get(), null);
            }
        }

        // 2. Validate User
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        // 3. Validate Stock
        String symbol = request.symbol().trim().toUpperCase();
        String exchange = request.exchange() != null && !request.exchange().isBlank() ? request.exchange().toUpperCase() : "NSE";
        StockEntity stock = stockRepository.findBySymbolAndExchange(symbol, exchange)
                .or(() -> stockRepository.findBySymbolIgnoreCase(symbol))
                .orElseThrow(() -> new StockNotFoundException("Instrument not found: " + symbol + " on " + exchange));

        if (!stock.isActive()) {
            throw new InvalidOrderException("Trading is currently disabled for " + symbol);
        }

        // 4. Validate Quantity
        if (request.quantity() == null || request.quantity() <= 0) {
            throw new InvalidOrderException("Order quantity must be strictly positive");
        }

        // 5. Validate Market Quote
        StockQuote quote = marketDataService.getQuote(symbol);
        marketDataService.validateQuoteFreshness(quote);

        if (request.orderType() == OrderType.MARKET) {
            return executeMarketOrder(user, stock, quote, request);
        } else if (request.orderType() == OrderType.LIMIT) {
            return placeLimitOrder(user, stock, quote, request);
        } else {
            throw new InvalidOrderException("Unsupported order type: " + request.orderType());
        }
    }

    private OrderResponse executeMarketOrder(UserEntity user, StockEntity stock, StockQuote quote, OrderRequest request) {
        Instant now = Instant.now();
        UUID orderId = UUID.randomUUID();

        if (request.side() == OrderSide.BUY) {
            // Execution price: Use ask price if present, else LTP (Blueprint Section 5.7)
            BigDecimal executionPrice = quote.askPrice() != null ? quote.askPrice() : quote.lastTradedPrice();
            BigDecimal totalCost = executionPrice.multiply(BigDecimal.valueOf(request.quantity())).setScale(4, RoundingMode.HALF_UP);

            // Pessimistic Lock on Wallet to prevent race condition overspending (Blueprint Section 11.2)
            WalletEntity wallet = walletRepository.findByUserIdForUpdate(user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for user: " + user.getId()));

            if (wallet.getAvailableBalance().compareTo(totalCost) < 0) {
                throw new InsufficientBalanceException(
                        "Insufficient virtual cash. Required: ₹" + totalCost + ", Available: ₹" + wallet.getAvailableBalance()
                );
            }

            // Deduct funds
            wallet.setAvailableBalance(wallet.getAvailableBalance().subtract(totalCost));
            walletRepository.save(wallet);

            // Record immutable ledger entry
            WalletTransactionEntity tx = WalletTransactionEntity.builder()
                    .id(UUID.randomUUID())
                    .wallet(wallet)
                    .transactionType(TransactionType.BUY_DEBIT)
                    .amount(totalCost.negate())
                    .createdAt(now)
                    .description("Bought " + request.quantity() + " " + stock.getSymbol() + " @ ₹" + executionPrice)
                    .build();
            walletTransactionRepository.save(tx);

            // Update holdings with weighted-average cost formula (Blueprint Section 7.1)
            HoldingEntity holding = holdingRepository.findByUserIdAndStockIdForUpdate(user.getId(), stock.getId())
                    .orElse(HoldingEntity.builder()
                            .id(UUID.randomUUID())
                            .user(user)
                            .stock(stock)
                            .quantity(0)
                            .averagePrice(BigDecimal.ZERO.setScale(4))
                            .updatedAt(now)
                            .build());

            int oldQty = holding.getQuantity();
            BigDecimal oldCost = holding.getAveragePrice().multiply(BigDecimal.valueOf(oldQty));
            int newQty = oldQty + request.quantity();
            BigDecimal newAvgPrice = oldCost.add(totalCost).divide(BigDecimal.valueOf(newQty), 4, RoundingMode.HALF_UP);

            holding.setQuantity(newQty);
            holding.setAveragePrice(newAvgPrice);
            holding.setUpdatedAt(now);
            holdingRepository.save(holding);

            // Create Order
            OrderEntity order = OrderEntity.builder()
                    .id(orderId)
                    .user(user)
                    .stock(stock)
                    .side(OrderSide.BUY)
                    .orderType(OrderType.MARKET)
                    .quantity(request.quantity())
                    .limitPrice(null)
                    .status(OrderStatus.FILLED)
                    .idempotencyKey(request.idempotencyKey())
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            orderRepository.save(order);

            // Create Trade record
            TradeEntity trade = TradeEntity.builder()
                    .id(UUID.randomUUID())
                    .order(order)
                    .executedPrice(executionPrice)
                    .executedQuantity(request.quantity())
                    .totalAmount(totalCost)
                    .executedAt(now)
                    .build();
            tradeRepository.save(trade);
            tx.setOrder(order);

            return new OrderResponse(
                    order.getId(),
                    stock.getSymbol(),
                    stock.getExchange(),
                    order.getSide(),
                    order.getOrderType(),
                    order.getQuantity(),
                    null,
                    order.getStatus(),
                    executionPrice,
                    totalCost,
                    quote.dataMode(),
                    order.getCreatedAt(),
                    order.getUpdatedAt()
            );

        } else { // SELL
            // Execution price: Use bid price if present, else LTP (Blueprint Section 5.7)
            BigDecimal executionPrice = quote.bidPrice() != null ? quote.bidPrice() : quote.lastTradedPrice();
            BigDecimal totalProceeds = executionPrice.multiply(BigDecimal.valueOf(request.quantity())).setScale(4, RoundingMode.HALF_UP);

            // Pessimistic Lock on Holdings
            HoldingEntity holding = holdingRepository.findByUserIdAndStockIdForUpdate(user.getId(), stock.getId())
                    .orElseThrow(() -> new InsufficientHoldingsException("No holdings found for " + stock.getSymbol()));

            if (holding.getQuantity() < request.quantity()) {
                throw new InsufficientHoldingsException(
                        "Insufficient shares to sell. Owned: " + holding.getQuantity() + ", Requested: " + request.quantity()
                );
            }

            // Deduct shares from holding
            holding.setQuantity(holding.getQuantity() - request.quantity());
            holding.setUpdatedAt(now);
            holdingRepository.save(holding);

            // Pessimistic Lock on Wallet
            WalletEntity wallet = walletRepository.findByUserIdForUpdate(user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for user: " + user.getId()));

            wallet.setAvailableBalance(wallet.getAvailableBalance().add(totalProceeds));
            walletRepository.save(wallet);

            // Record immutable ledger entry
            WalletTransactionEntity tx = WalletTransactionEntity.builder()
                    .id(UUID.randomUUID())
                    .wallet(wallet)
                    .transactionType(TransactionType.SELL_CREDIT)
                    .amount(totalProceeds)
                    .createdAt(now)
                    .description("Sold " + request.quantity() + " " + stock.getSymbol() + " @ ₹" + executionPrice)
                    .build();
            walletTransactionRepository.save(tx);

            // Create Order
            OrderEntity order = OrderEntity.builder()
                    .id(orderId)
                    .user(user)
                    .stock(stock)
                    .side(OrderSide.SELL)
                    .orderType(OrderType.MARKET)
                    .quantity(request.quantity())
                    .limitPrice(null)
                    .status(OrderStatus.FILLED)
                    .idempotencyKey(request.idempotencyKey())
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            orderRepository.save(order);

            // Create Trade record
            TradeEntity trade = TradeEntity.builder()
                    .id(UUID.randomUUID())
                    .order(order)
                    .executedPrice(executionPrice)
                    .executedQuantity(request.quantity())
                    .totalAmount(totalProceeds)
                    .executedAt(now)
                    .build();
            tradeRepository.save(trade);
            tx.setOrder(order);

            return new OrderResponse(
                    order.getId(),
                    stock.getSymbol(),
                    stock.getExchange(),
                    order.getSide(),
                    order.getOrderType(),
                    order.getQuantity(),
                    null,
                    order.getStatus(),
                    executionPrice,
                    totalProceeds,
                    quote.dataMode(),
                    order.getCreatedAt(),
                    order.getUpdatedAt()
            );
        }
    }

    private OrderResponse placeLimitOrder(UserEntity user, StockEntity stock, StockQuote quote, OrderRequest request) {
        if (request.limitPrice() == null || request.limitPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidOrderException("A valid positive limit price is required for LIMIT orders");
        }

        Instant now = Instant.now();
        UUID orderId = UUID.randomUUID();
        BigDecimal limitPrice = request.limitPrice().setScale(4, RoundingMode.HALF_UP);

        if (request.side() == OrderSide.BUY) {
            BigDecimal reservedAmount = limitPrice.multiply(BigDecimal.valueOf(request.quantity())).setScale(4, RoundingMode.HALF_UP);

            WalletEntity wallet = walletRepository.findByUserIdForUpdate(user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Wallet not found: " + user.getId()));

            if (wallet.getAvailableBalance().compareTo(reservedAmount) < 0) {
                throw new InsufficientBalanceException(
                        "Insufficient cash to reserve for limit buy. Required: ₹" + reservedAmount + ", Available: ₹" + wallet.getAvailableBalance()
                );
            }

            // Reserve cash
            wallet.setAvailableBalance(wallet.getAvailableBalance().subtract(reservedAmount));
            wallet.setLockedBalance(wallet.getLockedBalance().add(reservedAmount));
            walletRepository.save(wallet);

            walletTransactionRepository.save(WalletTransactionEntity.builder()
                    .id(UUID.randomUUID())
                    .wallet(wallet)
                    .transactionType(TransactionType.ORDER_RESERVE)
                    .amount(reservedAmount.negate())
                    .createdAt(now)
                    .description("Reserved funds for limit buy " + request.quantity() + " " + stock.getSymbol() + " @ ₹" + limitPrice)
                    .build());

            OrderEntity order = OrderEntity.builder()
                    .id(orderId)
                    .user(user)
                    .stock(stock)
                    .side(OrderSide.BUY)
                    .orderType(OrderType.LIMIT)
                    .quantity(request.quantity())
                    .limitPrice(limitPrice)
                    .status(OrderStatus.OPEN)
                    .idempotencyKey(request.idempotencyKey())
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            orderRepository.save(order);

            return mapOrderToResponse(order, quote.dataMode());

        } else { // LIMIT SELL
            HoldingEntity holding = holdingRepository.findByUserIdAndStockIdForUpdate(user.getId(), stock.getId())
                    .orElseThrow(() -> new InsufficientHoldingsException("No holdings found for " + stock.getSymbol()));

            if (holding.getQuantity() < request.quantity()) {
                throw new InsufficientHoldingsException(
                        "Insufficient shares for limit sell. Owned: " + holding.getQuantity() + ", Requested: " + request.quantity()
                );
            }

            // Lock shares by deducting temporarily from available holdings
            holding.setQuantity(holding.getQuantity() - request.quantity());
            holding.setUpdatedAt(now);
            holdingRepository.save(holding);

            OrderEntity order = OrderEntity.builder()
                    .id(orderId)
                    .user(user)
                    .stock(stock)
                    .side(OrderSide.SELL)
                    .orderType(OrderType.LIMIT)
                    .quantity(request.quantity())
                    .limitPrice(limitPrice)
                    .status(OrderStatus.OPEN)
                    .idempotencyKey(request.idempotencyKey())
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            orderRepository.save(order);

            return mapOrderToResponse(order, quote.dataMode());
        }
    }

    @Transactional
    public OrderResponse cancelOrder(UUID userId, UUID orderId) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        if (!order.getUser().getId().equals(userId)) {
            throw new TradeLabException("Access denied: You do not own this order", org.springframework.http.HttpStatus.FORBIDDEN);
        }

        if (order.getStatus() != OrderStatus.OPEN) {
            throw new InvalidOrderException("Only OPEN orders can be cancelled. Current status: " + order.getStatus());
        }

        Instant now = Instant.now();

        if (order.getSide() == OrderSide.BUY) {
            // Refund reserved funds
            BigDecimal reservedAmount = order.getLimitPrice().multiply(BigDecimal.valueOf(order.getQuantity())).setScale(4, RoundingMode.HALF_UP);
            WalletEntity wallet = walletRepository.findByUserIdForUpdate(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Wallet not found: " + userId));

            wallet.setLockedBalance(wallet.getLockedBalance().subtract(reservedAmount).max(BigDecimal.ZERO));
            wallet.setAvailableBalance(wallet.getAvailableBalance().add(reservedAmount));
            walletRepository.save(wallet);

            walletTransactionRepository.save(WalletTransactionEntity.builder()
                    .id(UUID.randomUUID())
                    .wallet(wallet)
                    .order(order)
                    .transactionType(TransactionType.ORDER_RELEASE)
                    .amount(reservedAmount)
                    .createdAt(now)
                    .description("Released reserved funds for cancelled limit order " + orderId)
                    .build());
        } else {
            // Restore reserved shares
            HoldingEntity holding = holdingRepository.findByUserIdAndStockIdForUpdate(userId, order.getStock().getId())
                    .orElse(HoldingEntity.builder()
                            .id(UUID.randomUUID())
                            .user(order.getUser())
                            .stock(order.getStock())
                            .quantity(0)
                            .averagePrice(order.getLimitPrice())
                            .updatedAt(now)
                            .build());

            holding.setQuantity(holding.getQuantity() + order.getQuantity());
            holding.setUpdatedAt(now);
            holdingRepository.save(holding);
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setUpdatedAt(now);
        orderRepository.save(order);

        return mapOrderToResponse(order, DataMode.SIMULATED);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getUserOrders(UUID userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(o -> mapOrderToResponse(o, DataMode.SIMULATED))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TradeDto> getUserTrades(UUID userId) {
        return tradeRepository.findByUserIdOrderByExecutedAtDesc(userId).stream()
                .map(t -> new TradeDto(
                        t.getId(),
                        t.getOrder().getId(),
                        t.getOrder().getStock().getSymbol(),
                        t.getOrder().getStock().getExchange(),
                        t.getOrder().getSide(),
                        t.getExecutedPrice(),
                        t.getExecutedQuantity(),
                        t.getTotalAmount(),
                        t.getExecutedAt()
                ))
                .toList();
    }

    private OrderResponse mapOrderToResponse(OrderEntity order, DataMode dataMode) {
        List<TradeEntity> trades = tradeRepository.findByOrderId(order.getId());
        BigDecimal execPrice = trades.isEmpty() ? null : trades.get(0).getExecutedPrice();
        BigDecimal totalAmount = trades.isEmpty() ? null : trades.get(0).getTotalAmount();

        return new OrderResponse(
                order.getId(),
                order.getStock().getSymbol(),
                order.getStock().getExchange(),
                order.getSide(),
                order.getOrderType(),
                order.getQuantity(),
                order.getLimitPrice(),
                order.getStatus(),
                execPrice,
                totalAmount,
                dataMode != null ? dataMode : DataMode.SIMULATED,
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
