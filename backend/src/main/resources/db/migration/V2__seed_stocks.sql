-- V2__seed_stocks.sql
-- Seed top Indian Equities on NSE

INSERT INTO stocks (id, symbol, company_name, exchange, provider_instrument_token, active)
VALUES
    ('a0000000-0000-0000-0000-000000000001', 'RELIANCE', 'Reliance Industries Ltd.', 'NSE', '738561', true),
    ('a0000000-0000-0000-0000-000000000002', 'TCS', 'Tata Consultancy Services Ltd.', 'NSE', '2953217', true),
    ('a0000000-0000-0000-0000-000000000003', 'HDFCBANK', 'HDFC Bank Ltd.', 'NSE', '341249', true),
    ('a0000000-0000-0000-0000-000000000004', 'INFY', 'Infosys Ltd.', 'NSE', '408065', true),
    ('a0000000-0000-0000-0000-000000000005', 'ICICIBANK', 'ICICI Bank Ltd.', 'NSE', '1270529', true),
    ('a0000000-0000-0000-0000-000000000006', 'TATAMOTORS', 'Tata Motors Passenger Vehicles Ltd.', 'NSE', '884737', true),
    ('a0000000-0000-0000-0000-000000000007', 'SBIN', 'State Bank of India', 'NSE', '779521', true),
    ('a0000000-0000-0000-0000-000000000008', 'BHARTIARTL', 'Bharti Airtel Ltd.', 'NSE', '2714625', true),
    ('a0000000-0000-0000-0000-000000000009', 'ITC', 'ITC Ltd.', 'NSE', '424961', true),
    ('a0000000-0000-0000-0000-000000000010', 'LT', 'Larsen & Toubro Ltd.', 'NSE', '2939649', true),
    ('a0000000-0000-0000-0000-000000000011', 'WIPRO', 'Wipro Ltd.', 'NSE', '969473', true),
    ('a0000000-0000-0000-0000-000000000012', 'BAJFINANCE', 'Bajaj Finance Ltd.', 'NSE', '81153', true)
ON CONFLICT (symbol, exchange) DO NOTHING;

-- Seed initial default competition
INSERT INTO competitions (id, name, starting_balance, start_time, end_time, status)
VALUES (
    'b0000000-0000-0000-0000-000000000001',
    'TradeLab National Paper-Trading Championship',
    100000.0000,
    NOW() - INTERVAL '1 day',
    NOW() + INTERVAL '30 days',
    'ACTIVE'
) ON CONFLICT (id) DO NOTHING;
