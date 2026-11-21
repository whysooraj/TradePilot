# TradeLab --- Complete Project Blueprint

**Project:** Real-market-data virtual stock trading simulator\
**Category:** Advanced Java / Full-stack application\
**Primary purpose:** Let users trade real or historical stocks using
virtual money, track portfolios, and compete without risking real funds.

------------------------------------------------------------------------

## 1. Project overview

TradeLab is a paper-trading platform. Users receive a virtual cash
balance, view stock prices, place simulated buy/sell orders, manage
holdings, and track profit/loss. The application does not place real
orders or move real money.

### Main systems

1.  **Market Data Engine** --- obtains and distributes stock quotes and
    historical prices.
2.  **Virtual Trading Engine** --- validates and executes simulated
    orders.
3.  **Portfolio Engine** --- tracks cash, holdings, average cost, and
    P&L.
4.  **Competition Engine** --- runs trading contests and leaderboards.
5.  **Analytics Engine** --- reports trading performance and risk
    metrics.
6.  **Identity and Security** --- manages users, sessions, permissions,
    and audit trails.

### Recommended initial scope

Build a modular monolith, not microservices. Complete the core trading
workflow before adding advanced features.

------------------------------------------------------------------------

## 2. Recommended technology stack

  -----------------------------------------------------------------------
  Layer                   Technology              Purpose
  ----------------------- ----------------------- -----------------------
  Frontend                React + TypeScript      User interface

  Styling                 Tailwind CSS            Responsive styling

  Charts                  TradingView Lightweight Candlestick and line
                          Charts                  charts

  Backend                 Java + Spring Boot      Application and
                                                  business logic

  Database                PostgreSQL              Persistent application
                                                  data

  ORM                     Spring Data JPA /       Database persistence
                          Hibernate               

  Direct SQL              JDBC                    Focused SQL/reporting
                                                  demonstrations

  Security                Spring Security         Authentication and
                                                  authorization

  Live updates            Spring WebSocket /      Push quote and
                          STOMP                   portfolio updates

  Migrations              Flyway                  Version-controlled
                                                  database schema

  API docs                OpenAPI / Swagger       API documentation

  Tests                   JUnit 5, Mockito,       Unit and integration
                          Testcontainers          tests

  Build                   Maven                   Dependency and build
                                                  management

  Deployment              Docker / Docker Compose Reproducible deployment

  Optional cache          Redis                   Shared latest-price
                                                  cache and pub/sub
  -----------------------------------------------------------------------

**Why this stack:** It supports a substantial Java backend while
allowing a modern browser interface. If the course specifically requires
Servlets and JSP, use those for the presentation layer or create a
smaller traditional-Java demonstration module. Confirm the instructor's
requirements before committing to Spring Boot alone.

### Architecture choice

Use a **modular monolith**. Keep market data, trading, portfolio,
competitions, and identity as separate packages/modules inside one
Spring Boot application. This is simpler to build, test, deploy, and
explain than microservices.

------------------------------------------------------------------------

## 3. High-level architecture

``` text
                   Stock Market Data Provider
                              |
                              v
                    Market Data Adapter
                              |
                    Quote validation/cache
                       /            \
                      v              v
                 PostgreSQL       WebSocket
                                      |
                                      v
                                React clients

 React client
      |
      | HTTPS / REST
      v
 Spring Boot application
      |
      +---- Authentication and authorization
      +---- Stock and market-data services
      +---- Trading and order execution
      +---- Wallet and ledger
      +---- Portfolio and analytics
      +---- Competition and leaderboard
      |
      v
 PostgreSQL
```

### Main data flow

1.  A provider sends quotes through REST or a WebSocket feed.
2.  The market-data adapter converts provider-specific messages into the
    application's common quote format.
3.  The backend validates the symbol, timestamp, price, and feed status.
4.  The latest valid quote is kept in a cache.
5.  Selected quote updates are broadcast to subscribed clients.
6.  A user submits an order through REST.
7.  The backend authenticates the user and validates the order.
8.  The execution service uses a server-side quote and defined execution
    rules.
9.  A database transaction updates the wallet, holdings, order, trade,
    and ledger.
10. The frontend refreshes the wallet and portfolio and receives
    relevant live updates.

**Never trust the browser for balances, execution prices, ownership, or
P&L.** The backend is authoritative.

------------------------------------------------------------------------

## 4. Market data

### 4.1 Data source options

For Indian equities, investigate official developer offerings from:

-   DhanHQ: https://dhanhq.co/docs/v2/
-   Upstox: https://upstox.com/developer/api-documentation/
-   Zerodha Kite Connect: https://kite.trade/docs/connect/v3/

Review current pricing, account requirements, exchange coverage, API
limits, WebSocket limits, redistribution rules, and commercial-use
restrictions before selecting a provider. API access and real-time data
may require a paid plan or an eligible broker account.

Do not assume that a broker API allows unrestricted public
redistribution of its market feed. Check the provider's terms and
exchange data licensing requirements before making the app public.

### 4.2 Provider abstraction

Do not couple the rest of the application to one provider. Define a
common interface:

``` java
public interface MarketDataProvider {
    List<StockQuote> getQuotes(List<String> symbols);
    void subscribe(String symbol);
    void unsubscribe(String symbol);
    Optional<StockQuote> getLatestQuote(String symbol);
}
```

