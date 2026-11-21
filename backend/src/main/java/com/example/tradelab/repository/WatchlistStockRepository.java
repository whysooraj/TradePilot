package com.example.tradelab.repository;

import com.example.tradelab.entity.WatchlistStockEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WatchlistStockRepository extends JpaRepository<WatchlistStockEntity, UUID> {
    List<WatchlistStockEntity> findByWatchlistId(UUID watchlistId);
    Optional<WatchlistStockEntity> findByWatchlistIdAndStockId(UUID watchlistId, UUID stockId);
    boolean existsByWatchlistIdAndStockId(UUID watchlistId, UUID stockId);
}
