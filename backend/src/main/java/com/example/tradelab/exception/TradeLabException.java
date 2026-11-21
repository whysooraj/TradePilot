package com.example.tradelab.exception;

import org.springframework.http.HttpStatus;

public class TradeLabException extends RuntimeException {
    private final HttpStatus status;

    public TradeLabException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
