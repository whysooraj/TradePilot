package com.example.tradelab.repository;

import com.example.tradelab.entity.HoldingEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HoldingRepository extends JpaRepository<HoldingEntity, UUID> {

    List<HoldingEntity> findByUserId(UUID userId);

    Optional<HoldingEntity> findByUserIdAndStockId(UUID userId, UUID stockId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT h FROM HoldingEntity h WHERE h.user.id = :userId AND h.stock.id = :stockId")
    Optional<HoldingEntity> findByUserIdAndStockIdForUpdate(@Param("userId") UUID userId, @Param("stockId") UUID stockId);
}
