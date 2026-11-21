package com.example.tradelab.controller;

import com.example.tradelab.dto.CandleDto;
import com.example.tradelab.marketdata.MarketDataService;
import com.example.tradelab.marketdata.StockQuote;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Market & Stocks", description = "Stock quotes, market status, and historical data")
public class StockController {

    private final MarketDataService marketDataService;

    @GetMapping("/stocks")
    @Operation(summary = "List and search all instruments with real-time quotes")
    public ResponseEntity<List<StockQuote>> listStocks(
            @RequestParam(required = false) String search
    ) {
        List<StockQuote> quotes = marketDataService.getAllQuotes();
        if (search != null && !search.isBlank()) {
            String q = search.toLowerCase();
            quotes = quotes.stream()
                    .filter(s -> s.symbol().toLowerCase().contains(q) || s.companyName().toLowerCase().contains(q))
                    .toList();
        }
        return ResponseEntity.ok(quotes);
    }

    @GetMapping("/stocks/{symbol}")
    @Operation(summary = "Get detailed real-time quote for an instrument")
    public ResponseEntity<StockQuote> getStock(@PathVariable String symbol) {
        return ResponseEntity.ok(marketDataService.getQuote(symbol));
    }

    @GetMapping("/stocks/{symbol}/history")
    @Operation(summary = "Get historical OHLCV candlestick data for TradingView charts")
    public ResponseEntity<List<CandleDto>> getHistory(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "60") int count
    ) {
        return ResponseEntity.ok(marketDataService.getHistoricalCandles(symbol, count));
    }

    @GetMapping("/stocks/top-gainers")
    @Operation(summary = "Get top gaining stocks for the session")
    public ResponseEntity<List<StockQuote>> getTopGainers(@RequestParam(defaultValue = "5") int limit) {
        List<StockQuote> list = marketDataService.getAllQuotes().stream()
                .sorted(Comparator.comparing(StockQuote::changePercent).reversed())
                .limit(limit)
                .toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/stocks/top-losers")
    @Operation(summary = "Get top losing stocks for the session")
    public ResponseEntity<List<StockQuote>> getTopLosers(@RequestParam(defaultValue = "5") int limit) {
        List<StockQuote> list = marketDataService.getAllQuotes().stream()
                .sorted(Comparator.comparing(StockQuote::changePercent))
                .limit(limit)
                .toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/market/status")
    @Operation(summary = "Get exchange and market-data feed status")
    public ResponseEntity<Map<String, Object>> getMarketStatus() {
        return ResponseEntity.ok(Map.of(
                "exchange", "NSE",
                "status", "OPEN",
                "mode", "SIMULATED",
                "feed", "FRESH",
                "timestamp", Instant.now(),
                "serverTimezone", "Asia/Kolkata"
        ));
    }
}
