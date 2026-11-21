package com.example.tradelab.repository;

import com.example.tradelab.entity.TradeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TradeRepository extends JpaRepository<TradeEntity, UUID> {
    List<TradeEntity> findByOrderId(UUID orderId);

    @Query("SELECT t FROM TradeEntity t JOIN t.order o WHERE o.user.id = :userId ORDER BY t.executedAt DESC")
    List<TradeEntity> findByUserIdOrderByExecutedAtDesc(@Param("userId") UUID userId);
}
