package com.example.tradelab.exception;

import org.springframework.http.HttpStatus;

public class StockNotFoundException extends TradeLabException {
    public StockNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}
