package com.example.tradelab.exception;

import org.springframework.http.HttpStatus;

public class DuplicateOrderException extends TradeLabException {
    public DuplicateOrderException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}
