import React from 'react';
import { TrendingUp, TrendingDown, ArrowUpRight, ArrowDownRight, IndianRupee, ShieldCheck } from 'lucide-react';
import { PortfolioSummary, StockQuote } from '../types';

interface DashboardViewProps {
  portfolio: PortfolioSummary | null;
  quotes: StockQuote[];
  onSelectStock: (symbol: string) => void;
  onNavigate: (tab: string) => void;
}

export const DashboardView: React.FC<DashboardViewProps> = ({
  portfolio,
  quotes,
  onSelectStock,
  onNavigate,
}) => {
  const topGainers = [...quotes].sort((a, b) => b.changePercent - a.changePercent).slice(0, 4);
  const topLosers = [...quotes].sort((a, b) => a.changePercent - b.changePercent).slice(0, 4);

  const totalAccountValue = portfolio ? portfolio.totalAccountValue : 100000;
  const availableCash = portfolio ? portfolio.availableCash : 100000;
  const investedValue = portfolio ? portfolio.investedValue : 0;
  const unrealizedPnl = portfolio ? portfolio.unrealizedPnl : 0;
  const realizedPnl = portfolio ? portfolio.realizedPnl : 0;
  const totalPnl = portfolio ? portfolio.totalPnl : 0;
  const returnPercentage = portfolio ? portfolio.returnPercentage : 0;

  return (
    <div className="space-y-6">
      {/* Hero Metric Banner */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        {/* Total Net Worth */}
        <div className="p-5 rounded-2xl bg-surface border border-border">
          <div className="flex items-center justify-between text-xs text-zinc-400">
            <span>Total Account Value</span>
            <IndianRupee size={15} className="text-zinc-500" />
          </div>
          <div className="mt-2 text-2xl font-bold font-mono text-zinc-100">
            ₹{totalAccountValue.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
          </div>
          <div className={`mt-2 flex items-center gap-1 text-xs font-mono font-medium ${returnPercentage >= 0 ? 'text-emerald-400' : 'text-rose-400'}`}>
            {returnPercentage >= 0 ? <ArrowUpRight size={14} /> : <ArrowDownRight size={14} />}
            <span>{returnPercentage >= 0 ? '+' : ''}{returnPercentage.toFixed(2)}% Return</span>
          </div>
        </div>

        {/* Available Virtual Cash */}
        <div className="p-5 rounded-2xl bg-surface border border-border">
          <div className="flex items-center justify-between text-xs text-zinc-400">
            <span>Available Virtual Cash</span>
            <ShieldCheck size={15} className="text-emerald-400" />
          </div>
          <div className="mt-2 text-2xl font-bold font-mono text-emerald-400">
            ₹{availableCash.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
          </div>
          <div className="mt-2 text-xs text-zinc-500 font-mono">
            Reserved: ₹{(portfolio?.reservedCash || 0).toFixed(2)}
          </div>
        </div>

        {/* Invested Value */}
        <div className="p-5 rounded-2xl bg-surface border border-border">
          <div className="flex items-center justify-between text-xs text-zinc-400">
            <span>Holdings Invested</span>
            <TrendingUp size={15} className="text-zinc-500" />
          </div>
          <div className="mt-2 text-2xl font-bold font-mono text-zinc-100">
            ₹{investedValue.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
          </div>
          <div className="mt-2 text-xs text-zinc-500 font-mono">
            Market Val: ₹{(portfolio?.holdingsMarketValue || 0).toLocaleString()}
          </div>
        </div>

        {/* Total P&L */}
        <div className="p-5 rounded-2xl bg-surface border border-border">
          <div className="flex items-center justify-between text-xs text-zinc-400">
            <span>Total Simulated P&L</span>
            {totalPnl >= 0 ? <TrendingUp size={15} className="text-emerald-400" /> : <TrendingDown size={15} className="text-rose-400" />}
          </div>
          <div className={`mt-2 text-2xl font-bold font-mono ${totalPnl >= 0 ? 'text-emerald-400' : 'text-rose-400'}`}>
            {totalPnl >= 0 ? '+' : ''}₹{totalPnl.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
          </div>
          <div className="mt-2 flex items-center justify-between text-[11px] text-zinc-500 font-mono">
            <span>Unrealized: ₹{unrealizedPnl.toFixed(2)}</span>
            <span>Realized: ₹{realizedPnl.toFixed(2)}</span>
          </div>
        </div>
      </div>

      {/* Main Grid: Gainers & Losers */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Top Gainers */}
        <div className="rounded-2xl bg-surface border border-border p-5">
          <div className="flex items-center justify-between mb-4">
            <div className="flex items-center gap-2">
              <span className="w-2 h-2 rounded-full bg-emerald-500"></span>
              <h3 className="text-sm font-semibold text-zinc-200">Session Top Gainers</h3>
            </div>
            <button
              onClick={() => onNavigate('explore')}
              className="text-xs text-zinc-400 hover:text-zinc-200 transition"
            >
              View all
            </button>
          </div>
          <div className="space-y-2.5">
            {topGainers.map((stock) => (
              <div
                key={stock.symbol}
                onClick={() => onSelectStock(stock.symbol)}
                className="flex items-center justify-between p-3 rounded-xl bg-surface-elevated/40 hover:bg-surface-elevated border border-border/60 hover:border-border transition cursor-pointer"
              >
                <div>
                  <div className="font-semibold text-xs text-zinc-100">{stock.symbol}</div>
                  <div className="text-[11px] text-zinc-400 truncate max-w-[180px]">{stock.companyName}</div>
                </div>
                <div className="text-right">
                  <div className="font-mono text-xs font-semibold text-zinc-100">₹{stock.lastTradedPrice.toFixed(2)}</div>
                  <div className="font-mono text-[11px] font-medium text-emerald-400">
                    +{stock.changePercent.toFixed(2)}%
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Top Losers */}
        <div className="rounded-2xl bg-surface border border-border p-5">
          <div className="flex items-center justify-between mb-4">
            <div className="flex items-center gap-2">
              <span className="w-2 h-2 rounded-full bg-rose-500"></span>
              <h3 className="text-sm font-semibold text-zinc-200">Session Top Losers</h3>
            </div>
            <button
              onClick={() => onNavigate('explore')}
              className="text-xs text-zinc-400 hover:text-zinc-200 transition"
            >
              View all
            </button>
          </div>
          <div className="space-y-2.5">
            {topLosers.map((stock) => (
              <div
                key={stock.symbol}
                onClick={() => onSelectStock(stock.symbol)}
                className="flex items-center justify-between p-3 rounded-xl bg-surface-elevated/40 hover:bg-surface-elevated border border-border/60 hover:border-border transition cursor-pointer"
              >
                <div>
                  <div className="font-semibold text-xs text-zinc-100">{stock.symbol}</div>
                  <div className="text-[11px] text-zinc-400 truncate max-w-[180px]">{stock.companyName}</div>
                </div>
                <div className="text-right">
                  <div className="font-mono text-xs font-semibold text-zinc-100">₹{stock.lastTradedPrice.toFixed(2)}</div>
                  <div className="font-mono text-[11px] font-medium text-rose-400">
                    {stock.changePercent.toFixed(2)}%
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Quick Portfolio Holdings Overview */}
      <div className="rounded-2xl bg-surface border border-border p-5">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-sm font-semibold text-zinc-200">Active Portfolio Positions</h3>
          <button
            onClick={() => onNavigate('portfolio')}
            className="text-xs text-zinc-400 hover:text-zinc-200 transition"
          >
            Full portfolio details →
          </button>
        </div>

        {portfolio && portfolio.holdings && portfolio.holdings.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead>
                <tr className="border-b border-border/80 text-zinc-500 pb-2">
                  <th className="pb-3 font-medium">Instrument</th>
                  <th className="pb-3 font-medium text-right">Shares</th>
                  <th className="pb-3 font-medium text-right">Avg Cost</th>
                  <th className="pb-3 font-medium text-right">LTP</th>
                  <th className="pb-3 font-medium text-right">Current Value</th>
                  <th className="pb-3 font-medium text-right">Unrealized P&L</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border/40">
                {portfolio.holdings.map((h) => (
                  <tr
                    key={h.symbol}
                    onClick={() => onSelectStock(h.symbol)}
                    className="hover:bg-surface-elevated/50 transition cursor-pointer"
                  >
                    <td className="py-3 font-semibold text-zinc-200">
                      {h.symbol}
                      <span className="block text-[10px] text-zinc-500 font-normal">{h.companyName}</span>
                    </td>
                    <td className="py-3 text-right font-mono text-zinc-300">{h.quantity}</td>
                    <td className="py-3 text-right font-mono text-zinc-300">₹{h.averagePrice.toFixed(2)}</td>
                    <td className="py-3 text-right font-mono text-zinc-200">₹{h.currentPrice.toFixed(2)}</td>
                    <td className="py-3 text-right font-mono text-zinc-200 font-semibold">₹{h.marketValue.toLocaleString()}</td>
                    <td className={`py-3 text-right font-mono font-medium ${h.unrealizedPnl >= 0 ? 'text-emerald-400' : 'text-rose-400'}`}>
                      {h.unrealizedPnl >= 0 ? '+' : ''}₹{h.unrealizedPnl.toFixed(2)} ({h.returnPercentage.toFixed(2)}%)
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <div className="text-center py-10 border border-dashed border-border rounded-xl">
            <p className="text-xs text-zinc-400">No active stock holdings in your portfolio yet.</p>
            <button
              onClick={() => onNavigate('explore')}
              className="mt-3 px-4 py-2 rounded-lg bg-emerald-600 hover:bg-emerald-500 text-xs font-semibold text-white transition"
            >
              Explore Stocks to Trade
            </button>
          </div>
        )}
      </div>
    </div>
  );
};