Implement provider adapters such as:

``` text
MarketDataProvider
  |-- DhanMarketDataProvider
  |-- UpstoxMarketDataProvider
  |-- ZerodhaMarketDataProvider
  |-- MockMarketDataProvider
```

The mock provider is essential for development, automated tests, market
holidays, API outages, and classroom demonstrations.

### 4.3 Quote model

A normalized quote should contain at least:

  Field               Meaning
  ------------------- -----------------------------------------
  instrumentId        Internal instrument identifier
  symbol              Trading symbol
  exchange            NSE, BSE, etc.
  lastTradedPrice     Last traded price
  bidPrice            Best available bid, if supplied
  askPrice            Best available ask, if supplied
  open                Session opening price
  high                Session high
  low                 Session low
  previousClose       Previous session close
  volume              Traded volume
  providerTimestamp   Timestamp from the source
  receivedAt          Time received by this backend
  dataMode            LIVE, DELAYED, HISTORICAL, or SIMULATED
  feedStatus          FRESH, STALE, or UNAVAILABLE

Never label delayed or simulated prices as live.

### 4.4 Market-data processing

``` text
Provider WebSocket / REST
          |
          v
Provider-specific decoder
          |
          v
Normalize into StockQuote
          |
          v
Validate symbol, price, timestamp
          |
          v
Update latest quote cache
          |
          +------> Broadcast to subscribed clients
          |
          +------> Persist selected historical data
```

Start with an in-memory `ConcurrentHashMap` for latest quotes. Add Redis
only if you need shared cache state across multiple backend instances.

Avoid storing every tick forever. Define a retention policy. For an MVP,
store periodic snapshots or candles (for example, one-minute OHLCV bars)
and retain raw ticks only if needed.

### 4.5 Data outages and stale prices

-   Track the timestamp of every quote.
-   Define a configurable maximum quote age.
-   Reject market orders if the quote is stale or unavailable.
-   Show the data mode and last-updated time in the UI.
-   Do not silently switch from real to mock data while still showing a
    "LIVE" label.
-   Provide an explicit demo mode using simulated prices.

------------------------------------------------------------------------

## 5. Virtual trading engine

This is the most important business module. It simulates trades and
never sends orders to a broker.

### 5.1 Initial wallet

Example new-user account:

  Item                         Amount
  ----------------------- -----------
  Initial virtual cash      ₹1,00,000
  Available cash            ₹1,00,000
  Reserved cash                    ₹0
  Holdings market value            ₹0
  Total account value       ₹1,00,000

Create the wallet on the server during account provisioning. Do not
initialize it in React.

### 5.2 Market buy example

A user buys 10 shares at a simulated execution price of ₹1,450.

``` text
Quantity       = 10
Execution price = ₹1,450
Gross amount    = 10 × 1,450 = ₹14,500
Starting cash   = ₹1,00,000
Remaining cash  = ₹85,500
```

The backend should:

1.  Authenticate the user.
2.  Validate the instrument and exchange.
3.  Validate the order side, type, and quantity.
4.  Check market status and quote freshness.
5.  Determine the execution price using server-side rules.
6.  Calculate fees, if enabled.
7.  Check available virtual cash.
8.  Lock or atomically update the wallet.
9.  Create the order and execution record.
10. Update holdings and wallet ledger.
11. Commit all changes in one database transaction.
12. Return the authoritative execution result.

### 5.3 Market sell example

The user sells 10 shares at ₹1,500.

``` text
Sale proceeds  = 10 × ₹1,500 = ₹15,000
Original cost  = ₹14,500
Realized P&L   = ₹500
```

The backend must verify that the user owns enough available shares,
update holdings, credit proceeds, record the execution, and record the
realized result.

### 5.4 Required validations

  Condition                        Expected result
  -------------------------------- ------------------------------------------
  Insufficient cash                Reject buy
  Insufficient available shares    Reject sell
  Zero or negative quantity        Reject
  Unknown or inactive instrument   Reject
  Stale quote                      Reject or require fresh quote
  Market closed                    Reject market order in live-session mode
  Duplicate request                Do not execute twice
  Concurrent orders                Never overspend or oversell
  Unauthorized account             Reject
  Invalid limit price              Reject

### 5.5 Order types

**Market order:** Executes immediately according to the simulator's
pricing policy.

**Limit order:** Remains pending until the execution condition is met.

-   Buy limit: execute only when the eligible ask is at or below the
    user's limit.
-   Sell limit: execute only when the eligible bid is at or above the
    user's limit.

If the provider only supplies LTP, a simplified simulator may use LTP as
the trigger. Clearly document that this is not a full exchange order
book simulation.

For the first version, implement market orders. Add limit orders after
the wallet and holdings logic is correct.

### 5.6 Order statuses

Use a controlled set of statuses:

``` text
PENDING
OPEN
PARTIALLY_FILLED
FILLED
CANCELLED
REJECTED
EXPIRED
```

For an MVP with immediate full fills, `FILLED` and `REJECTED` may be
enough initially. Add the others when pending and partial execution are
implemented.

### 5.7 Execution realism

Two implementation levels:

**Basic MVP** - Execute at the latest valid LTP. - Fill the entire order
immediately. - Apply configurable fees or zero fees. - Reject stale
prices.

