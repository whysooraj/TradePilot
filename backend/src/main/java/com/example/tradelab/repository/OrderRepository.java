package com.example.tradelab.repository;

import com.example.tradelab.entity.OrderEntity;
import com.example.tradelab.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, UUID> {
    List<OrderEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);
    List<OrderEntity> findByStatus(OrderStatus status);

    @Query("SELECT o FROM OrderEntity o WHERE o.status = :status AND o.orderType = 'LIMIT'")
    List<OrderEntity> findOpenLimitOrders(@Param("status") OrderStatus status);

    Optional<OrderEntity> findByUserIdAndIdempotencyKey(UUID userId, String idempotencyKey);
}
