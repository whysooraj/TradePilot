package com.example.tradelab.repository;

import com.example.tradelab.entity.StockEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StockRepository extends JpaRepository<StockEntity, UUID> {
    Optional<StockEntity> findBySymbolAndExchange(String symbol, String exchange);
    Optional<StockEntity> findBySymbolIgnoreCase(String symbol);
    List<StockEntity> findByActiveTrue();
}
