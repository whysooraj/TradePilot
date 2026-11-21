package com.example.tradelab.repository;

import com.example.tradelab.entity.PortfolioSnapshotEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PortfolioSnapshotRepository extends JpaRepository<PortfolioSnapshotEntity, UUID> {
    List<PortfolioSnapshotEntity> findByUserIdOrderByRecordedAtDesc(UUID userId, Pageable pageable);
}
