package com.example.tradelab.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "stocks", uniqueConstraints = {
    @UniqueConstraint(name = "uq_stock_symbol_exchange", columnNames = {"symbol", "exchange"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 20)
    private String symbol;

    @Column(name = "company_name", nullable = false, length = 100)
    private String companyName;

    @Column(nullable = false, length = 10)
    private String exchange;

    @Column(name = "provider_instrument_token", length = 50)
    private String providerInstrumentToken;

    @Column(nullable = false)
    private boolean active;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }
}
