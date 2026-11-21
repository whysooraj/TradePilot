package com.example.tradelab.dto;

import com.example.tradelab.entity.DataMode;
import com.example.tradelab.entity.OrderSide;
import com.example.tradelab.entity.OrderStatus;
import com.example.tradelab.entity.OrderType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class OrderDtos {

    public record OrderRequest(
            @NotBlank(message = "Stock symbol is required")
            String symbol,

            String exchange, // Defaults to NSE if null

            @NotNull(message = "Order side (BUY/SELL) is required")
            OrderSide side,

            @NotNull(message = "Order type (MARKET/LIMIT) is required")
            OrderType orderType,

            @NotNull(message = "Quantity is required")
            @Min(value = 1, message = "Quantity must be at least 1")
            Integer quantity,

            BigDecimal limitPrice,

            String idempotencyKey
    ) {}

    public record OrderResponse(
            UUID orderId,
            String symbol,
            String exchange,
            OrderSide side,
            OrderType orderType,
            Integer quantity,
            BigDecimal limitPrice,
            OrderStatus status,
            BigDecimal executedPrice,
            BigDecimal totalAmount,
            DataMode dataMode,
            Instant createdAt,
            Instant updatedAt
    ) {}

    public record TradeDto(
            UUID tradeId,
            UUID orderId,
            String symbol,
            String exchange,
            OrderSide side,
            BigDecimal executedPrice,
            Integer executedQuantity,
            BigDecimal totalAmount,
            Instant executedAt
    ) {}
}
