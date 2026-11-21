package com.example.tradelab.controller;

import com.example.tradelab.dto.PortfolioDtos.*;
import com.example.tradelab.repository.PortfolioReportingDao;
import com.example.tradelab.service.PortfolioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/portfolio")
@RequiredArgsConstructor
@Tag(name = "Portfolio & Accounting", description = "Portfolio valuation, holdings, P&L, and performance")
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final PortfolioReportingDao portfolioReportingDao;

    @GetMapping
    @Operation(summary = "Get complete portfolio summary (account value, cash, unrealized/realized P&L)")
    public ResponseEntity<PortfolioSummaryDto> getPortfolio(@AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        return ResponseEntity.ok(portfolioService.getPortfolioSummary(userId));
    }

    @GetMapping("/holdings")
    @Operation(summary = "Get user holdings with current market value and individual P&L")
    public ResponseEntity<List<HoldingDto>> getHoldings(@AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        return ResponseEntity.ok(portfolioService.getPortfolioSummary(userId).holdings());
    }

    @GetMapping("/performance")
    @Operation(summary = "Get historical account value snapshots for equity curve charting")
    public ResponseEntity<List<SnapshotDto>> getPerformance(@AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        return ResponseEntity.ok(portfolioService.getHistoricalSnapshots(userId));
    }

    @GetMapping("/analytics-sql")
    @Operation(summary = "Direct JDBC execution demonstrating advanced SQL reporting aggregation")
    public ResponseEntity<Map<String, Object>> getAnalyticsSql(@AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        return ResponseEntity.ok(portfolioReportingDao.getTradingAnalyticsSql(userId));
    }
}