**Advanced** - Buy using ask and sell using bid. - Apply configurable
slippage. - Support partial fills. - Match pending limit orders. -
Simulate market depth. - Track execution events.

Do not claim LTP-only execution reproduces real exchange fills.

------------------------------------------------------------------------

## 6. Wallet and accounting

Use a ledger-based design. A balance column is useful for fast reads,
but every balance change must also have an immutable transaction record.

Suggested wallet transaction types:

-   INITIAL_CREDIT
-   BUY_DEBIT
-   SELL_CREDIT
-   ORDER_RESERVE
-   ORDER_RELEASE
-   FEE_DEBIT
-   DIVIDEND_CREDIT
-   ADMIN_ADJUSTMENT

For every change, store the amount, transaction type, related
order/trade if applicable, timestamp, and resulting balance or enough
information to reconstruct it.

For limit orders, reserve the required cash when appropriate. On
cancellation or expiry, release the reserved amount. For sells, consider
reserving shares so the user cannot sell the same shares twice.

Use `BigDecimal` for money and prices. Never use Java `float` or
`double` for financial amounts.

Recommended SQL precision: `DECIMAL(19,4)` or a precision appropriate to
the instruments and currency. Define rounding rules explicitly.

------------------------------------------------------------------------

## 7. Portfolio calculations

### 7.1 Average cost

If a user buys: - 10 shares at ₹100 - 10 shares at ₹120

Then:

``` text
Average cost = ((10 × 100) + (10 × 120)) / 20
             = ₹110
```

For the basic version, use weighted-average cost. If you later want
tax-lot-level reporting, implement FIFO or another explicit accounting
method.

### 7.2 Core formulas

``` text
Invested value = Σ(quantity × average cost)

Holdings market value = Σ(quantity × current price)

Account value = available cash + reserved cash + holdings market value

Unrealized P&L = holdings market value - remaining cost basis

Realized P&L = sum of realized gains/losses on completed sales

Total P&L = realized P&L + unrealized P&L - applicable fees
```

Be careful not to subtract the original purchase amount twice. Define
one consistent accounting model and test it.

### 7.3 Return

``` text
Return (%) = ((current account value - initial capital)
              / initial capital) × 100
```

For daily P&L, compare the current account value with the previous
official portfolio snapshot using a consistent valuation timestamp.

### 7.4 Corporate actions

Later, support: - Stock splits. - Bonus shares. - Dividends. - Symbol
changes. - Delisted instruments.

Example: a 2-for-1 split changes 10 shares at ₹1,000 average cost to 20
shares at ₹500 adjusted average cost. Total cost basis remains ₹10,000,
ignoring market movement.

Do not add these until the basic ledger and portfolio calculations are
stable.

------------------------------------------------------------------------

## 8. Database design

Use PostgreSQL and Flyway migrations.

### 8.1 Core tables

#### `users`

  Column          Type
  --------------- -------------------
  id              UUID, primary key
  username        VARCHAR, unique
  email           VARCHAR, unique
  password_hash   VARCHAR
  role            VARCHAR
  created_at      TIMESTAMPTZ
  updated_at      TIMESTAMPTZ

#### `wallets`

  Column              Type
  ------------------- --------------------------
  id                  UUID, primary key
  user_id             UUID, unique foreign key
  available_balance   DECIMAL(19,4)
  locked_balance      DECIMAL(19,4)
  updated_at          TIMESTAMPTZ

#### `stocks` / `instruments`

  Column                      Type
  --------------------------- -------------------
  id                          UUID, primary key
  symbol                      VARCHAR
  company_name                VARCHAR
  exchange                    VARCHAR
  provider_instrument_token   VARCHAR
  active                      BOOLEAN

Use a unique constraint on the exchange and symbol combination. Provider
instrument tokens are not necessarily globally unique.

#### `stock_prices`

  Column        Type
  ------------- ---------------------
  id            BIGINT, primary key
  stock_id      UUID, foreign key
  price         DECIMAL(19,4)
  volume        BIGINT
  recorded_at   TIMESTAMPTZ
  data_mode     VARCHAR

For larger datasets, consider partitioning or separate candle tables.

#### `orders`

  Column            Type
  ----------------- -------------------------
  id                UUID, primary key
  user_id           UUID, foreign key
  stock_id          UUID, foreign key
  side              VARCHAR
  order_type        VARCHAR
  quantity          INTEGER
  limit_price       DECIMAL(19,4), nullable
  status            VARCHAR
  idempotency_key   VARCHAR
  created_at        TIMESTAMPTZ
  updated_at        TIMESTAMPTZ

#### `trades`

  Column              Type
  ------------------- -------------------
  id                  UUID, primary key
  order_id            UUID, foreign key
  executed_price      DECIMAL(19,4)
  executed_quantity   INTEGER
  total_amount        DECIMAL(19,4)
  executed_at         TIMESTAMPTZ

#### `holdings`

  Column          Type
  --------------- -------------------
  id              UUID, primary key
  user_id         UUID, foreign key
  stock_id        UUID, foreign key
  quantity        INTEGER
  average_price   DECIMAL(19,4)
  updated_at      TIMESTAMPTZ

Add a unique constraint on `(user_id, stock_id)`.

