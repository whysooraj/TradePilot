package com.example.tradelab.marketdata;

import com.example.tradelab.entity.DataMode;
import com.example.tradelab.entity.FeedStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record StockQuote(
        String symbol,
        String companyName,
        String exchange,
        BigDecimal lastTradedPrice,
        BigDecimal bidPrice,
        BigDecimal askPrice,
        BigDecimal open,
        BigDecimal high,
        BigDecimal low,
        BigDecimal previousClose,
        Long volume,
        BigDecimal change,
        BigDecimal changePercent,
        Instant providerTimestamp,
        Instant receivedAt,
        DataMode dataMode,
        FeedStatus feedStatus
) {}
