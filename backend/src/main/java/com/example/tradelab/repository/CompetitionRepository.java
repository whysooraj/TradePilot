package com.example.tradelab.repository;

import com.example.tradelab.entity.CompetitionEntity;
import com.example.tradelab.entity.CompetitionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CompetitionRepository extends JpaRepository<CompetitionEntity, UUID> {
    List<CompetitionEntity> findByStatus(CompetitionStatus status);
}