#### `wallet_transactions`

  Column             Type
  ------------------ -------------------
  id                 UUID, primary key
  wallet_id          UUID, foreign key
  order_id           UUID, nullable
  transaction_type   VARCHAR
  amount             DECIMAL(19,4)
  created_at         TIMESTAMPTZ
  description        TEXT

#### `competitions`

  Column             Type
  ------------------ -------------------
  id                 UUID, primary key
  name               VARCHAR
  starting_balance   DECIMAL(19,4)
  start_time         TIMESTAMPTZ
  end_time           TIMESTAMPTZ
  status             VARCHAR

#### `competition_participants`

  Column           Type
  ---------------- -------------------
  id               UUID, primary key
  competition_id   UUID, foreign key
  user_id          UUID, foreign key
  joined_at        TIMESTAMPTZ

Use a unique constraint on `(competition_id, user_id)`. For serious
competition isolation, maintain a separate competition account, wallet,
holdings, and ledger per participant. Do not reuse a user's main wallet
for multiple contests.

#### `portfolio_snapshots`

  Column           Type
  ---------------- -------------------
  id               UUID, primary key
  account_id       UUID
  total_value      DECIMAL(19,4)
  cash_balance     DECIMAL(19,4)
  holdings_value   DECIMAL(19,4)
  recorded_at      TIMESTAMPTZ

### 8.2 Additional tables for later

-   `watchlists`
-   `watchlist_stocks`
-   `price_alerts`
-   `refresh_tokens` (if using refresh-token authentication)
-   `idempotency_keys` (or enforce uniqueness on orders)
-   `corporate_actions`
-   `candles`
-   `competition_accounts`
-   `competition_holdings`
-   `competition_orders`
-   `competition_trades`

### 8.3 Database rules

1.  Use foreign keys and unique constraints.
2.  Index `(stock_id, recorded_at)` for price history.
3.  Index `(user_id, created_at)` for order history.
4.  Index order status for pending-order processing.
5.  Store timestamps in UTC using `TIMESTAMPTZ`.
6.  Keep transaction and trade history immutable.
7.  Use database transactions for every multi-record trade.
8.  Use migrations; do not rely on automatic schema generation in
    production.
9.  Store cash and holdings separately.
10. Define how balances are reconciled against ledger entries.

### 8.4 Relationship overview

``` text
USERS 1---1 WALLETS
USERS 1---N ORDERS
USERS 1---N HOLDINGS
USERS 1---N WALLET_TRANSACTIONS
STOCKS 1---N ORDERS
STOCKS 1---N HOLDINGS
STOCKS 1---N STOCK_PRICES
ORDERS 1---N TRADES
COMPETITIONS 1---N COMPETITION_PARTICIPANTS
USERS 1---N PORTFOLIO_SNAPSHOTS
```

------------------------------------------------------------------------

## 9. Backend package structure

``` text
backend/
  src/main/java/com/example/tradelab/
    config/
      SecurityConfig.java
      WebSocketConfig.java
      CorsConfig.java
    controller/
      AuthController.java
      StockController.java
      OrderController.java
      PortfolioController.java
      WalletController.java
      CompetitionController.java
    service/
      AuthService.java
      MarketDataService.java
      TradingService.java
      OrderExecutionService.java
      PortfolioService.java
      WalletService.java
      CompetitionService.java
    repository/
    entity/
    dto/
    mapper/
    exception/
    scheduler/
    websocket/
    security/
    marketdata/
      MarketDataProvider.java
      DhanMarketDataProvider.java
      UpstoxMarketDataProvider.java
      MockMarketDataProvider.java
    util/
  src/main/resources/
    application.yml
    db/migration/
```

### Layer responsibilities

  Layer                 Responsibility
  --------------------- ------------------------------------
  Controller            HTTP request/response handling
  Service               Business rules and orchestration
  Repository            Database operations
  Entity                Persistent data model
  DTO                   API request and response contracts
  Mapper                Entity/DTO conversion
  Config                Infrastructure configuration
  Scheduler             Periodic background tasks
  WebSocket             Live push communication
  Security              Authentication and access checks
  Market data adapter   Provider-specific API integration

Controllers should not contain trade calculations or direct database
logic.

------------------------------------------------------------------------

## 10. REST API design

Use versioned routes such as `/api/v1`.

### Authentication

  Method   Endpoint                  Purpose
  -------- ------------------------- -----------------------
  POST     `/api/v1/auth/register`   Register
  POST     `/api/v1/auth/login`      Login
  POST     `/api/v1/auth/refresh`    Refresh session/token
  POST     `/api/v1/auth/logout`     Logout
  GET      `/api/v1/users/me`        Current user

### Stocks and market data

  Method   Endpoint                            Purpose
  -------- ----------------------------------- -------------------------
  GET      `/api/v1/stocks`                    List/search instruments
  GET      `/api/v1/stocks/{symbol}`           Instrument details
  GET      `/api/v1/stocks/{symbol}/history`   Historical data
  GET      `/api/v1/market/status`             Market/feed status
  GET      `/api/v1/stocks/top-gainers`        Top gainers
  GET      `/api/v1/stocks/top-losers`         Top losers

