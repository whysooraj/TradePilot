package com.example.tradelab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TradeLabApplication {
    public static void main(String[] args) {
        SpringApplication.run(TradeLabApplication.class, args);
    }
}
