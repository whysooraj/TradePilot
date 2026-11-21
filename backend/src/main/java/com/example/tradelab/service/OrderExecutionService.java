package com.example.tradelab.service;

import com.example.tradelab.entity.*;
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
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderExecutionService implements MarketDataService.OrderMatchingListener {

    private final OrderRepository orderRepository;
    private final TradeRepository tradeRepository;
    private final WalletRepository walletRepository;
    private final HoldingRepository holdingRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    @Override
    @Transactional
    public void onQuoteTick(StockQuote quote) {
        List<OrderEntity> openLimitOrders = orderRepository.findOpenLimitOrders(OrderStatus.OPEN);
        if (openLimitOrders.isEmpty()) return;

        Instant now = Instant.now();

        for (OrderEntity order : openLimitOrders) {
            if (!order.getStock().getSymbol().equalsIgnoreCase(quote.symbol())) {
                continue;
            }

            try {
                if (order.getSide() == OrderSide.BUY) {
                    BigDecimal ask = quote.askPrice() != null ? quote.askPrice() : quote.lastTradedPrice();
                    // Buy limit triggers if ask <= limitPrice
                    if (ask.compareTo(order.getLimitPrice()) <= 0) {
                        executeLimitBuy(order, ask, now);
                    }
                } else if (order.getSide() == OrderSide.SELL) {
                    BigDecimal bid = quote.bidPrice() != null ? quote.bidPrice() : quote.lastTradedPrice();
                    // Sell limit triggers if bid >= limitPrice
                    if (bid.compareTo(order.getLimitPrice()) >= 0) {
                        executeLimitSell(order, bid, now);
                    }
                }
            } catch (Exception e) {
                log.error("Failed to execute limit order {}: {}", order.getId(), e.getMessage(), e);
            }
        }
    }

    private void executeLimitBuy(OrderEntity order, BigDecimal executionPrice, Instant now) {
        UUID userId = order.getUser().getId();
        BigDecimal reservedAmount = order.getLimitPrice().multiply(BigDecimal.valueOf(order.getQuantity())).setScale(4, RoundingMode.HALF_UP);
        BigDecimal actualCost = executionPrice.multiply(BigDecimal.valueOf(order.getQuantity())).setScale(4, RoundingMode.HALF_UP);
        BigDecimal priceImprovementRefund = reservedAmount.subtract(actualCost);

        WalletEntity wallet = walletRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new IllegalStateException("Wallet not found: " + userId));

        // Release reserved cash
        wallet.setLockedBalance(wallet.getLockedBalance().subtract(reservedAmount).max(BigDecimal.ZERO));
        if (priceImprovementRefund.compareTo(BigDecimal.ZERO) > 0) {
            wallet.setAvailableBalance(wallet.getAvailableBalance().add(priceImprovementRefund));
        }
        walletRepository.save(wallet);

        // Record ledger entry
        walletTransactionRepository.save(WalletTransactionEntity.builder()
                .id(UUID.randomUUID())
                .wallet(wallet)
                .order(order)
                .transactionType(TransactionType.BUY_DEBIT)
                .amount(actualCost.negate())
                .createdAt(now)
                .description("Limit Buy filled " + order.getQuantity() + " " + order.getStock().getSymbol() + " @ ₹" + executionPrice)
                .build());

        // Update holding
        HoldingEntity holding = holdingRepository.findByUserIdAndStockIdForUpdate(userId, order.getStock().getId())
                .orElse(HoldingEntity.builder()
                        .id(UUID.randomUUID())
                        .user(order.getUser())
                        .stock(order.getStock())
                        .quantity(0)
                        .averagePrice(BigDecimal.ZERO.setScale(4))
                        .updatedAt(now)
                        .build());

        int oldQty = holding.getQuantity();
        BigDecimal oldCost = holding.getAveragePrice().multiply(BigDecimal.valueOf(oldQty));
        int newQty = oldQty + order.getQuantity();
        BigDecimal newAvgPrice = oldCost.add(actualCost).divide(BigDecimal.valueOf(newQty), 4, RoundingMode.HALF_UP);

        holding.setQuantity(newQty);
        holding.setAveragePrice(newAvgPrice);
        holding.setUpdatedAt(now);
        holdingRepository.save(holding);

        order.setStatus(OrderStatus.FILLED);
        order.setUpdatedAt(now);
        orderRepository.save(order);

        tradeRepository.save(TradeEntity.builder()
                .id(UUID.randomUUID())
                .order(order)
                .executedPrice(executionPrice)
                .executedQuantity(order.getQuantity())
                .totalAmount(actualCost)
                .executedAt(now)
                .build());

        log.info("Limit BUY order {} filled at ₹{}", order.getId(), executionPrice);
    }

    private void executeLimitSell(OrderEntity order, BigDecimal executionPrice, Instant now) {
        UUID userId = order.getUser().getId();
        BigDecimal totalProceeds = executionPrice.multiply(BigDecimal.valueOf(order.getQuantity())).setScale(4, RoundingMode.HALF_UP);

        WalletEntity wallet = walletRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new IllegalStateException("Wallet not found: " + userId));

        wallet.setAvailableBalance(wallet.getAvailableBalance().add(totalProceeds));
        walletRepository.save(wallet);

        walletTransactionRepository.save(WalletTransactionEntity.builder()
                .id(UUID.randomUUID())
                .wallet(wallet)
                .order(order)
                .transactionType(TransactionType.SELL_CREDIT)
                .amount(totalProceeds)
                .createdAt(now)
                .description("Limit Sell filled " + order.getQuantity() + " " + order.getStock().getSymbol() + " @ ₹" + executionPrice)
                .build());

        order.setStatus(OrderStatus.FILLED);
        order.setUpdatedAt(now);
        orderRepository.save(order);

        tradeRepository.save(TradeEntity.builder()
                .id(UUID.randomUUID())
                .order(order)
                .executedPrice(executionPrice)
                .executedQuantity(order.getQuantity())
                .totalAmount(totalProceeds)
                .executedAt(now)
                .build());

        log.info("Limit SELL order {} filled at ₹{}", order.getId(), executionPrice);
    }
}