### Orders and trades

  Method   Endpoint                Purpose
  -------- ----------------------- -------------------------------
  POST     `/api/v1/orders`        Place order
  GET      `/api/v1/orders`        List user's orders
  GET      `/api/v1/orders/{id}`   Order detail
  DELETE   `/api/v1/orders/{id}`   Cancel eligible pending order
  GET      `/api/v1/trades`        Executed trades

### Wallet and portfolio

  Method   Endpoint                          Purpose
  -------- --------------------------------- ------------------------
  GET      `/api/v1/wallet`                  Wallet summary
  GET      `/api/v1/wallet/transactions`     Ledger history
  GET      `/api/v1/portfolio`               Portfolio summary
  GET      `/api/v1/portfolio/holdings`      Current holdings
  GET      `/api/v1/portfolio/performance`   Historical performance

### Competitions

  Method   Endpoint                                  Purpose
  -------- ----------------------------------------- ---------------------
  POST     `/api/v1/competitions`                    Create competition
  GET      `/api/v1/competitions`                    List competitions
  POST     `/api/v1/competitions/{id}/join`          Join competition
  GET      `/api/v1/competitions/{id}`               Competition details
  GET      `/api/v1/competitions/{id}/leaderboard`   Rankings

### Example order request

``` json
{
  "symbol": "RELIANCE",
  "exchange": "NSE",
  "side": "BUY",
  "orderType": "MARKET",
  "quantity": 10
}
```

### Example order response

``` json
{
  "orderId": "example-order-id",
  "status": "FILLED",
  "symbol": "RELIANCE",
  "quantity": 10,
  "executedPrice": 1450.00,
  "totalAmount": 14500.00,
  "dataMode": "LIVE"
}
```

The response's execution price and data mode must be produced by the
backend. Never accept a client-supplied execution price as
authoritative.

Use proper HTTP status codes and a consistent error structure. Document
validation errors, authorization errors, insufficient balance, stale
quotes, and market-closed conditions.

------------------------------------------------------------------------

## 11. Advanced Java concepts to demonstrate

These should be implemented, not merely listed in the report.

### 11.1 Multithreading and executors

Potential concurrent tasks:

``` text
Worker 1: Receive/decode market data
Worker 2: Process quote updates
Worker 3: Evaluate pending limit orders
Worker 4: Generate portfolio snapshots
Worker 5: Update competition rankings
```

Useful Java APIs:

-   `ExecutorService`
-   `ScheduledExecutorService`
-   `CompletableFuture`
-   `ConcurrentHashMap`
-   `BlockingQueue`
-   `AtomicLong`
-   `ReentrantLock`

Example:

``` java
ExecutorService executor = Executors.newFixedThreadPool(5);

executor.submit(() -> marketDataService.updatePrices());
executor.submit(() -> orderExecutionService.processPendingOrders());
```

Do not create a new thread for every quote. Use bounded executors and
queues. Define shutdown behavior and handle task failures.

### 11.2 Race conditions and synchronization

Example: a wallet contains ₹10,000. Two simultaneous requests attempt
purchases worth ₹7,000 and ₹6,000. If both read ₹10,000 before either
writes, both may pass the balance check and overspend.

Use database concurrency control, not just Java `synchronized`, because
multiple application instances can run at once.

Possible solutions:

-   `@Transactional`
-   Pessimistic row locking with `SELECT ... FOR UPDATE`
-   Atomic conditional SQL updates
-   Unique idempotency keys
-   Database constraints

Example Spring Data JPA lock:

``` java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT w FROM Wallet w WHERE w.user.id = :userId")
Optional<Wallet> findByUserIdForUpdate(
    @Param("userId") UUID userId
);
```

Perform the balance check, wallet change, holding change, order/trade
creation, and ledger write inside one transaction. If any step fails,
roll back all steps.

### 11.3 JDBC and Hibernate

-   JDBC provides direct database connectivity and SQL execution.
-   JPA defines an ORM standard.
-   Hibernate is a JPA implementation.
-   Spring Data JPA provides repository abstractions.

Use JPA/Hibernate for most application persistence. Use JDBC for a
focused reporting or bulk-query example if the course requires direct
JDBC knowledge.

### 11.4 WebSockets

REST is request/response. WebSockets let the server push new quotes and
account updates.

Use Spring WebSocket with STOMP, or a simpler WebSocket protocol if
appropriate. Authenticate WebSocket connections and ensure users only
receive permitted account-specific messages.

### 11.5 Scheduled jobs

Use Spring `@Scheduled` or a managed scheduler for:

-   Pending limit-order checks.
-   Portfolio snapshots.
-   Competition rankings.
-   Stale-feed checks.
-   Daily summaries.

Ensure scheduled jobs are safe if more than one backend instance runs.
Use distributed locking or a single designated scheduler when necessary.

### 11.6 Exception handling

Useful custom exceptions:

``` text
InsufficientBalanceException
InsufficientHoldingsException
InvalidOrderException
MarketClosedException
StaleMarketPriceException
StockNotFoundException
DuplicateOrderException
UnauthorizedAccountAccessException
```

Use `@RestControllerAdvice` for consistent API errors. Do not return
stack traces or internal secrets to clients.

------------------------------------------------------------------------

## 12. Frontend screens and UX

Use a professional dark trading-terminal style, but prioritize readable
data and clear actions.

