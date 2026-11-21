import React, { useState } from 'react';
import { Search, TrendingUp, TrendingDown, Star, ArrowUpDown } from 'lucide-react';
import { StockQuote } from '../types';

interface ExploreViewProps {
  quotes: StockQuote[];
  watchlistSymbols: string[];
  onSelectStock: (symbol: string) => void;
  onToggleWatchlist: (symbol: string) => void;
}

export const ExploreView: React.FC<ExploreViewProps> = ({
  quotes,
  watchlistSymbols,
  onSelectStock,
  onToggleWatchlist,
}) => {
  const [search, setSearch] = useState('');
  const [filter, setFilter] = useState<'ALL' | 'GAINERS' | 'LOSERS'>('ALL');

  const filteredQuotes = quotes
    .filter((q) => {
      const matchesSearch =
        q.symbol.toLowerCase().includes(search.toLowerCase()) ||
        q.companyName.toLowerCase().includes(search.toLowerCase());
      if (!matchesSearch) return false;

      if (filter === 'GAINERS') return q.change >= 0;
      if (filter === 'LOSERS') return q.change < 0;
      return true;
    });

  return (
    <div className="space-y-5">
      {/* Search and Filter Controls */}
      <div className="flex flex-col sm:flex-row items-center justify-between gap-3">
        <div className="relative w-full sm:w-80">
          <Search size={15} className="absolute left-3.5 top-1/2 -translate-y-1/2 text-zinc-500" />
          <input
            type="text"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search NSE stock symbols, companies..."
            className="w-full bg-surface border border-border rounded-xl pl-9 pr-4 py-2 text-xs font-mono text-zinc-100 placeholder:text-zinc-500 focus:outline-none focus:border-zinc-500"
          />
        </div>

        {/* Filters */}
        <div className="flex items-center gap-1.5 p-1 bg-surface border border-border rounded-xl text-xs">
          {(['ALL', 'GAINERS', 'LOSERS'] as const).map((mode) => (
            <button
              key={mode}
              onClick={() => setFilter(mode)}
              className={`px-3 py-1.5 rounded-lg font-medium transition ${
                filter === mode
                  ? 'bg-zinc-800 text-zinc-100 shadow-sm border border-zinc-700/60'
                  : 'text-zinc-400 hover:text-zinc-200'
              }`}
            >
              {mode}
            </button>
          ))}
        </div>
      </div>

      {/* Real-Market Indian Equities Table */}
      <div className="rounded-2xl bg-surface border border-border overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead>
              <tr className="border-b border-border text-zinc-500 bg-surface-elevated/40">
                <th className="py-3 px-4 font-medium w-10"></th>
                <th className="py-3 px-4 font-medium">Instrument</th>
                <th className="py-3 px-4 font-medium text-right">LTP (₹)</th>
                <th className="py-3 px-4 font-medium text-right">Day Change</th>
                <th className="py-3 px-4 font-medium text-right">Day High / Low</th>
                <th className="py-3 px-4 font-medium text-right">Traded Volume</th>
                <th className="py-3 px-4 font-medium text-center">Action</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border/40 font-mono">
              {filteredQuotes.map((stock) => {
                const isWatched = watchlistSymbols.includes(stock.symbol);
                const isPositive = stock.change >= 0;

                return (
                  <tr
                    key={stock.symbol}
                    className="hover:bg-surface-elevated/50 transition group"
                  >
                    {/* Star Watchlist */}
                    <td className="py-3 px-4 text-center">
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          onToggleWatchlist(stock.symbol);
                        }}
                        className={`transition ${isWatched ? 'text-amber-400' : 'text-zinc-600 hover:text-zinc-400'}`}
                        title={isWatched ? 'Remove from Watchlist' : 'Add to Watchlist'}
                      >
                        <Star size={14} fill={isWatched ? 'currentColor' : 'none'} />
                      </button>
                    </td>

                    {/* Stock Symbol & Company */}
                    <td
                      onClick={() => onSelectStock(stock.symbol)}
                      className="py-3 px-4 cursor-pointer"
                    >
                      <div className="font-semibold text-zinc-100 group-hover:text-emerald-400 transition font-sans">
                        {stock.symbol}
                      </div>
                      <div className="text-[11px] text-zinc-400 font-sans truncate max-w-[200px]">
                        {stock.companyName}
                      </div>
                    </td>

                    {/* LTP */}
                    <td
                      onClick={() => onSelectStock(stock.symbol)}
                      className="py-3 px-4 text-right font-bold text-sm text-zinc-100 cursor-pointer"
                    >
                      ₹{stock.lastTradedPrice.toFixed(2)}
                    </td>

                    {/* Day Change */}
                    <td
                      onClick={() => onSelectStock(stock.symbol)}
                      className={`py-3 px-4 text-right font-medium cursor-pointer ${
                        isPositive ? 'text-emerald-400' : 'text-rose-400'
                      }`}
                    >
                      <div className="flex items-center justify-end gap-1">
                        {isPositive ? <TrendingUp size={12} /> : <TrendingDown size={12} />}
                        <span>
                          {isPositive ? '+' : ''}
                          {stock.change.toFixed(2)} ({stock.changePercent.toFixed(2)}%)
                        </span>
                      </div>
                    </td>

                    {/* High / Low */}
                    <td
                      onClick={() => onSelectStock(stock.symbol)}
                      className="py-3 px-4 text-right text-zinc-400 text-[11px] cursor-pointer"
                    >
                      <span className="text-emerald-400">₹{stock.high.toFixed(2)}</span>
                      <span className="mx-1 text-zinc-600">/</span>
                      <span className="text-rose-400">₹{stock.low.toFixed(2)}</span>
                    </td>

                    {/* Volume */}
                    <td
                      onClick={() => onSelectStock(stock.symbol)}
                      className="py-3 px-4 text-right text-zinc-400 cursor-pointer"
                    >
                      {stock.volume.toLocaleString()}
                    </td>

                    {/* Trade Button */}
                    <td className="py-3 px-4 text-center">
                      <button
                        onClick={() => onSelectStock(stock.symbol)}
                        className="px-3 py-1.5 rounded-lg bg-zinc-800 hover:bg-emerald-600 hover:text-white text-zinc-300 font-sans font-semibold text-xs border border-zinc-700/80 hover:border-emerald-500 transition shadow-sm"
                      >
                        Trade
                      </button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};
