package com.example.tradelab.marketdata;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface MarketDataProvider {
    List<StockQuote> getQuotes(List<String> symbols);
    void subscribe(String symbol);
    void unsubscribe(String symbol);
    Optional<StockQuote> getLatestQuote(String symbol);
    Map<String, StockQuote> getAllQuotes();
    void tick();
}