### Required screens

  Screen           Content
  ---------------- --------------------------------------------
  Landing          Product summary, demo mode, registration
  Register/Login   Authentication
  Dashboard        Account value, cash, P&L, market overview
  Explore          Searchable stock list
  Stock details    Chart, quote, company data, buy/sell
  Trade ticket     Side, order type, quantity, limit price
  Portfolio        Holdings, average cost, current value, P&L
  Orders           Open, completed, cancelled, rejected
  Trade history    Executions and realized P&L
  Watchlist        Saved instruments
  Competitions     Available and joined contests
  Leaderboard      Ranking and returns
  Profile          User settings and security

### Dashboard layout

**Top summary** - Total account value. - Available virtual cash. -
Reserved cash. - Daily P&L. - Overall return.

**Main area** - Portfolio value chart. - Market indices. - Top gainers
and losers.

**Lower area** - Holdings. - Recent orders/trades. - Watchlist.

### Stock details

Display: - Company name, symbol, and exchange. - Latest price and
percentage change. - Data mode and last-updated time. - Candlestick or
line chart. - Volume and historical range. - Buy/sell ticket. - Company
information where licensed and available.

### Frontend engineering rules

-   Treat backend data as authoritative.
-   Use typed API response models.
-   Handle WebSocket reconnects and stale data.
-   Show loading, empty, error, and disconnected states.
-   Do not optimistically show a trade as completed before the server
    confirms it.
-   Refresh wallet and holdings after a successful trade.
-   Prevent duplicate submissions while a request is in progress.

------------------------------------------------------------------------

## 13. Financial calculations and rules

### Account value

``` text
Account value = available cash + reserved cash + holdings market value
```

### Invested value

``` text
Invested value = sum(quantity × average cost)
```

### Holdings market value

``` text
Holdings market value = sum(quantity × current market price)
```

### Unrealized P&L

``` text
Unrealized P&L = current holdings value - remaining cost basis
```

### Realized P&L

For a sale:

``` text
Realized P&L = sale proceeds - cost basis of shares sold - applicable fees
```

### Total P&L

``` text
Total P&L = realized P&L + unrealized P&L
```

Ensure fees are not subtracted twice. Define whether displayed P&L is
gross or net.

### Return percentage

``` text
Return (%) = ((current account value - initial capital)
              / initial capital) × 100
```

For more advanced reporting, use time-weighted returns if deposits or
withdrawals are allowed. For a fixed-capital competition with no
deposits or withdrawals, simple percentage return is easier to compare.

### Fees and taxes

Start with zero brokerage or a configurable simulated fee schedule. If
you simulate Indian delivery or intraday charges, research the
applicable rules and keep the schedules configurable and dated. Do not
present a made-up fixed charge as an accurate tax calculation.

------------------------------------------------------------------------

## 14. Competition mode

Competition mode gives the platform a campus use case.

### Example

**SRM Virtual Trading Championship**

-   Starting virtual capital: ₹1,00,000 per participant.
-   Fixed start and end dates.
-   Equal execution rules for everyone.
-   Rankings based on portfolio return.
-   No real-money deposits or withdrawals.

### Competition rules

1.  Every participant gets an isolated competition account.
2.  Participants cannot join after the start time unless the rules
    explicitly allow it.
3.  Users cannot add virtual capital after the competition begins.
4.  All participants use the same market data source and execution
    policy.
5.  Portfolio valuation uses a consistent timestamp.
6.  Leaderboards should handle ties consistently.
7.  Admin actions and rule changes must be audited.

Possible extensions: - Private competitions. - College-wide
competitions. - Weekly and monthly challenges. - Team competitions. -
Historical-market challenges.

------------------------------------------------------------------------

## 15. Security requirements

Even though the balances are virtual, user accounts and records must be
protected.

  Measure                                 Purpose
  --------------------------------------- ----------------------------------
  BCrypt or Argon2                        Password hashing
  Spring Security                         Authentication and authorization
  Secure session or token design          Session management
  Role-based access                       Admin/user permissions
  HTTPS                                   Encrypted transport
  Input validation                        Reject malformed data
  Rate limiting                           Reduce abuse
  Parameterized SQL                       Prevent SQL injection
  CORS restrictions                       Limit browser origins
  Environment variables/secrets manager   Protect API credentials
  Audit logs                              Track sensitive actions

Critical rules:

1.  Never store plaintext passwords.
2.  Never expose market-data API credentials in frontend code.
3.  Never accept the wallet balance from the frontend.
4.  Never allow users to change their own balance through public
    endpoints.
5.  Enforce ownership checks on every wallet, order, trade, and
    portfolio query.
6.  Make order submission idempotent.
7.  Keep wallet ledger entries immutable.
8.  Avoid logging passwords, tokens, or provider secrets.
9.  Validate WebSocket authorization as carefully as REST authorization.
10. Restrict admin operations and audit them.

------------------------------------------------------------------------

## 16. Testing plan

Use JUnit 5, Mockito, Spring Boot Test, and Testcontainers with
PostgreSQL.

### Functional tests

  Test                               Expected result
  ---------------------------------- ------------------------------------
  Buy with sufficient cash           Order succeeds
  Buy with insufficient cash         Order rejected
  Sell more shares than owned        Order rejected
  Unknown instrument                 Order rejected
  Negative quantity                  Order rejected
  Stale quote                        Order rejected
  Market closed                      Market order rejected in live mode
  Duplicate order request            No duplicate execution
  Cancel filled order                Cancellation rejected
  Limit condition reached            Order executes under defined rules
  User reads another user's wallet   Access denied
  Provider disconnects               Feed marked unavailable/stale

