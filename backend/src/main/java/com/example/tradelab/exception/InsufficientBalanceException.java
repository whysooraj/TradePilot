package com.example.tradelab.exception;

import org.springframework.http.HttpStatus;

public class InsufficientBalanceException extends TradeLabException {
    public InsufficientBalanceException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
