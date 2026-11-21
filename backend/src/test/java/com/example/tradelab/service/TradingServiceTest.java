package com.example.tradelab.service;

import com.example.tradelab.dto.AuthDtos.RegisterRequest;
import com.example.tradelab.dto.OrderDtos.OrderRequest;
import com.example.tradelab.dto.OrderDtos.OrderResponse;
import com.example.tradelab.dto.PortfolioDtos.PortfolioSummaryDto;
import com.example.tradelab.entity.*;
import com.example.tradelab.exception.InsufficientBalanceException;
import com.example.tradelab.exception.InsufficientHoldingsException;
import com.example.tradelab.repository.StockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class TradingServiceTest {

    @Autowired
    private TradingService tradingService;

    @Autowired
    private AuthService authService;

    @Autowired
    private PortfolioService portfolioService;

    @Autowired
    private StockRepository stockRepository;

    private UUID testUserId;

    @BeforeEach
    void setUp() {
        // Seed stock if needed in H2 in-memory test DB
        if (stockRepository.findBySymbolIgnoreCase("RELIANCE").isEmpty()) {
            stockRepository.save(StockEntity.builder()
                    .id(UUID.randomUUID())
                    .symbol("RELIANCE")
                    .companyName("Reliance Industries Ltd.")
                    .exchange("NSE")
                    .active(true)
                    .build());
        }

        String username = "trader_" + System.currentTimeMillis();
        var authRes = authService.register(new RegisterRequest(username, username + "@example.com", "Password@123"));
        testUserId = authRes.userId();
    }

    @Test
    @DisplayName("Market buy with sufficient cash succeeds and updates wallet and holdings")
    void testMarketBuySuccess() {
        OrderRequest request = new OrderRequest(
                "RELIANCE",
                "NSE",
                OrderSide.BUY,
                OrderType.MARKET,
                10,
                null,
                "idemp-key-1"
        );

        OrderResponse response = tradingService.placeOrder(testUserId, request);
        assertNotNull(response);
        assertEquals(OrderStatus.FILLED, response.status());
        assertEquals("RELIANCE", response.symbol());
        assertEquals(10, response.quantity());
        assertNotNull(response.executedPrice());

        PortfolioSummaryDto portfolio = portfolioService.getPortfolioSummary(testUserId);
        assertFalse(portfolio.holdings().isEmpty());
        assertEquals(10, portfolio.holdings().get(0).quantity());
        assertTrue(portfolio.availableCash().compareTo(new BigDecimal("100000.0000")) < 0);
    }

    @Test
    @DisplayName("Market buy with insufficient cash is rejected")
    void testMarketBuyInsufficientCash() {
        OrderRequest request = new OrderRequest(
                "RELIANCE",
                "NSE",
                OrderSide.BUY,
                OrderType.MARKET,
                10000, // Requires ~₹2.95 Crore, user only has ₹1 Lakh
                null,
                null
        );

        assertThrows(InsufficientBalanceException.class, () -> {
            tradingService.placeOrder(testUserId, request);
        });
    }

    @Test
    @DisplayName("Market sell without owning shares is rejected")
    void testMarketSellWithoutShares() {
        OrderRequest request = new OrderRequest(
                "RELIANCE",
                "NSE",
                OrderSide.SELL,
                OrderType.MARKET,
                5,
                null,
                null
        );

        assertThrows(InsufficientHoldingsException.class, () -> {
            tradingService.placeOrder(testUserId, request);
        });
    }

    @Test
    @DisplayName("Idempotent order returns existing order without double-executing")
    void testIdempotentOrder() {
        String key = "test-idemp-" + UUID.randomUUID();
        OrderRequest request = new OrderRequest(
                "RELIANCE",
                "NSE",
                OrderSide.BUY,
                OrderType.MARKET,
                2,
                null,
                key
        );

        OrderResponse res1 = tradingService.placeOrder(testUserId, request);
        OrderResponse res2 = tradingService.placeOrder(testUserId, request);

        assertEquals(res1.orderId(), res2.orderId());
        PortfolioSummaryDto portfolio = portfolioService.getPortfolioSummary(testUserId);
        assertEquals(2, portfolio.holdings().get(0).quantity()); // Only 2 shares, not 4
    }
}
