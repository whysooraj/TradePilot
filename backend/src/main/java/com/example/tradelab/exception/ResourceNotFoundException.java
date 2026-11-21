package com.example.tradelab.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends TradeLabException {
    public ResourceNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}
