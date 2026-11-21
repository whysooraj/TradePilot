package com.example.tradelab.exception;

import org.springframework.http.HttpStatus;

public class InvalidOrderException extends TradeLabException {
    public InvalidOrderException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