### Concurrency test

Create a test with multiple threads submitting purchases against one
wallet. The total successful debit must never exceed the available
balance. Also test simultaneous sell requests against the same holding.

Test the real database locking behavior using PostgreSQL integration
tests. A unit test with mocked repositories cannot prove database
concurrency correctness.

### Other tests

-   Average cost after multiple buys.
-   Partial sale and remaining cost basis.
-   Realized and unrealized P&L.
-   Fee calculations.
-   Ledger reconciliation.
-   Competition ranking.
-   Quote normalization and stale-feed handling.
-   WebSocket reconnect behavior.

------------------------------------------------------------------------

## 17. Development roadmap

Build each phase into a working deliverable.

### Phase 1 --- Foundation

-   Create Spring Boot project.
-   Configure PostgreSQL.
-   Configure Flyway.
-   Create user registration and login.
-   Configure Spring Security.
-   Create wallet provisioning.
-   Add stock master data.

**Deliverable:** A user can register, log in, and receive ₹1 lakh
virtual capital.

### Phase 2 --- Market data

-   Define `MarketDataProvider`.
-   Implement mock provider.
-   Integrate one real provider.
-   Normalize quote data.
-   Track quote freshness and data mode.
-   Build stock listing and quote APIs.
-   Add WebSocket broadcasting.
-   Build market dashboard.

**Deliverable:** Stock prices appear and update, with mock fallback
clearly labelled.

### Phase 3 --- Trading engine

-   Implement market orders.
-   Implement order validation.
-   Implement wallet accounting.
-   Implement buy and sell operations.
-   Update holdings and average cost.
-   Record orders, trades, and ledger entries.
-   Add database locking and idempotency.
-   Write concurrency tests.

**Deliverable:** Users can execute virtual trades safely.

### Phase 4 --- Portfolio

-   Calculate account value.
-   Calculate realized and unrealized P&L.
-   Build holdings screen.
-   Build order and trade history.
-   Add portfolio snapshots.
-   Add portfolio charts.
-   Add watchlists.

**Deliverable:** Users can inspect complete portfolio performance.

### Phase 5 --- Advanced features

-   Limit orders.
-   Pending-order cancellation.
-   Price alerts.
-   Competitions.
-   Competition accounts.
-   Leaderboards.
-   Historical replay.
-   Performance analytics.

**Deliverable:** A competitive paper-trading platform.

### Phase 6 --- Quality and deployment

-   Complete unit and integration tests.
-   Test concurrency and authorization.
-   Add API documentation.
-   Configure Docker Compose.
-   Deploy frontend and backend.
-   Configure production database and secrets.
-   Prepare report, diagrams, and demo.

**Deliverable:** A documented, deployable application ready for
evaluation.

------------------------------------------------------------------------

## 18. Deployment plan

Possible hosting choices:

  Component          Options
  ------------------ -------------------------------------------------------
  Frontend           Vercel or static hosting
  Backend            VPS, Railway, Render, or another Java-compatible host
  PostgreSQL         Managed PostgreSQL provider
  Redis              Optional managed Redis
  Domain             Custom domain
  Containerization   Docker Compose

For the first deployment, Docker Compose can run the backend and
PostgreSQL together. Keep production secrets outside the repository.
Configure HTTPS, database backups, health checks, logs, and monitoring.

A market-data WebSocket connection may need a long-running backend
process; confirm that the hosting platform supports persistent
connections and background workers.

------------------------------------------------------------------------

## 19. Project differentiators

Prioritize these features to make the project more than a basic CRUD
application.

### 1. Real market integration

Use actual NSE/BSE data where authorized. Label live, delayed,
historical, and simulated data correctly.

### 2. Reliable trading engine

Demonstrate transactional accounting, locking, idempotency, and safe
concurrent execution.

### 3. Campus competitions

Let students create contests and compete with equal virtual capital.

### 4. Historical replay

Allow users to select a historical market session and practice using
historical prices. Make it clear that this is a replay, not live data.

### 5. Performance analytics

Include: - Win rate. - Average profit per winning trade. - Average loss
per losing trade. - Largest drawdown. - Best and worst trades. -
Portfolio return. - Performance over time.

Do not present these metrics as investment advice or predictions.

------------------------------------------------------------------------

## 20. Learning prerequisites

Learn in this order. You can learn each topic while implementing the
related module.

  Priority   Topic             Required knowledge
  ---------- ----------------- ------------------------------------------------
  1          Java OOP          Classes, interfaces, inheritance, polymorphism
  2          Collections       List, Map, Set, HashMap
  3          Exceptions        Custom exceptions and error handling
  4          Multithreading    Threads, synchronization, executors
  5          SQL               Joins, indexes, constraints, transactions
  6          JDBC              Connections, PreparedStatement, ResultSet
  7          Spring Boot       Controllers, services, dependency injection
  8          Hibernate/JPA     Entities, relationships, transactions
  9          Spring Security   Authentication and authorization
  10         REST APIs         HTTP, JSON, status codes
  11         WebSockets        Persistent connections and broadcasting
  12         React             Components, hooks, API integration

