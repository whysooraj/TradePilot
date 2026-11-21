package com.example.tradelab.dto;

import com.example.tradelab.entity.CompetitionStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class CompetitionDtos {

    public record CreateCompetitionRequest(
            @NotBlank(message = "Name is required")
            String name,

            BigDecimal startingBalance,

            @NotNull(message = "Start time is required")
            Instant startTime,

            @NotNull(message = "End time is required")
            Instant endTime
    ) {}

    public record CompetitionDto(
            UUID id,
            String name,
            BigDecimal startingBalance,
            Instant startTime,
            Instant endTime,
            CompetitionStatus status,
            long participantCount,
            boolean hasJoined
    ) {}

    public record LeaderboardEntryDto(
            int rank,
            UUID userId,
            String username,
            BigDecimal totalAccountValue,
            BigDecimal returnPercentage
    ) {}
}
