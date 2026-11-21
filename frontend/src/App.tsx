import React, { useState, useEffect, useCallback } from 'react';
import { Header } from './components/Header';
import { DashboardView } from './components/DashboardView';
import { ExploreView } from './components/ExploreView';
import { PortfolioView } from './components/PortfolioView';
import { OrdersView } from './components/OrdersView';
import { CompetitionsView } from './components/CompetitionsView';
import { WatchlistView } from './components/WatchlistView';
import { TradeTicketModal } from './components/TradeTicketModal';
import { AuthModal } from './components/AuthModal';
import {
  stockApi,
  portfolioApi,
  orderApi,
  watchlistApi,
  getAuthToken,
  getStoredUser,
  clearAuthToken,
  clearStoredUser,
} from './services/api';
import { wsService } from './services/websocket';
import { AuthState, Candle, Order, PortfolioSummary, StockQuote, Trade } from './types';

export const App: React.FC = () => {
  const [activeTab, setActiveTab] = useState<string>('dashboard');
  const [quotes, setQuotes] = useState<StockQuote[]>([]);
  const [portfolio, setPortfolio] = useState<PortfolioSummary | null>(null);
  const [orders, setOrders] = useState<Order[]>([]);
  const [trades, setTrades] = useState<Trade[]>([]);
  const [watchlistSymbols, setWatchlistSymbols] = useState<string[]>([]);

  // Trade Ticket & Chart Modal
  const [ticketQuote, setTicketQuote] = useState<StockQuote | null>(null);
  const [ticketCandles, setTicketCandles] = useState<Candle[]>([]);

  // Auth State
  const [authState, setAuthState] = useState<AuthState>(() => {
    const token = getAuthToken();
    const user = getStoredUser();
    return { token, user };
  });
  const [isAuthModalOpen, setIsAuthModalOpen] = useState(false);

  // Load Quotes
  const loadQuotes = useCallback(async () => {
    try {
      const data = await stockApi.list();
      setQuotes(data);
    } catch (err) {
      console.warn('Failed to fetch stock quotes', err);
    }
  }, []);

  // Load Portfolio & Orders
  const loadUserData = useCallback(async () => {
    if (!authState.token) return;
    try {
      const [portfolioData, ordersData, tradesData, watchlistData] = await Promise.all([
        portfolioApi.getSummary().catch(() => null),
        orderApi.list().catch(() => []),
        orderApi.trades().catch(() => []),
        watchlistApi.get().catch(() => []),
      ]);

      if (portfolioData) setPortfolio(portfolioData);
      setOrders(ordersData);
      setTrades(tradesData);
      setWatchlistSymbols(watchlistData.map((w) => w.symbol));
    } catch (err) {
      console.error('Failed to load user trading data', err);
    }
  }, [authState.token]);

  // Initial load and WebSocket subscription
  useEffect(() => {
    loadQuotes();

    const unsubscribeWs = wsService.subscribeToQuotes((updatedQuotes) => {
      setQuotes(updatedQuotes);
    });

    // Fallback polling interval every 3 seconds
    const interval = setInterval(() => {
      loadQuotes();
    }, 3000);

    return () => {
      unsubscribeWs();
      clearInterval(interval);
    };
  }, [loadQuotes]);

  // Sync user data when token changes
  useEffect(() => {
    if (authState.token) {
      loadUserData();
    } else {
      setPortfolio(null);
      setOrders([]);
      setTrades([]);
    }
  }, [authState.token, loadUserData]);

  // Handle stock selection for Trading Ticket & Interactive Chart
  const handleSelectStock = async (symbol: string) => {
    const quote = quotes.find((q) => q.symbol.toUpperCase() === symbol.toUpperCase());
    if (!quote) return;

    setTicketQuote(quote);
    try {
      const candles = await stockApi.getHistory(symbol, 60);
      setTicketCandles(candles);
    } catch {
      setTicketCandles([]);
    }
  };

  const handleToggleWatchlist = async (symbol: string) => {
    if (!authState.token) {
      setIsAuthModalOpen(true);
      return;
    }

    try {
      if (watchlistSymbols.includes(symbol)) {
        await watchlistApi.remove(symbol);
        setWatchlistSymbols((prev) => prev.filter((s) => s !== symbol));
      } else {
        await watchlistApi.add(symbol);
        setWatchlistSymbols((prev) => [...prev, symbol]);
      }
    } catch (err) {
      console.error('Failed to toggle watchlist symbol', err);
    }
  };

  const handleLogout = () => {
    clearAuthToken();
    clearStoredUser();
    setAuthState({ token: null, user: null });
  };

  const availableCash = portfolio ? portfolio.availableCash : authState.user?.availableCash || 100000;
  const userHolding = portfolio?.holdings?.find((h) => h.symbol === ticketQuote?.symbol);

  return (
    <div className="min-h-screen bg-background flex flex-col">
      {/* Top Navigation */}
      <Header
        activeTab={activeTab}
        setActiveTab={setActiveTab}
        authState={authState}
        availableCash={availableCash}
        onOpenAuth={() => setIsAuthModalOpen(true)}
        onLogout={handleLogout}
      />

      {/* Main Container */}
      <main className="flex-1 max-w-7xl w-full mx-auto px-4 py-6">
        {activeTab === 'dashboard' && (
          <DashboardView
            portfolio={portfolio}
            quotes={quotes}
            onSelectStock={handleSelectStock}
            onNavigate={setActiveTab}
          />
        )}

        {activeTab === 'explore' && (
          <ExploreView
            quotes={quotes}
            watchlistSymbols={watchlistSymbols}
            onSelectStock={handleSelectStock}
            onToggleWatchlist={handleToggleWatchlist}
          />
        )}

        {activeTab === 'portfolio' && (
          <PortfolioView
            portfolio={portfolio}
            onTradeStock={handleSelectStock}
            onRefresh={loadUserData}
          />
        )}

        {activeTab === 'orders' && (
          <OrdersView
            orders={orders}
            trades={trades}
            onRefresh={loadUserData}
          />
        )}

        {activeTab === 'competitions' && (
          <CompetitionsView
            onRefreshPortfolio={loadUserData}
          />
        )}

        {activeTab === 'watchlist' && (
          <WatchlistView
            quotes={quotes}
            watchlistSymbols={watchlistSymbols}
            onSelectStock={handleSelectStock}
            onRemoveWatchlist={handleToggleWatchlist}
            onExplore={() => setActiveTab('explore')}
          />
        )}
      </main>

      {/* Trade Ticket Modal with TradingView Chart */}
      {ticketQuote && (
        <TradeTicketModal
          quote={ticketQuote}
          candles={ticketCandles}
          availableCash={availableCash}
          userHolding={userHolding}
          onClose={() => setTicketQuote(null)}
          onOrderComplete={() => {
            loadUserData();
            loadQuotes();
          }}
        />
      )}

      {/* Auth Modal */}
      <AuthModal
        isOpen={isAuthModalOpen}
        onClose={() => setIsAuthModalOpen(false)}
        onAuthSuccess={(auth) => {
          setAuthState(auth);
          loadUserData();
        }}
      />
    </div>
  );
};
export default App;
