export type OrderSide = 'BUY' | 'SELL';
export type OrderType = 'MARKET' | 'LIMIT';
export type OrderStatus = 'PENDING' | 'OPEN' | 'FILLED' | 'CANCELLED' | 'REJECTED' | 'EXPIRED';
export type DataMode = 'LIVE' | 'DELAYED' | 'HISTORICAL' | 'SIMULATED';
export type FeedStatus = 'FRESH' | 'STALE' | 'UNAVAILABLE';
export type CompetitionStatus = 'UPCOMING' | 'ACTIVE' | 'COMPLETED';

export interface StockQuote {
  symbol: string;
  companyName: string;
  exchange: string;
  lastTradedPrice: number;
  bidPrice: number;
  askPrice: number;
  open: number;
  high: number;
  low: number;
  previousClose: number;
  volume: number;
  change: number;
  changePercent: number;
  providerTimestamp: string;
  receivedAt: string;
  dataMode: DataMode;
  feedStatus: FeedStatus;
}

export interface Candle {
  time: number;
  open: number;
  high: number;
  low: number;
  close: number;
  volume: number;
}

export interface Holding {
  stockId: string;
  symbol: string;
  companyName: string;
  exchange: string;
  quantity: number;
  averagePrice: number;
  currentPrice: number;
  investedValue: number;
  marketValue: number;
  unrealizedPnl: number;
  returnPercentage: number;
}

export interface PortfolioSummary {
  availableCash: number;
  reservedCash: number;
  investedValue: number;
  holdingsMarketValue: number;
  totalAccountValue: number;
  unrealizedPnl: number;
  realizedPnl: number;
  totalPnl: number;
  returnPercentage: number;
  holdings: Holding[];
}

export interface Order {
  orderId: string;
  symbol: string;
  exchange: string;
  side: OrderSide;
  orderType: OrderType;
  quantity: number;
  limitPrice: number | null;
  status: OrderStatus;
  executedPrice: number | null;
  totalAmount: number | null;
  dataMode: DataMode;
  createdAt: string;
  updatedAt: string;
}

export interface Trade {
  tradeId: string;
  orderId: string;
  symbol: string;
  exchange: string;
  side: OrderSide;
  executedPrice: number;
  executedQuantity: number;
  totalAmount: number;
  executedAt: string;
}

export interface WalletSummary {
  walletId: string;
  availableBalance: number;
  lockedBalance: number;
  totalBalance: number;
  updatedAt: string;
}

export interface WalletTransaction {
  id: string;
  orderId: string | null;
  transactionType: string;
  amount: number;
  createdAt: string;
  description: string;
}

export interface Competition {
  id: string;
  name: string;
  startingBalance: number;
  startTime: string;
  endTime: string;
  status: CompetitionStatus;
  participantCount: number;
  hasJoined: boolean;
}

export interface LeaderboardEntry {
  rank: number;
  userId: string;
  username: string;
  totalAccountValue: number;
  returnPercentage: number;
}

export interface User {
  id: string;
  username: string;
  email: string;
  role: string;
  createdAt: string;
}

export interface AuthResponse {
  token: string;
  userId: string;
  username: string;
  email: string;
  role: string;
  availableCash: number;
}

export interface AuthState {
  token: string | null;
  user: {
    userId: string;
    username: string;
    email: string;
    role: string;
    availableCash: number;
  } | null;
}
