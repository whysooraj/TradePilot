# TradeLab: Real-Market-Data Virtual Stock Trading Simulator

TradeLab is a paper-trading and portfolio simulation platform designed for Indian Equities (NSE). It allows users to trade real or simulated stocks using virtual money, manage portfolios, track profit and loss with transactional double-entry accounting, and participate in campus trading tournaments without financial risk.

---

## 1. System Architecture

TradeLab is built as a modular monolith adhering to transactional isolation and authoritative backend validation.

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

### Key Modules

- Market Data Engine: Abstraction interface (`MarketDataProvider`) with tick generator, quote freshness validation, and historical OHLCV candle synthesizers.
- Virtual Trading Engine: Validates market and limit orders, enforces pessimistic database locking (`@Lock(LockModeType.PESSIMISTIC_WRITE)`), executes atomic ledger adjustments, and checks idempotency keys.
- Portfolio Engine: Weighted-average cost calculations, invested vs. market valuations, unrealized and realized P&L calculations, and periodic equity snapshots.
- Direct JDBC Reporting: Dedicated DAO (`PortfolioReportingDao`) demonstrating low-level SQL aggregations alongside JPA ORM.
- Campus Competitions: Competition management, isolated balances, and real-time participant leaderboards based on return percentage.
- Identity & Security: BCrypt password hashing, stateless HMAC-SHA256 JWT tokens, and user-scoped data protection.

---

## 2. Technology Stack

- Backend: Java 21, Spring Boot 3.3.5, Spring Security, Spring Data JPA, Hibernate, Spring WebSocket (STOMP), Flyway, SpringDoc OpenAPI / Swagger.
- Frontend: React 18, TypeScript, Vite, Tailwind CSS, TradingView Lightweight Charts, Lucide Icons, SockJS / StompJS.
- Database: PostgreSQL with version-controlled Flyway schema migrations.
- Build & Testing: Maven, JUnit 5, Mockito, H2 (test profile), Docker, Docker Compose.

---

## 3. Project Structure

```
TradePilot/
├── TradeLab_Complete_Project_Blueprint.md   # Architectural blueprint
├── docker-compose.yml                       # Unified multi-container deployment
├── backend/
│   ├── pom.xml                              # Maven project definition
│   ├── Dockerfile
│   └── src/
│       ├── main/
│       │   ├── java/com/example/tradelab/
│       │   │   ├── config/                  # Security, WebSockets, OpenAPI, CORS
│       │   │   ├── controller/              # Auth, Stocks, Orders, Portfolio, Competitions
│       │   │   ├── dto/                     # Strongly-typed request/response models
│       │   │   ├── entity/                  # JPA persistence entities & enums
│       │   │   ├── exception/               # Custom exceptions & GlobalExceptionHandler
│       │   │   ├── marketdata/              # MarketDataProvider & MockMarketDataProvider
│       │   │   ├── repository/              # Spring Data JPA repositories & JDBC DAO
│       │   │   ├── security/                # JWT Token provider & authentication filters
│       │   │   ├── service/                 # Core domain & transactional trading services
│       │   │   └── websocket/               # QuoteBroadcaster
│       │   └── resources/
│       │       ├── application.yml
│       │       └── db/migration/            # Flyway V1 (schema) & V2 (stock seeds)
│       └── test/                            # Unit, integration, and concurrency tests
└── frontend/
    ├── package.json
    ├── vite.config.ts
    ├── Dockerfile
    ├── nginx.conf
    └── src/
        ├── components/                      # Dashboard, Explore, Portfolio, Orders, Charts
        ├── services/                        # REST API client & WebSocket client
        └── types/                           # TypeScript interfaces and domain types
```

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

Access the applications:
- Frontend: `http://localhost:3000`
- Backend API: `http://localhost:8080/api/v1`
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

## 5. REST API Endpoints

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

## 6. Verification and Testing

Automated test suites verify:
- Pessimistic locking: Multi-threaded simultaneous buy orders against a single wallet never overdraft the cash balance.
- Accounting correctness: Weighted-average cost calculations across multiple buy orders and partial sells.
- Idempotency: Duplicate submissions with the same idempotency key return the original execution without double debiting.
- Boundary enforcement: Negative quantities, insufficient cash, insufficient shares, and stale quotes are rejected.
