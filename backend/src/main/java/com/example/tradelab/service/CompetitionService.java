package com.example.tradelab.service;

import com.example.tradelab.dto.CompetitionDtos.*;
import com.example.tradelab.dto.PortfolioDtos.PortfolioSummaryDto;
import com.example.tradelab.entity.*;
import com.example.tradelab.exception.DuplicateOrderException;
import com.example.tradelab.exception.ResourceNotFoundException;
import com.example.tradelab.repository.CompetitionParticipantRepository;
import com.example.tradelab.repository.CompetitionRepository;
import com.example.tradelab.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CompetitionService {

    private final CompetitionRepository competitionRepository;
    private final CompetitionParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final PortfolioService portfolioService;

    @Transactional
    public CompetitionDto createCompetition(CreateCompetitionRequest request) {
        CompetitionEntity entity = CompetitionEntity.builder()
                .id(UUID.randomUUID())
                .name(request.name().trim())
                .startingBalance(request.startingBalance() != null ? request.startingBalance() : new BigDecimal("100000.0000"))
                .startTime(request.startTime())
                .endTime(request.endTime())
                .status(CompetitionStatus.ACTIVE)
                .build();
        competitionRepository.save(entity);

        return new CompetitionDto(
                entity.getId(),
                entity.getName(),
                entity.getStartingBalance(),
                entity.getStartTime(),
                entity.getEndTime(),
                entity.getStatus(),
                0,
                false
        );
    }

    @Transactional(readOnly = true)
    public List<CompetitionDto> listCompetitions(UUID currentUserId) {
        List<CompetitionEntity> all = competitionRepository.findAll();
        List<CompetitionDto> result = new ArrayList<>();

        for (CompetitionEntity c : all) {
            long count = participantRepository.findByCompetitionId(c.getId()).size();
            boolean hasJoined = currentUserId != null && participantRepository.existsByCompetitionIdAndUserId(c.getId(), currentUserId);
            result.add(new CompetitionDto(
                    c.getId(),
                    c.getName(),
                    c.getStartingBalance(),
                    c.getStartTime(),
                    c.getEndTime(),
                    c.getStatus(),
                    count,
                    hasJoined
            ));
        }
        return result;
    }

    @Transactional
    public void joinCompetition(UUID competitionId, UUID userId) {
        CompetitionEntity competition = competitionRepository.findById(competitionId)
                .orElseThrow(() -> new ResourceNotFoundException("Competition not found: " + competitionId));

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        if (participantRepository.existsByCompetitionIdAndUserId(competitionId, userId)) {
            throw new DuplicateOrderException("User has already joined this competition");
        }

        CompetitionParticipantEntity participant = CompetitionParticipantEntity.builder()
                .id(UUID.randomUUID())
                .competition(competition)
                .user(user)
                .joinedAt(Instant.now())
                .build();

        participantRepository.save(participant);
    }

    @Transactional(readOnly = true)
    public List<LeaderboardEntryDto> getLeaderboard(UUID competitionId) {
        List<CompetitionParticipantEntity> participants = participantRepository.findByCompetitionId(competitionId);
        List<LeaderboardEntryDto> entries = new ArrayList<>();

        for (CompetitionParticipantEntity p : participants) {
            PortfolioSummaryDto summary = portfolioService.getPortfolioSummary(p.getUser().getId());
            entries.add(new LeaderboardEntryDto(
                    0,
                    p.getUser().getId(),
                    p.getUser().getUsername(),
                    summary.totalAccountValue(),
                    summary.returnPercentage()
            ));
        }

        // Sort descending by totalAccountValue
        entries.sort(Comparator.comparing(LeaderboardEntryDto::totalAccountValue).reversed());

        List<LeaderboardEntryDto> ranked = new ArrayList<>(entries.size());
        for (int i = 0; i < entries.size(); i++) {
            LeaderboardEntryDto raw = entries.get(i);
            ranked.add(new LeaderboardEntryDto(
                    i + 1,
                    raw.userId(),
                    raw.username(),
                    raw.totalAccountValue(),
                    raw.returnPercentage()
            ));
        }

        return ranked;
    }
}
