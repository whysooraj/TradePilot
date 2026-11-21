package com.example.tradelab.concurrency;

import com.example.tradelab.dto.AuthDtos.RegisterRequest;
import com.example.tradelab.dto.OrderDtos.OrderRequest;
import com.example.tradelab.dto.PortfolioDtos.PortfolioSummaryDto;
import com.example.tradelab.entity.OrderSide;
import com.example.tradelab.entity.OrderType;
import com.example.tradelab.entity.StockEntity;
import com.example.tradelab.exception.InsufficientBalanceException;
import com.example.tradelab.repository.StockRepository;
import com.example.tradelab.service.AuthService;
import com.example.tradelab.service.PortfolioService;
import com.example.tradelab.service.TradingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class ConcurrencyTest {

    @Autowired
    private TradingService tradingService;

    @Autowired
    private AuthService authService;

    @Autowired
    private PortfolioService portfolioService;

    @Autowired
    private StockRepository stockRepository;

    private UUID userId;

    @BeforeEach
    void setUp() {
        if (stockRepository.findBySymbolIgnoreCase("TCS").isEmpty()) {
            stockRepository.save(StockEntity.builder()
                    .id(UUID.randomUUID())
                    .symbol("TCS")
                    .companyName("Tata Consultancy Services Ltd.")
                    .exchange("NSE")
                    .active(true)
                    .build());
        }

        String username = "concurrent_" + System.currentTimeMillis();
        var authRes = authService.register(new RegisterRequest(username, username + "@example.com", "Password@123"));
        userId = authRes.userId();
    }

    @Test
    @DisplayName("Simultaneous concurrent buy orders never overdraft the virtual wallet")
    void testConcurrentPurchasesNeverOverspend() throws InterruptedException {
        int threads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);

        AtomicInteger successfulOrders = new AtomicInteger(0);
        AtomicInteger rejectedOrders = new AtomicInteger(0);

        // TCS is ~₹4,120. 15 shares = ~₹61,800.
        // User has ₹1,00,000. Exactly 1 order can succeed, remaining 9 must be rejected!
        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    tradingService.placeOrder(userId, new OrderRequest(
                            "TCS",
                            "NSE",
                            OrderSide.BUY,
                            OrderType.MARKET,
                            15,
                            null,
                            null
                    ));
                    successfulOrders.incrementAndGet();
                } catch (InsufficientBalanceException e) {
                    rejectedOrders.incrementAndGet();
                } catch (Exception e) {
                    // unexpected error
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // Release all threads concurrently
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS));
        executor.shutdown();

        PortfolioSummaryDto portfolio = portfolioService.getPortfolioSummary(userId);
        assertTrue(portfolio.availableCash().compareTo(BigDecimal.ZERO) >= 0, "Available cash must never be negative!");
        assertEquals(1, successfulOrders.get(), "Only 1 order of ₹61,800 should succeed out of ₹1,00,000 wallet");
        assertEquals(9, rejectedOrders.get(), "9 concurrent orders must be safely rejected due to insufficient balance");
    }
}
