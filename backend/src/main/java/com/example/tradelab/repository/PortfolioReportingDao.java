package com.example.tradelab.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Direct JDBC implementation demonstrating low-level SQL reporting queries
 * alongside JPA as required by the TradeLab specification (Section 2, 11.3).
 */
@Repository
@RequiredArgsConstructor
public class PortfolioReportingDao {

    private final JdbcTemplate jdbcTemplate;

    public Map<String, Object> getTradingAnalyticsSql(UUID userId) {
        String sql = """
            SELECT
                COUNT(DISTINCT o.id) AS total_orders,
                COUNT(DISTINCT t.id) AS total_trades,
                COALESCE(SUM(t.total_amount), 0) AS total_turnover,
                COALESCE(MAX(t.total_amount), 0) AS largest_trade,
                COALESCE(SUM(CASE WHEN o.side = 'BUY' THEN t.total_amount ELSE 0 END), 0) AS total_bought,
                COALESCE(SUM(CASE WHEN o.side = 'SELL' THEN t.total_amount ELSE 0 END), 0) AS total_sold,
                COUNT(DISTINCT h.stock_id) AS active_positions
            FROM users u
            LEFT JOIN orders o ON o.user_id = u.id
            LEFT JOIN trades t ON t.order_id = o.id
            LEFT JOIN holdings h ON h.user_id = u.id AND h.quantity > 0
            WHERE u.id = ?
            GROUP BY u.id
        """;

        return jdbcTemplate.query(sql, rs -> {
            if (rs.next()) {
                return Map.of(
                        "totalOrders", rs.getLong("total_orders"),
                        "totalTrades", rs.getLong("total_trades"),
                        "totalTurnover", rs.getBigDecimal("total_turnover"),
                        "largestTrade", rs.getBigDecimal("largest_trade"),
                        "totalBought", rs.getBigDecimal("total_bought"),
                        "totalSold", rs.getBigDecimal("total_sold"),
                        "activePositions", rs.getLong("active_positions")
                );
            }
            return Map.of(
                    "totalOrders", 0L,
                    "totalTrades", 0L,
                    "totalTurnover", BigDecimal.ZERO,
                    "largestTrade", BigDecimal.ZERO,
                    "totalBought", BigDecimal.ZERO,
                    "totalSold", BigDecimal.ZERO,
                    "activePositions", 0L
            );
        }, userId);
    }
}
