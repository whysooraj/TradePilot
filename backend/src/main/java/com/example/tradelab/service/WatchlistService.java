package com.example.tradelab.service;

import com.example.tradelab.entity.*;
import com.example.tradelab.exception.DuplicateOrderException;
import com.example.tradelab.exception.ResourceNotFoundException;
import com.example.tradelab.exception.StockNotFoundException;
import com.example.tradelab.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WatchlistService {

    private final WatchlistRepository watchlistRepository;
    private final WatchlistStockRepository watchlistStockRepository;
    private final StockRepository stockRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<String> getUserWatchlistSymbols(UUID userId) {
        WatchlistEntity watchlist = getOrCreateWatchlist(userId);
        return watchlistStockRepository.findByWatchlistId(watchlist.getId()).stream()
                .map(ws -> ws.getStock().getSymbol())
                .toList();
    }

    @Transactional
    public void addSymbol(UUID userId, String symbol) {
        WatchlistEntity watchlist = getOrCreateWatchlist(userId);
        StockEntity stock = stockRepository.findBySymbolIgnoreCase(symbol.trim())
                .orElseThrow(() -> new StockNotFoundException("Stock not found: " + symbol));

        if (watchlistStockRepository.existsByWatchlistIdAndStockId(watchlist.getId(), stock.getId())) {
            throw new DuplicateOrderException("Stock is already in watchlist");
        }

        WatchlistStockEntity entry = WatchlistStockEntity.builder()
                .id(UUID.randomUUID())
                .watchlist(watchlist)
                .stock(stock)
                .addedAt(Instant.now())
                .build();
        watchlistStockRepository.save(entry);
    }

    @Transactional
    public void removeSymbol(UUID userId, String symbol) {
        WatchlistEntity watchlist = getOrCreateWatchlist(userId);
        StockEntity stock = stockRepository.findBySymbolIgnoreCase(symbol.trim())
                .orElseThrow(() -> new StockNotFoundException("Stock not found: " + symbol));

        watchlistStockRepository.findByWatchlistIdAndStockId(watchlist.getId(), stock.getId())
                .ifPresent(watchlistStockRepository::delete);
    }

    private WatchlistEntity getOrCreateWatchlist(UUID userId) {
        return watchlistRepository.findFirstByUserId(userId)
                .orElseGet(() -> {
                    UserEntity user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
                    WatchlistEntity wl = WatchlistEntity.builder()
                            .id(UUID.randomUUID())
                            .user(user)
                            .name("My Watchlist")
                            .createdAt(Instant.now())
                            .build();
                    return watchlistRepository.save(wl);
                });
    }
}
