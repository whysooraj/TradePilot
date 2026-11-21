import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { StockQuote } from '../types';

export class WebSocketService {
  private client: Client | null = null;
  private onQuoteCallbacks: ((quotes: StockQuote[]) => void)[] = [];
  private isConnected = false;

  constructor() {
    this.init();
  }

  private init() {
    try {
      this.client = new Client({
        webSocketFactory: () => new SockJS('/ws'),
        reconnectDelay: 5000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,
        onConnect: () => {
          this.isConnected = true;
          this.client?.subscribe('/topic/quotes', (message) => {
            try {
              const quotes: StockQuote[] = JSON.parse(message.body);
              this.onQuoteCallbacks.forEach((cb) => cb(quotes));
            } catch (err) {
              console.error('Error parsing live quote message', err);
            }
          });
        },
        onDisconnect: () => {
          this.isConnected = false;
        },
        onStompError: (frame) => {
          console.warn('STOMP protocol error:', frame.headers['message']);
        },
      });

      this.client.activate();
    } catch (err) {
      console.warn('WebSocket connection not available; will use API polling', err);
    }
  }

  public subscribeToQuotes(callback: (quotes: StockQuote[]) => void): () => void {
    this.onQuoteCallbacks.push(callback);
    return () => {
      this.onQuoteCallbacks = this.onQuoteCallbacks.filter((cb) => cb !== callback);
    };
  }

  public getConnected(): boolean {
    return this.isConnected;
  }
}

export const wsService = new WebSocketService();
