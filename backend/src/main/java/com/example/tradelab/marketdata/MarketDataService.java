package com.example.tradelab.marketdata;

import com.example.tradelab.dto.CandleDto;
import com.example.tradelab.entity.FeedStatus;
import com.example.tradelab.exception.StaleMarketPriceException;
import com.example.tradelab.exception.StockNotFoundException;
import com.example.tradelab.websocket.QuoteBroadcaster;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarketDataService {

    private final MarketDataProvider provider;
    private final QuoteBroadcaster broadcaster;
    private final ObjectProvider<OrderMatchingListener> orderMatchingListener;

    @Value("${app.trading.max-quote-age-seconds:60}")
    private long maxQuoteAgeSeconds;

    public interface OrderMatchingListener {
        void onQuoteTick(StockQuote quote);
    }

    public List<StockQuote> getAllQuotes() {
        return new ArrayList<>(provider.getAllQuotes().values());
    }

    public StockQuote getQuote(String symbol) {
        return provider.getLatestQuote(symbol)
                .orElseThrow(() -> new StockNotFoundException("Quote not found for symbol: " + symbol));
    }

    public void validateQuoteFreshness(StockQuote quote) {
        if (quote.feedStatus() != FeedStatus.FRESH) {
            throw new StaleMarketPriceException("Market feed is currently " + quote.feedStatus() + " for " + quote.symbol());
        }
        Instant cutoff = Instant.now().minus(Duration.ofSeconds(maxQuoteAgeSeconds));
        if (quote.receivedAt().isBefore(cutoff)) {
            throw new StaleMarketPriceException("Quote price for " + quote.symbol() + " is stale (older than " + maxQuoteAgeSeconds + "s)");
        }
    }

    public List<CandleDto> getHistoricalCandles(String symbol, int count) {
        StockQuote currentQuote = getQuote(symbol);
        BigDecimal currentPrice = currentQuote.lastTradedPrice();
        long nowSeconds = Instant.now().getEpochSecond();
        long intervalSeconds = 300; // 5-minute bars

        List<CandleDto> candles = new ArrayList<>(count);
        BigDecimal runningClose = currentPrice.multiply(new BigDecimal("0.95")).setScale(2, RoundingMode.HALF_UP);

        long startSeconds = nowSeconds - ((long) count * intervalSeconds);
        for (int i = 0; i < count; i++) {
            long barTime = startSeconds + ((long) i * intervalSeconds);
            double delta = ThreadLocalRandom.current().nextDouble(-0.008, 0.0085);
            BigDecimal barOpen = runningClose;
            BigDecimal barClose = barOpen.multiply(BigDecimal.valueOf(1.0 + delta)).setScale(2, RoundingMode.HALF_UP);
            BigDecimal highDelta = barOpen.max(barClose).multiply(BigDecimal.valueOf(ThreadLocalRandom.current().nextDouble(0.001, 0.005)));
            BigDecimal lowDelta = barOpen.min(barClose).multiply(BigDecimal.valueOf(ThreadLocalRandom.current().nextDouble(0.001, 0.005)));
            BigDecimal barHigh = barOpen.max(barClose).add(highDelta).setScale(2, RoundingMode.HALF_UP);
            BigDecimal barLow = barOpen.min(barClose).subtract(lowDelta).setScale(2, RoundingMode.HALF_UP);
            long volume = ThreadLocalRandom.current().nextLong(5000, 80000);

            candles.add(new CandleDto(barTime, barOpen, barHigh, barLow, barClose, volume));
            runningClose = barClose;
        }

        // Align last candle close with current live LTP
        if (!candles.isEmpty()) {
            CandleDto last = candles.get(candles.size() - 1);
            candles.set(candles.size() - 1, new CandleDto(last.time(), last.open(), last.high().max(currentPrice), last.low().min(currentPrice), currentPrice, last.volume()));
        }

        return candles;
    }

    @Scheduled(fixedRateString = "${app.market-data.tick-interval-ms:2000}")
    public void tickMarket() {
        provider.tick();
        Collection<StockQuote> quotes = provider.getAllQuotes().values();
        broadcaster.broadcastQuotes(quotes);

        OrderMatchingListener listener = orderMatchingListener.getIfAvailable();
        if (listener != null) {
            for (StockQuote quote : quotes) {
                try {
                    listener.onQuoteTick(quote);
                } catch (Exception e) {
                    log.error("Error executing pending order matching for quote: {}", quote.symbol(), e);
                }
            }
        }
    }
}