### Learning resources

-   Java: https://dev.java/learn/
-   Spring guides: https://spring.io/guides
-   Spring documentation: https://docs.spring.io/spring-boot/
-   PostgreSQL: https://www.postgresql.org/docs/
-   Baeldung: https://www.baeldung.com/
-   JUnit 5: https://junit.org/junit5/docs/current/user-guide/
-   Upstox API: https://upstox.com/developer/api-documentation/
-   Dhan API: https://dhanhq.co/docs/v2/
-   Kite Connect: https://kite.trade/docs/connect/v3/

------------------------------------------------------------------------

## 21. Feature priority checklist

### MVP --- Must have

-   [ ] User registration and login
-   [ ] Virtual wallet with ₹1 lakh
-   [ ] Indian stock listing
-   [ ] Market quote dashboard
-   [ ] Stock price chart
-   [ ] Buy and sell market orders
-   [ ] Order history
-   [ ] Portfolio and holdings
-   [ ] Realized and unrealized P&L
-   [ ] Database persistence
-   [ ] Mock market-data mode
-   [ ] Secure account ownership checks

### Advanced --- Build next

-   [ ] WebSocket price updates
-   [ ] Limit orders
-   [ ] Watchlists
-   [ ] Price alerts
-   [ ] Trading competitions
-   [ ] Leaderboards
-   [ ] Historical trading mode
-   [ ] Portfolio analytics
-   [ ] Configurable fees
-   [ ] Audit and reconciliation tools

### Extra --- If time permits

-   [ ] Multiple exchanges
-   [ ] Corporate actions
-   [ ] Admin dashboard
-   [ ] Team competitions
-   [ ] Advanced execution simulation
-   [ ] Exportable performance reports

------------------------------------------------------------------------

## 22. Formal project specification

**Title:** TradeLab --- Real-Market-Data Virtual Stock Trading and
Portfolio Simulation Platform

### Objective

Develop a web-based paper-trading platform that enables users to
simulate trading in Indian equities using virtual money and actual,
delayed, or historical market prices. The system will provide order
management, virtual wallet accounting, portfolio valuation, performance
analytics, and trading competitions.

### Core modules

1.  User authentication and management.
2.  Market-data integration and quote distribution.
3.  Virtual wallet and ledger management.
4.  Order validation and simulated execution.
5.  Holdings and portfolio management.
6.  Competition and leaderboard management.
7.  Analytics and reporting.

### Technologies

Java, Spring Boot, Spring Security, Hibernate, JDBC, PostgreSQL, React,
TypeScript, WebSockets, Maven, JUnit, and Docker.

### Advanced Java concepts demonstrated

-   Object-oriented programming.
-   Collections Framework.
-   Exception handling.
-   Multithreading and executor services.
-   Synchronization and concurrency control.
-   JDBC and database transactions.
-   Hibernate/JPA.
-   REST API development.
-   WebSocket communication.
-   Scheduled background processing.

### Expected outcome

A functional paper-trading application where multiple users can view
market data, place simulated trades, manage virtual portfolios, and
compete using consistent trading rules without risking real money.

------------------------------------------------------------------------

## 23. Decisions to finalize before implementation

These decisions affect the architecture and workload. Fill them in
before choosing the provider and generating the initial codebase.

  -----------------------------------------------------------------------
  Decision                            Options
  ----------------------------------- -----------------------------------
  Team size                           Individual / 2 / 3--4 / 5+

  Market                              NSE+BSE / US / Both

  Data source                         Real + mock fallback / Real only /
                                      Historical / Mock only

  Stack                               Spring Boot + React / Servlets +
                                      JSP + JDBC / JavaFX

  Target                              College submission / Portfolio
                                      product / Competition-level

  Timeline                            1--2 weeks / 3--4 weeks / 4--6
                                      weeks / 2--3 months
  -----------------------------------------------------------------------

### Recommended defaults

If no constraints are imposed:

-   Team: 2--4 members.
-   Market: NSE stocks first.
-   Data: Real provider behind an adapter, plus mock fallback.
-   Stack: Spring Boot + React + PostgreSQL.
-   Scope: Complete MVP, then competitions and analytics.
-   Architecture: Modular monolith.
-   Initial virtual balance: ₹1,00,000.
-   Execution: LTP-based market orders first, with clearly documented
    limitations.

------------------------------------------------------------------------

## 24. Implementation order: first 10 tasks

Do these in order. Do not start by building charts or polishing the UI.

1.  Confirm course restrictions, team size, and deadline.
2.  Create the Git repository and project README.
3.  Generate the Spring Boot project with Web, Validation, Security,
    Data JPA, PostgreSQL, Flyway, and test dependencies.
4.  Create the PostgreSQL database and first Flyway migration.
5.  Implement user registration, password hashing, and login.
6.  Implement wallet creation and immutable ledger entries.
7.  Add stock master data and a mock market-data provider.
8.  Implement quote retrieval and stock listing APIs.
9.  Implement one transactional market-buy and market-sell workflow.
10. Test insufficient balance, insufficient holdings, duplicate
    requests, and concurrent orders before adding more features.

**Core engineering rule:** correctness of the wallet, ledger, order, and
holdings matters more than the number of screens or features. Build and
test the accounting engine before expanding the product.
