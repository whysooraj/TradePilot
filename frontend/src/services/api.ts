import {
  AuthResponse,
  Candle,
  Competition,
  LeaderboardEntry,
  Order,
  OrderSide,
  OrderType,
  PortfolioSummary,
  StockQuote,
  Trade,
  User,
  WalletSummary,
  WalletTransaction,
} from '../types';

const API_BASE = '/api/v1';

export const getAuthToken = (): string | null => localStorage.getItem('tradelab_token');
export const setAuthToken = (token: string) => localStorage.setItem('tradelab_token', token);
export const clearAuthToken = () => localStorage.removeItem('tradelab_token');

export const getStoredUser = () => {
  const raw = localStorage.getItem('tradelab_user');
  if (!raw) return null;
  try {
    return JSON.parse(raw);
  } catch {
    return null;
  }
};

export const setStoredUser = (user: any) => localStorage.setItem('tradelab_user', JSON.stringify(user));
export const clearStoredUser = () => localStorage.removeItem('tradelab_user');

async function request<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
  const token = getAuthToken();
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(options.headers as Record<string, string>),
  };

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const response = await fetch(`${API_BASE}${endpoint}`, {
    ...options,
    headers,
  });

  if (!response.ok) {
    let errorMsg = `Request failed: ${response.statusText}`;
    try {
      const errorJson = await response.json();
      errorMsg = errorJson.message || errorJson.error || errorMsg;
    } catch {
      // Ignore
    }
    throw new Error(errorMsg);
  }

  return response.json();
}

// Authentication
export const authApi = {
  register: (data: { username: string; email: string; password: string }) =>
    request<any>('/auth/register', { method: 'POST', body: JSON.stringify(data) }),
  login: (data: { usernameOrEmail: string; password: string }) =>
    request<any>('/auth/login', { method: 'POST', body: JSON.stringify(data) }),
  getMe: () => request<User>('/auth/me'),
};

// Stocks & Market
export const stockApi = {
  list: (search?: string) =>
    request<StockQuote[]>(search ? `/stocks?search=${encodeURIComponent(search)}` : '/stocks'),
  get: (symbol: string) => request<StockQuote>(`/stocks/${symbol}`),
  getHistory: (symbol: string, count = 60) =>
    request<Candle[]>(`/stocks/${symbol}/history?count=${count}`),
  topGainers: (limit = 5) => request<StockQuote[]>(`/stocks/top-gainers?limit=${limit}`),
  topLosers: (limit = 5) => request<StockQuote[]>(`/stocks/top-losers?limit=${limit}`),
  marketStatus: () => request<any>('/market/status'),
};

// Orders & Trades
export const orderApi = {
  place: (data: {
    symbol: string;
    exchange?: string;
    side: OrderSide;
    orderType: OrderType;
    quantity: number;
    limitPrice?: number | null;
    idempotencyKey?: string;
  }) => request<Order>('/orders', { method: 'POST', body: JSON.stringify(data) }),
  list: () => request<Order[]>('/orders'),
  cancel: (orderId: string) => request<Order>(`/orders/${orderId}`, { method: 'DELETE' }),
  trades: () => request<Trade[]>('/trades'),
};

// Portfolio
export const portfolioApi = {
  getSummary: () => request<PortfolioSummary>('/portfolio'),
  getPerformance: () => request<any[]>('/portfolio/performance'),
  getAnalyticsSql: () => request<any>('/portfolio/analytics-sql'),
};

// Wallet
export const walletApi = {
  getSummary: () => request<WalletSummary>('/wallet'),
  getTransactions: () => request<WalletTransaction[]>('/wallet/transactions'),
};

// Competitions
export const competitionApi = {
  list: () => request<Competition[]>('/competitions'),
  join: (id: string) => request<any>(`/competitions/${id}/join`, { method: 'POST' }),
  leaderboard: (id: string) => request<LeaderboardEntry[]>(`/competitions/${id}/leaderboard`),
};

// Watchlist
export const watchlistApi = {
  get: () => request<StockQuote[]>('/watchlist'),
  add: (symbol: string) => request<any>(`/watchlist/${symbol}`, { method: 'POST' }),
  remove: (symbol: string) => request<any>(`/watchlist/${symbol}`, { method: 'DELETE' }),
};
