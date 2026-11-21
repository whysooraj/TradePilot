package com.example.tradelab.dto;

import com.example.tradelab.entity.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class WalletDtos {

    public record WalletSummaryDto(
            UUID walletId,
            BigDecimal availableBalance,
            BigDecimal lockedBalance,
            BigDecimal totalBalance,
            Instant updatedAt
    ) {}

    public record WalletTransactionDto(
            UUID id,
            UUID orderId,
            TransactionType transactionType,
            BigDecimal amount,
            Instant createdAt,
            String description
    ) {}
}
