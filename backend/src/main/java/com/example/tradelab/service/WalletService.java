package com.example.tradelab.service;

import com.example.tradelab.dto.WalletDtos.WalletSummaryDto;
import com.example.tradelab.dto.WalletDtos.WalletTransactionDto;
import com.example.tradelab.entity.WalletEntity;
import com.example.tradelab.entity.WalletTransactionEntity;
import com.example.tradelab.exception.ResourceNotFoundException;
import com.example.tradelab.repository.WalletRepository;
import com.example.tradelab.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    @Transactional(readOnly = true)
    public WalletSummaryDto getWalletSummary(UUID userId) {
        WalletEntity wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for user: " + userId));

        return new WalletSummaryDto(
                wallet.getId(),
                wallet.getAvailableBalance(),
                wallet.getLockedBalance(),
                wallet.getAvailableBalance().add(wallet.getLockedBalance()),
                wallet.getUpdatedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<WalletTransactionDto> getTransactions(UUID userId) {
        List<WalletTransactionEntity> txs = walletTransactionRepository.findByWalletUserIdOrderByCreatedAtDesc(userId);
        return txs.stream().map(tx -> new WalletTransactionDto(
                tx.getId(),
                tx.getOrder() != null ? tx.getOrder().getId() : null,
                tx.getTransactionType(),
                tx.getAmount(),
                tx.getCreatedAt(),
                tx.getDescription()
        )).toList();
    }
}
