package com.example.tradelab.marketdata;

import com.example.tradelab.entity.DataMode;
import com.example.tradelab.entity.FeedStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class MockMarketDataProvider implements MarketDataProvider {

    private final Map<String, StockQuote> quoteCache = new ConcurrentHashMap<>();
    private final Set<String> subscribedSymbols = ConcurrentHashMap.newKeySet();

    private static final Map<String, InitialStockData> INITIAL_DATA = Map.ofEntries(
            Map.entry("RELIANCE", new InitialStockData("Reliance Industries Ltd.", "NSE", new BigDecimal("2950.00"), 1250000L)),
            Map.entry("TCS", new InitialStockData("Tata Consultancy Services Ltd.", "NSE", new BigDecimal("4120.00"), 850000L)),
            Map.entry("HDFCBANK", new InitialStockData("HDFC Bank Ltd.", "NSE", new BigDecimal("1680.00"), 2100000L)),
            Map.entry("INFY", new InitialStockData("Infosys Ltd.", "NSE", new BigDecimal("1850.00"), 1600000L)),
            Map.entry("ICICIBANK", new InitialStockData("ICICI Bank Ltd.", "NSE", new BigDecimal("1240.00"), 1900000L)),
            Map.entry("TATAMOTORS", new InitialStockData("Tata Motors Passenger Vehicles Ltd.", "NSE", new BigDecimal("980.00"), 3200000L)),
            Map.entry("SBIN", new InitialStockData("State Bank of India", "NSE", new BigDecimal("820.00"), 2800000L)),
            Map.entry("BHARTIARTL", new InitialStockData("Bharti Airtel Ltd.", "NSE", new BigDecimal("1590.00"), 1100000L)),
            Map.entry("ITC", new InitialStockData("ITC Ltd.", "NSE", new BigDecimal("485.00"), 4500000L)),
            Map.entry("LT", new InitialStockData("Larsen & Toubro Ltd.", "NSE", new BigDecimal("3650.00"), 750000L)),
            Map.entry("WIPRO", new InitialStockData("Wipro Ltd.", "NSE", new BigDecimal("540.00"), 1800000L)),
            Map.entry("BAJFINANCE", new InitialStockData("Bajaj Finance Ltd.", "NSE", new BigDecimal("7150.00"), 420000L))
    );

    public MockMarketDataProvider() {
        initQuotes();
    }

    private void initQuotes() {
        Instant now = Instant.now();
        INITIAL_DATA.forEach((symbol, data) -> {
            BigDecimal prevClose = data.basePrice();
            BigDecimal open = prevClose.multiply(BigDecimal.valueOf(1 + (ThreadLocalRandom.current().nextDouble(-0.005, 0.005))))
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal ltp = open;
            BigDecimal spread = ltp.multiply(new BigDecimal("0.0005")).setScale(2, RoundingMode.HALF_UP);
            BigDecimal bid = ltp.subtract(spread);
            BigDecimal ask = ltp.add(spread);
            BigDecimal change = ltp.subtract(prevClose);
            BigDecimal changePercent = change.divide(prevClose, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP);

            StockQuote quote = new StockQuote(
                    symbol,
                    data.companyName(),
                    data.exchange(),
                    ltp.setScale(4, RoundingMode.HALF_UP),
                    bid.setScale(4, RoundingMode.HALF_UP),
                    ask.setScale(4, RoundingMode.HALF_UP),
                    open.setScale(4, RoundingMode.HALF_UP),
                    open.setScale(4, RoundingMode.HALF_UP),
                    open.setScale(4, RoundingMode.HALF_UP),
                    prevClose.setScale(4, RoundingMode.HALF_UP),
                    data.initialVolume(),
                    change.setScale(4, RoundingMode.HALF_UP),
                    changePercent,
                    now,
                    now,
                    DataMode.SIMULATED,
                    FeedStatus.FRESH
            );
            quoteCache.put(symbol, quote);
            subscribedSymbols.add(symbol);
        });
    }

    @Override
    public List<StockQuote> getQuotes(List<String> symbols) {
        return symbols.stream()
                .map(String::toUpperCase)
                .map(quoteCache::get)
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public void subscribe(String symbol) {
        subscribedSymbols.add(symbol.toUpperCase());
    }

    @Override
    public void unsubscribe(String symbol) {
        subscribedSymbols.remove(symbol.toUpperCase());
    }

    @Override
    public Optional<StockQuote> getLatestQuote(String symbol) {
        return Optional.ofNullable(quoteCache.get(symbol.toUpperCase()));
    }

    @Override
    public Map<String, StockQuote> getAllQuotes() {
        return Collections.unmodifiableMap(quoteCache);
    }

    @Override
    public void tick() {
        Instant now = Instant.now();
        for (String symbol : quoteCache.keySet()) {
            StockQuote current = quoteCache.get(symbol);
            if (current == null) continue;

            // Random walk: -0.4% to +0.4% step
            double deltaPercent = ThreadLocalRandom.current().nextDouble(-0.004, 0.0042);
            BigDecimal multiplier = BigDecimal.valueOf(1.0 + deltaPercent);
            BigDecimal newPrice = current.lastTradedPrice().multiply(multiplier).setScale(2, RoundingMode.HALF_UP);

            BigDecimal high = newPrice.max(current.high());
            BigDecimal low = newPrice.min(current.low());
            BigDecimal spread = newPrice.multiply(new BigDecimal("0.0005")).setScale(2, RoundingMode.HALF_UP);
            BigDecimal bid = newPrice.subtract(spread);
            BigDecimal ask = newPrice.add(spread);
            BigDecimal change = newPrice.subtract(current.previousClose());
            BigDecimal changePercent = change.divide(current.previousClose(), 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP);
            long addedVolume = ThreadLocalRandom.current().nextLong(50, 1500);

            StockQuote updated = new StockQuote(
                    symbol,
                    current.companyName(),
                    current.exchange(),
                    newPrice.setScale(4, RoundingMode.HALF_UP),
                    bid.setScale(4, RoundingMode.HALF_UP),
                    ask.setScale(4, RoundingMode.HALF_UP),
                    current.open(),
                    high.setScale(4, RoundingMode.HALF_UP),
                    low.setScale(4, RoundingMode.HALF_UP),
                    current.previousClose(),
                    current.volume() + addedVolume,
                    change.setScale(4, RoundingMode.HALF_UP),
                    changePercent,
                    now,
                    now,
                    DataMode.SIMULATED,
                    FeedStatus.FRESH
            );

            quoteCache.put(symbol, updated);
        }
    }

    private record InitialStockData(String companyName, String exchange, BigDecimal basePrice, Long initialVolume) {}
}
