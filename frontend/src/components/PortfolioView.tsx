import React, { useState } from 'react';
import { Database, TrendingUp, TrendingDown, RefreshCw, AlertCircle } from 'lucide-react';
import { PortfolioSummary, Holding } from '../types';
import { portfolioApi } from '../services/api';

interface PortfolioViewProps {
  portfolio: PortfolioSummary | null;
  onTradeStock: (symbol: string) => void;
  onRefresh: () => void;
}

export const PortfolioView: React.FC<PortfolioViewProps> = ({
  portfolio,
  onTradeStock,
  onRefresh,
}) => {
  const [sqlAnalytics, setSqlAnalytics] = useState<any | null>(null);
  const [loadingSql, setLoadingSql] = useState(false);
  const [showSqlModal, setShowSqlModal] = useState(false);

  const fetchSqlAnalytics = async () => {
    setLoadingSql(true);
    try {
      const data = await portfolioApi.getAnalyticsSql();
      setSqlAnalytics(data);
      setShowSqlModal(true);
    } catch (err) {
      console.error('Failed to load SQL analytics', err);
    } finally {
      setLoadingSql(false);
    }
  };

  if (!portfolio) {
    return (
      <div className="flex items-center justify-center p-12 text-zinc-400">
        <RefreshCw className="animate-spin mr-2" size={16} /> Loading portfolio data...
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Portfolio Header Bar */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-surface p-5 rounded-2xl border border-border">
        <div>
          <span className="text-xs text-zinc-400 uppercase tracking-wider font-semibold">Total Portfolio Valuation</span>
          <div className="text-3xl font-bold font-mono text-zinc-100 mt-1">
            ₹{portfolio.totalAccountValue.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
          </div>
          <div className={`flex items-center gap-2 mt-1.5 text-xs font-mono font-medium ${portfolio.totalPnl >= 0 ? 'text-emerald-400' : 'text-rose-400'}`}>
            <span>Total P&L: {portfolio.totalPnl >= 0 ? '+' : ''}₹{portfolio.totalPnl.toFixed(2)} ({portfolio.returnPercentage.toFixed(2)}%)</span>
          </div>
        </div>

        {/* Actions & JDBC demonstration trigger */}
        <div className="flex items-center gap-3">
          <button
            onClick={fetchSqlAnalytics}
            disabled={loadingSql}
            className="flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-surface-elevated hover:bg-zinc-800 border border-border text-xs font-semibold text-zinc-200 transition"
            title="Execute direct JDBC SQL reporting query on PostgreSQL"
          >
            <Database size={14} className="text-emerald-400" />
            <span>{loadingSql ? 'Executing SQL...' : 'Direct JDBC Report'}</span>
          </button>
          <button
            onClick={onRefresh}
            className="p-2 rounded-xl bg-surface-elevated hover:bg-zinc-800 border border-border text-zinc-400 hover:text-zinc-100 transition"
            title="Refresh Portfolio"
          >
            <RefreshCw size={15} />
          </button>
        </div>
      </div>

      {/* Metrics Row */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-xs font-mono">
        <div className="p-4 rounded-xl bg-surface border border-border">
          <div className="text-zinc-400 font-sans">Available Cash</div>
          <div className="text-lg font-bold text-emerald-400 mt-1">₹{portfolio.availableCash.toLocaleString()}</div>
        </div>
        <div className="p-4 rounded-xl bg-surface border border-border">
          <div className="text-zinc-400 font-sans">Locked In Orders</div>
          <div className="text-lg font-bold text-zinc-200 mt-1">₹{portfolio.reservedCash.toLocaleString()}</div>
        </div>
        <div className="p-4 rounded-xl bg-surface border border-border">
          <div className="text-zinc-400 font-sans">Holdings Invested</div>
          <div className="text-lg font-bold text-zinc-200 mt-1">₹{portfolio.investedValue.toLocaleString()}</div>
        </div>
        <div className="p-4 rounded-xl bg-surface border border-border">
          <div className="text-zinc-400 font-sans">Unrealized P&L</div>
          <div className={`text-lg font-bold mt-1 ${portfolio.unrealizedPnl >= 0 ? 'text-emerald-400' : 'text-rose-400'}`}>
            {portfolio.unrealizedPnl >= 0 ? '+' : ''}₹{portfolio.unrealizedPnl.toFixed(2)}
          </div>
        </div>
      </div>

      {/* Holdings Table */}
      <div className="rounded-2xl bg-surface border border-border overflow-hidden">
        <div className="p-4 border-b border-border flex items-center justify-between">
          <h3 className="text-sm font-semibold text-zinc-200">Current Stock Holdings ({portfolio.holdings.length})</h3>
        </div>

        {portfolio.holdings.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs font-mono">
              <thead>
                <tr className="border-b border-border/80 text-zinc-500 bg-surface-elevated/40 font-sans">
                  <th className="py-3 px-4 font-medium">Instrument</th>
                  <th className="py-3 px-4 font-medium text-right">Shares</th>
                  <th className="py-3 px-4 font-medium text-right">Avg Cost</th>
                  <th className="py-3 px-4 font-medium text-right">Current LTP</th>
                  <th className="py-3 px-4 font-medium text-right">Invested</th>
                  <th className="py-3 px-4 font-medium text-right">Current Value</th>
                  <th className="py-3 px-4 font-medium text-right">Unrealized P&L</th>
                  <th className="py-3 px-4 font-medium text-center">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border/40">
                {portfolio.holdings.map((h) => {
                  const isPositive = h.unrealizedPnl >= 0;
                  return (
                    <tr key={h.symbol} className="hover:bg-surface-elevated/50 transition">
                      <td className="py-3 px-4 font-sans font-semibold text-zinc-200">
                        {h.symbol}
                        <span className="block text-[11px] text-zinc-500 font-normal">{h.companyName}</span>
                      </td>
                      <td className="py-3 px-4 text-right text-zinc-200 font-semibold">{h.quantity}</td>
                      <td className="py-3 px-4 text-right text-zinc-400">₹{h.averagePrice.toFixed(2)}</td>
                      <td className="py-3 px-4 text-right text-zinc-200">₹{h.currentPrice.toFixed(2)}</td>
                      <td className="py-3 px-4 text-right text-zinc-300">₹{h.investedValue.toLocaleString()}</td>
                      <td className="py-3 px-4 text-right text-zinc-100 font-bold">₹{h.marketValue.toLocaleString()}</td>
                      <td className={`py-3 px-4 text-right font-medium ${isPositive ? 'text-emerald-400' : 'text-rose-400'}`}>
                        {isPositive ? '+' : ''}₹{h.unrealizedPnl.toFixed(2)} ({h.returnPercentage.toFixed(2)}%)
                      </td>
                      <td className="py-3 px-4 text-center">
                        <button
                          onClick={() => onTradeStock(h.symbol)}
                          className="px-3 py-1 rounded-md bg-rose-950/60 hover:bg-rose-600 border border-rose-800 text-rose-300 hover:text-white font-sans text-xs transition font-semibold"
                        >
                          Trade / Exit
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        ) : (
          <div className="py-12 text-center text-zinc-500 text-xs">
            No active positions. Explore the market to buy your first shares with virtual ₹1 Lakh!
          </div>
        )}
      </div>

      {/* Direct JDBC Demonstration Modal */}
      {showSqlModal && sqlAnalytics && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-sm p-4">
          <div className="w-full max-w-lg bg-surface border border-border rounded-2xl shadow-2xl p-6 space-y-4">
            <div className="flex items-center justify-between border-b border-border pb-3">
              <div className="flex items-center gap-2">
                <Database size={16} className="text-emerald-400" />
                <h3 className="font-semibold text-sm text-zinc-100">Direct JDBC Aggregation Report</h3>
              </div>
              <button
                onClick={() => setShowSqlModal(false)}
                className="text-zinc-400 hover:text-zinc-100 text-sm font-mono"
              >
                ✕
              </button>
            </div>

            <p className="text-xs text-zinc-400">
              Demonstrates raw SQL execution via Spring <code className="bg-zinc-800 px-1 py-0.5 rounded text-emerald-400 font-mono">JdbcTemplate</code> querying PostgreSQL tables (<code className="font-mono text-zinc-300">orders</code>, <code className="font-mono text-zinc-300">trades</code>, <code className="font-mono text-zinc-300">holdings</code>) with aggregate group-bys.
            </p>

            <div className="grid grid-cols-2 gap-3 text-xs font-mono bg-surface-elevated p-4 rounded-xl border border-border/80">
              <div>
                <span className="text-zinc-500 font-sans">Total Orders</span>
                <div className="text-sm font-bold text-zinc-200 mt-0.5">{sqlAnalytics.totalOrders}</div>
              </div>
              <div>
                <span className="text-zinc-500 font-sans">Total Executed Trades</span>
                <div className="text-sm font-bold text-zinc-200 mt-0.5">{sqlAnalytics.totalTrades}</div>
              </div>
              <div>
                <span className="text-zinc-500 font-sans">Total Trading Turnover</span>
                <div className="text-sm font-bold text-emerald-400 mt-0.5">₹{Number(sqlAnalytics.totalTurnover).toLocaleString()}</div>
              </div>
              <div>
                <span className="text-zinc-500 font-sans">Largest Single Trade</span>
                <div className="text-sm font-bold text-zinc-200 mt-0.5">₹{Number(sqlAnalytics.largestTrade).toLocaleString()}</div>
              </div>
              <div>
                <span className="text-zinc-500 font-sans">Cumulative Buy Volume</span>
                <div className="text-sm font-bold text-zinc-300 mt-0.5">₹{Number(sqlAnalytics.totalBought).toLocaleString()}</div>
              </div>
              <div>
                <span className="text-zinc-500 font-sans">Cumulative Sell Volume</span>
                <div className="text-sm font-bold text-zinc-300 mt-0.5">₹{Number(sqlAnalytics.totalSold).toLocaleString()}</div>
              </div>
            </div>

            <div className="text-right">
              <button
                onClick={() => setShowSqlModal(false)}
                className="px-4 py-2 rounded-lg bg-zinc-800 hover:bg-zinc-700 text-xs font-semibold text-zinc-200 transition"
              >
                Close Report
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
