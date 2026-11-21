package com.example.tradelab.exception;

import org.springframework.http.HttpStatus;

public class StaleMarketPriceException extends TradeLabException {
    public StaleMarketPriceException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
