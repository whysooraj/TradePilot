package com.example.tradelab.controller;

import com.example.tradelab.dto.WalletDtos.WalletSummaryDto;
import com.example.tradelab.dto.WalletDtos.WalletTransactionDto;
import com.example.tradelab.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wallet")
@RequiredArgsConstructor
@Tag(name = "Wallet & Ledger", description = "Virtual cash balance and immutable transaction ledger")
public class WalletController {

    private final WalletService walletService;

    @GetMapping
    @Operation(summary = "Get wallet cash balance summary")
    public ResponseEntity<WalletSummaryDto> getWallet(@AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        return ResponseEntity.ok(walletService.getWalletSummary(userId));
    }

    @GetMapping("/transactions")
    @Operation(summary = "Get immutable transaction history / ledger records")
    public ResponseEntity<List<WalletTransactionDto>> getTransactions(@AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        return ResponseEntity.ok(walletService.getTransactions(userId));
    }
}
