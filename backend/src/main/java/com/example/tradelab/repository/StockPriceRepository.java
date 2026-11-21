package com.example.tradelab.repository;

import com.example.tradelab.entity.StockPriceEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StockPriceRepository extends JpaRepository<StockPriceEntity, Long> {
    List<StockPriceEntity> findByStockIdOrderByRecordedAtDesc(UUID stockId, Pageable pageable);
}
