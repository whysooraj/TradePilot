-- V1__initial_schema.sql
-- Core schema for TradeLab virtual stock trading simulator

CREATE TABLE users (
    id UUID PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'ROLE_USER',
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE wallets (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    available_balance DECIMAL(19,4) NOT NULL DEFAULT 100000.0000,
    locked_balance DECIMAL(19,4) NOT NULL DEFAULT 0.0000,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE stocks (
    id UUID PRIMARY KEY,
    symbol VARCHAR(20) NOT NULL,
    company_name VARCHAR(100) NOT NULL,
    exchange VARCHAR(10) NOT NULL DEFAULT 'NSE',
    provider_instrument_token VARCHAR(50),
    active BOOLEAN NOT NULL DEFAULT true,
    CONSTRAINT uq_stock_symbol_exchange UNIQUE (symbol, exchange)
);

CREATE TABLE stock_prices (
    id BIGSERIAL PRIMARY KEY,
    stock_id UUID NOT NULL REFERENCES stocks(id) ON DELETE CASCADE,
    price DECIMAL(19,4) NOT NULL,
    volume BIGINT NOT NULL DEFAULT 0,
    recorded_at TIMESTAMPTZ NOT NULL,
    data_mode VARCHAR(20) NOT NULL DEFAULT 'SIMULATED'
);
CREATE INDEX idx_stock_prices_stock_time ON stock_prices(stock_id, recorded_at DESC);

CREATE TABLE orders (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    stock_id UUID NOT NULL REFERENCES stocks(id) ON DELETE RESTRICT,
    side VARCHAR(10) NOT NULL, -- BUY, SELL
    order_type VARCHAR(10) NOT NULL, -- MARKET, LIMIT
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    limit_price DECIMAL(19,4),
    status VARCHAR(20) NOT NULL, -- PENDING, OPEN, FILLED, REJECTED, CANCELLED
    idempotency_key VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_orders_user_created ON orders(user_id, created_at DESC);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_idempotency ON orders(user_id, idempotency_key);

CREATE TABLE trades (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    executed_price DECIMAL(19,4) NOT NULL,
    executed_quantity INTEGER NOT NULL CHECK (executed_quantity > 0),
    total_amount DECIMAL(19,4) NOT NULL,
    executed_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_trades_order ON trades(order_id);

CREATE TABLE holdings (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    stock_id UUID NOT NULL REFERENCES stocks(id) ON DELETE RESTRICT,
    quantity INTEGER NOT NULL CHECK (quantity >= 0),
    average_price DECIMAL(19,4) NOT NULL CHECK (average_price >= 0),
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_holdings_user_stock UNIQUE (user_id, stock_id)
);

CREATE TABLE wallet_transactions (
    id UUID PRIMARY KEY,
    wallet_id UUID NOT NULL REFERENCES wallets(id) ON DELETE CASCADE,
    order_id UUID REFERENCES orders(id) ON DELETE SET NULL,
    transaction_type VARCHAR(30) NOT NULL, -- INITIAL_CREDIT, BUY_DEBIT, SELL_CREDIT, ORDER_RESERVE, ORDER_RELEASE, FEE_DEBIT
    amount DECIMAL(19,4) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    description TEXT
);
CREATE INDEX idx_wallet_tx_wallet ON wallet_transactions(wallet_id, created_at DESC);

CREATE TABLE competitions (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    starting_balance DECIMAL(19,4) NOT NULL DEFAULT 100000.0000,
    start_time TIMESTAMPTZ NOT NULL,
    end_time TIMESTAMPTZ NOT NULL,
    status VARCHAR(20) NOT NULL -- UPCOMING, ACTIVE, COMPLETED
);

CREATE TABLE competition_participants (
    id UUID PRIMARY KEY,
    competition_id UUID NOT NULL REFERENCES competitions(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    joined_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_competition_user UNIQUE (competition_id, user_id)
);

CREATE TABLE portfolio_snapshots (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    total_value DECIMAL(19,4) NOT NULL,
    cash_balance DECIMAL(19,4) NOT NULL,
    holdings_value DECIMAL(19,4) NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_portfolio_snapshots_user ON portfolio_snapshots(user_id, recorded_at DESC);

CREATE TABLE watchlists (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(50) NOT NULL DEFAULT 'Default Watchlist',
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE watchlist_stocks (
    id UUID PRIMARY KEY,
    watchlist_id UUID NOT NULL REFERENCES watchlists(id) ON DELETE CASCADE,
    stock_id UUID NOT NULL REFERENCES stocks(id) ON DELETE CASCADE,
    added_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_watchlist_stock UNIQUE (watchlist_id, stock_id)
);
