package com.example.tradelab.repository;

import com.example.tradelab.entity.WatchlistEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WatchlistRepository extends JpaRepository<WatchlistEntity, UUID> {
    List<WatchlistEntity> findByUserId(UUID userId);
    Optional<WatchlistEntity> findFirstByUserId(UUID userId);
}
