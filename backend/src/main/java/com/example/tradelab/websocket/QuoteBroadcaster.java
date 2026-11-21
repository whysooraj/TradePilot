package com.example.tradelab.websocket;

import com.example.tradelab.marketdata.StockQuote;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
@RequiredArgsConstructor
public class QuoteBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;

    public void broadcastQuotes(Collection<StockQuote> quotes) {
        messagingTemplate.convertAndSend("/topic/quotes", quotes);
    }

    public void broadcastQuote(StockQuote quote) {
        messagingTemplate.convertAndSend("/topic/quotes/" + quote.symbol(), quote);
    }
}
