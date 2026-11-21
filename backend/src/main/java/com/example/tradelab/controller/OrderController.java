package com.example.tradelab.controller;

import com.example.tradelab.dto.OrderDtos.*;
import com.example.tradelab.service.TradingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Trading & Orders", description = "Simulated order placement, cancellation and trade history")
public class OrderController {

    private final TradingService tradingService;

    @PostMapping("/orders")
    @Operation(summary = "Place a simulated market or limit order")
    public ResponseEntity<OrderResponse> placeOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody OrderRequest request
    ) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        return ResponseEntity.ok(tradingService.placeOrder(userId, request));
    }

    @GetMapping("/orders")
    @Operation(summary = "List all orders placed by the current user")
    public ResponseEntity<List<OrderResponse>> getUserOrders(@AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        return ResponseEntity.ok(tradingService.getUserOrders(userId));
    }

    @DeleteMapping("/orders/{id}")
    @Operation(summary = "Cancel an open pending limit order")
    public ResponseEntity<OrderResponse> cancelOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id
    ) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        return ResponseEntity.ok(tradingService.cancelOrder(userId, id));
    }

    @GetMapping("/trades")
    @Operation(summary = "List all completed executions / trades for the current user")
    public ResponseEntity<List<TradeDto>> getUserTrades(@AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        return ResponseEntity.ok(tradingService.getUserTrades(userId));
    }
}
