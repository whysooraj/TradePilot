package com.example.tradelab.service;

import com.example.tradelab.dto.AuthDtos.*;
import com.example.tradelab.entity.TransactionType;
import com.example.tradelab.entity.UserEntity;
import com.example.tradelab.entity.WalletEntity;
import com.example.tradelab.entity.WalletTransactionEntity;
import com.example.tradelab.entity.WatchlistEntity;
import com.example.tradelab.exception.DuplicateOrderException;
import com.example.tradelab.exception.ResourceNotFoundException;
import com.example.tradelab.repository.UserRepository;
import com.example.tradelab.repository.WalletRepository;
import com.example.tradelab.repository.WalletTransactionRepository;
import com.example.tradelab.repository.WatchlistRepository;
import com.example.tradelab.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final WatchlistRepository watchlistRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${app.trading.initial-wallet-balance:100000.0000}")
    private BigDecimal initialBalance;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateOrderException("Username is already taken");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateOrderException("Email is already registered");
        }

        UserEntity user = UserEntity.builder()
                .id(UUID.randomUUID())
                .username(request.username().trim())
                .email(request.email().trim().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role("ROLE_USER")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        userRepository.save(user);

        // Auto-provision virtual wallet with ₹1,00,000 (Blueprint Section 5.1)
        WalletEntity wallet = WalletEntity.builder()
                .id(UUID.randomUUID())
                .user(user)
                .availableBalance(initialBalance)
                .lockedBalance(BigDecimal.ZERO.setScale(4))
                .updatedAt(Instant.now())
                .build();
        walletRepository.save(wallet);

        // Record initial ledger transaction (immutable audit trail)
        WalletTransactionEntity initialTx = WalletTransactionEntity.builder()
                .id(UUID.randomUUID())
                .wallet(wallet)
                .transactionType(TransactionType.INITIAL_CREDIT)
                .amount(initialBalance)
                .createdAt(Instant.now())
                .description("Initial virtual paper trading deposit of ₹" + initialBalance)
                .build();
        walletTransactionRepository.save(initialTx);

        // Provision default watchlist
        WatchlistEntity watchlist = WatchlistEntity.builder()
                .id(UUID.randomUUID())
                .user(user)
                .name("My Watchlist")
                .createdAt(Instant.now())
                .build();
        watchlistRepository.save(watchlist);

        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername(), user.getRole());

        return new AuthResponse(
                token,
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                wallet.getAvailableBalance()
        );
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String identifier = request.usernameOrEmail().trim();
        UserEntity user = userRepository.findByUsername(identifier)
                .or(() -> userRepository.findByEmail(identifier.toLowerCase()))
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid username or password");
        }

        WalletEntity wallet = walletRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for user: " + user.getId()));

        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername(), user.getRole());

        return new AuthResponse(
                token,
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                wallet.getAvailableBalance()
        );
    }

    @Transactional(readOnly = true)
    public UserDto getCurrentUser(UUID userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        return new UserDto(user.getId(), user.getUsername(), user.getEmail(), user.getRole(), user.getCreatedAt());
    }
}
