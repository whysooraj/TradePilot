package com.example.tradelab.controller;

import com.example.tradelab.marketdata.MarketDataService;
import com.example.tradelab.marketdata.StockQuote;
import com.example.tradelab.service.WatchlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/watchlist")
@RequiredArgsConstructor
@Tag(name = "Watchlists", description = "User saved instrument watchlists")
public class WatchlistController {

    private final WatchlistService watchlistService;
    private final MarketDataService marketDataService;

    @GetMapping
    @Operation(summary = "Get user's watchlist with live quotes")
    public ResponseEntity<List<StockQuote>> getWatchlist(@AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        List<String> symbols = watchlistService.getUserWatchlistSymbols(userId);
        List<StockQuote> quotes = symbols.stream()
                .map(s -> {
                    try {
                        return marketDataService.getQuote(s);
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();
        return ResponseEntity.ok(quotes);
    }

    @PostMapping("/{symbol}")
    @Operation(summary = "Add a stock symbol to user's watchlist")
    public ResponseEntity<Map<String, String>> addSymbol(
            @PathVariable String symbol,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        watchlistService.addSymbol(userId, symbol);
        return ResponseEntity.ok(Map.of("message", "Symbol added to watchlist"));
    }

    @DeleteMapping("/{symbol}")
    @Operation(summary = "Remove a stock symbol from user's watchlist")
    public ResponseEntity<Map<String, String>> removeSymbol(
            @PathVariable String symbol,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        watchlistService.removeSymbol(userId, symbol);
        return ResponseEntity.ok(Map.of("message", "Symbol removed from watchlist"));
    }
}
