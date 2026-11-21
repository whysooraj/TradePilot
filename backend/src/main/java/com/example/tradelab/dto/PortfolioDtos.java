package com.example.tradelab.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class PortfolioDtos {

    public record HoldingDto(
            UUID stockId,
            String symbol,
            String companyName,
            String exchange,
            Integer quantity,
            BigDecimal averagePrice,
            BigDecimal currentPrice,
            BigDecimal investedValue,
            BigDecimal marketValue,
            BigDecimal unrealizedPnl,
            BigDecimal returnPercentage
    ) {}

    public record PortfolioSummaryDto(
            BigDecimal availableCash,
            BigDecimal reservedCash,
            BigDecimal investedValue,
            BigDecimal holdingsMarketValue,
            BigDecimal totalAccountValue,
            BigDecimal unrealizedPnl,
            BigDecimal realizedPnl,
            BigDecimal totalPnl,
            BigDecimal returnPercentage,
            List<HoldingDto> holdings
    ) {}

    public record SnapshotDto(
            UUID id,
            BigDecimal totalValue,
            BigDecimal cashBalance,
            BigDecimal holdingsValue,
            Instant recordedAt
    ) {}
}
