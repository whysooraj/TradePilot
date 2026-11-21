package com.example.tradelab.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record CandleDto(
        long time, // Epoch seconds for TradingView Lightweight charts
        BigDecimal open,
        BigDecimal high,
        BigDecimal low,
        BigDecimal close,
        long volume
) {}
