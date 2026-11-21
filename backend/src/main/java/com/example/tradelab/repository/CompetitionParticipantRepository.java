package com.example.tradelab.repository;

import com.example.tradelab.entity.CompetitionParticipantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CompetitionParticipantRepository extends JpaRepository<CompetitionParticipantEntity, UUID> {
    List<CompetitionParticipantEntity> findByCompetitionId(UUID competitionId);
    Optional<CompetitionParticipantEntity> findByCompetitionIdAndUserId(UUID competitionId, UUID userId);
    boolean existsByCompetitionIdAndUserId(UUID competitionId, UUID userId);
}
