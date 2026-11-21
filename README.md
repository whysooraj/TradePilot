<p align="center">
  <img src="assets/logo.svg" alt="TradeLab Logo" width="340" />
</p>

<p align="center">
  <strong>Real-Market-Data Virtual Stock Trading & Portfolio Simulation Platform</strong>
</p>

<p align="center">
  <a href="#1-system-architecture">Architecture</a> &bull;
  <a href="#2-technology-stack">Tech Stack</a> &bull;
  <a href="#3-features">Features</a> &bull;
  <a href="#4-getting-started">Getting Started</a> &bull;
  <a href="#5-rest-api-reference">API Docs</a> &bull;
  <a href="#6-verification--concurrency-guarantees">Concurrency Guarantees</a>
</p>

---

## 1. System Architecture

TradeLab is engineered as a modular monolith adhering to transactional isolation, strict concurrency control, and authoritative server-side validation. Clients never dictate execution prices, account balances, or transaction outcomes.

```
                      Market Data Engine
                 (Live / Simulated NSE Equities)
                               |
                               v
                     Quote Validation Cache
                        /             \
                       v               v
                  PostgreSQL       STOMP WebSocket
                      ^                |
                      |                v
               Spring Boot App <--> React Client
             - Auth & Identity     - TradingView Charts
             - Order Matching      - Real-Time Tick Tables
             - Wallet Ledger       - Portfolio & P&L
             - Tournaments         - Order Execution Tickets
```

### Core Modules

- Market Data Engine: Decoupled interface (`MarketDataProvider`) with tick generator, quote freshness validation, and historical OHLCV candle synthesizers.
- Virtual Trading Engine: Validates market and limit orders, enforces pessimistic database locking (`@Lock(LockModeType.PESSIMISTIC_WRITE)`), executes atomic ledger adjustments, and checks idempotency keys.
- Portfolio Engine: Weighted-average cost calculations, invested vs. market valuations, unrealized and realized P&L calculations, and periodic equity snapshots.
- Direct JDBC Reporting: Dedicated DAO (`PortfolioReportingDao`) demonstrating low-level SQL aggregations alongside JPA ORM.
- Campus Competitions: Tournament management, isolated balances, and real-time participant leaderboards based on return percentage.
- Identity & Security: BCrypt password hashing, stateless HMAC-SHA256 JWT tokens, and user-scoped data protection.

---

## 2. Technology Stack

- Backend: Java 21, Spring Boot 3.3.5, Spring Security, Spring Data JPA, Hibernate, Spring WebSocket (STOMP), Flyway, SpringDoc OpenAPI / Swagger.
- Frontend: React 18, TypeScript, Vite, Tailwind CSS, TradingView Lightweight Charts, Lucide Icons, SockJS / StompJS.
- Database: PostgreSQL with version-controlled Flyway schema migrations.
- Build & Testing: Maven, JUnit 5, Mockito, H2 (test profile), Docker, Docker Compose.

---

## 3. Features

- Real-Market Simulation: Top NSE equities (RELIANCE, TCS, HDFCBANK, INFY, ICICIBANK, TATAMOTORS, SBIN, BHARTIARTL, ITC, LT, WIPRO, BAJFINANCE) with live ticks.
- Double-Entry Wallet Ledger: Immutable audit trail (`INITIAL_CREDIT`, `BUY_DEBIT`, `SELL_CREDIT`, `ORDER_RESERVE`, `ORDER_RELEASE`).
- Interactive TradingView Charts: 5-minute candlestick chart with OHLCV volume overlays.
- Order Management: Instant market fills and price-triggered limit orders with automated refunding of price improvements.
- Direct JDBC Analytics: Raw SQL execution (`PortfolioReportingDao`) querying aggregations across orders, trades, and holdings.
- Campus Tournaments: Competitive paper-trading with equal initial capital and live return rankings.
- 1-Click Demo Access: Instant demo account provisioning loaded with ₹1,00,000 virtual INR.

---

## 4. Getting Started

### Prerequisites

- Java 21 JDK
- Maven 3.9+
- Node.js 18+ and npm
- PostgreSQL 15+ (or Docker)

### Option A: Running with Docker Compose

Run the entire platform (PostgreSQL, Spring Boot Backend, Nginx React Frontend):

```bash
docker-compose up --build
```

Access endpoints:
- Frontend Application: `http://localhost:3000`
- Backend REST API: `http://localhost:8080/api/v1`
- Swagger UI Documentation: `http://localhost:8080/swagger-ui.html`

---

### Option B: Running Locally for Development

#### 1. Database Setup

Ensure PostgreSQL is running locally on port 5432 with database `tradelab`:

```bash
psql -U postgres -c "CREATE DATABASE tradelab;"
```

#### 2. Run Backend

```bash
cd backend
mvn spring-boot:run
```

Flyway will automatically execute database migrations (`V1__initial_schema.sql` and `V2__seed_stocks.sql`).

To run the automated tests (including multi-threaded concurrency tests):

```bash
mvn test
```

#### 3. Run Frontend

```bash
cd frontend
npm install
npm run dev
```

Open `http://localhost:5173` in your browser.

---

## 5. REST API Reference

### Authentication
- `POST /api/v1/auth/register` - Create account and provision virtual ₹1,00,000 cash balance
- `POST /api/v1/auth/login` - Authenticate and retrieve JWT token
- `GET /api/v1/auth/me` - Get profile of authenticated user

### Stocks and Market Data
- `GET /api/v1/stocks` - List and search Indian equities (NSE) with live quotes
- `GET /api/v1/stocks/{symbol}` - Quote details for specific stock
- `GET /api/v1/stocks/{symbol}/history` - Historical OHLCV candles for TradingView chart
- `GET /api/v1/stocks/top-gainers` - Session top gainers
- `GET /api/v1/stocks/top-losers` - Session top losers
- `GET /api/v1/market/status` - Exchange operational status and data mode

### Orders and Trading
- `POST /api/v1/orders` - Place market or limit order (with idempotency support)
- `GET /api/v1/orders` - User order log (OPEN, FILLED, CANCELLED, REJECTED)
- `DELETE /api/v1/orders/{id}` - Cancel pending open limit order
- `GET /api/v1/trades` - Completed execution records

### Portfolio & Accounting
- `GET /api/v1/portfolio` - Complete portfolio summary (cash, invested, unrealized/realized P&L)
- `GET /api/v1/portfolio/holdings` - Active positions with average buy price and current market value
- `GET /api/v1/portfolio/performance` - Historical account valuation snapshots
- `GET /api/v1/portfolio/analytics-sql` - Direct JDBC execution showing turnover and volumes

### Campus Competitions & Leaderboards
- `GET /api/v1/competitions` - List active tournaments
- `POST /api/v1/competitions` - Create a new tournament
- `POST /api/v1/competitions/{id}/join` - Register for a tournament
- `GET /api/v1/competitions/{id}/leaderboard` - Live participant rankings by portfolio return

---

## 6. Verification & Concurrency Guarantees

Automated test suites verify:
- Pessimistic Locking: Multi-threaded simultaneous buy orders against a single wallet never overdraft the cash balance (`ConcurrencyTest.java`).
- Accounting Correctness: Weighted-average cost calculations across multiple buy orders and partial sells.
- Idempotency: Duplicate submissions with the same idempotency key return the original execution without double debiting.
- Boundary Enforcement: Negative quantities, insufficient cash, insufficient shares, and stale quotes are rejected.
