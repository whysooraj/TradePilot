package com.example.tradelab.controller;

import com.example.tradelab.dto.CompetitionDtos.*;
import com.example.tradelab.service.CompetitionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/competitions")
@RequiredArgsConstructor
@Tag(name = "Competitions & Leaderboards", description = "Paper-trading tournaments and competitive rankings")
public class CompetitionController {

    private final CompetitionService competitionService;

    @GetMapping
    @Operation(summary = "List all competitions and user participation status")
    public ResponseEntity<List<CompetitionDto>> listCompetitions(@AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = userDetails != null ? UUID.fromString(userDetails.getUsername()) : null;
        return ResponseEntity.ok(competitionService.listCompetitions(userId));
    }

    @PostMapping
    @Operation(summary = "Create a new trading competition")
    public ResponseEntity<CompetitionDto> createCompetition(
            @Valid @RequestBody CreateCompetitionRequest request
    ) {
        return ResponseEntity.ok(competitionService.createCompetition(request));
    }

    @PostMapping("/{id}/join")
    @Operation(summary = "Join a trading competition")
    public ResponseEntity<Map<String, String>> joinCompetition(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        competitionService.joinCompetition(id, userId);
        return ResponseEntity.ok(Map.of("message", "Successfully joined competition"));
    }

    @GetMapping("/{id}/leaderboard")
    @Operation(summary = "Get ranked leaderboard standings for a competition")
    public ResponseEntity<List<LeaderboardEntryDto>> getLeaderboard(@PathVariable UUID id) {
        return ResponseEntity.ok(competitionService.getLeaderboard(id));
    }
}
