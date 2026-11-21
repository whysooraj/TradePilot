package com.example.tradelab.exception;

import org.springframework.http.HttpStatus;

public class InsufficientHoldingsException extends TradeLabException {
    public InsufficientHoldingsException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
