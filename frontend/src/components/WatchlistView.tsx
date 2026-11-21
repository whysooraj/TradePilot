import React from 'react';
import { Star, TrendingUp, TrendingDown, Trash2 } from 'lucide-react';
import { StockQuote } from '../types';

interface WatchlistViewProps {
  quotes: StockQuote[];
  watchlistSymbols: string[];
  onSelectStock: (symbol: string) => void;
  onRemoveWatchlist: (symbol: string) => void;
  onExplore: () => void;
}

export const WatchlistView: React.FC<WatchlistViewProps> = ({
  quotes,
  watchlistSymbols,
  onSelectStock,
  onRemoveWatchlist,
  onExplore,
}) => {
  const watchedQuotes = quotes.filter((q) => watchlistSymbols.includes(q.symbol));

  return (
    <div className="space-y-5">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <Star size={18} className="text-amber-400" fill="currentColor" />
          <h3 className="text-sm font-semibold text-zinc-200">
            Saved Instruments Watchlist ({watchedQuotes.length})
          </h3>
        </div>
        <button
          onClick={onExplore}
          className="text-xs text-zinc-400 hover:text-zinc-200 transition"
        >
          + Add more instruments
        </button>
      </div>

      <div className="rounded-2xl bg-surface border border-border overflow-hidden">
        {watchedQuotes.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs font-mono">
              <thead>
                <tr className="border-b border-border/80 text-zinc-500 bg-surface-elevated/40 font-sans">
                  <th className="py-3 px-4 font-medium">Instrument</th>
                  <th className="py-3 px-4 font-medium text-right">LTP (₹)</th>
                  <th className="py-3 px-4 font-medium text-right">Day Change</th>
                  <th className="py-3 px-4 font-medium text-right">High / Low</th>
                  <th className="py-3 px-4 font-medium text-center">Trade</th>
                  <th className="py-3 px-4 font-medium text-center">Remove</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border/40">
                {watchedQuotes.map((stock) => {
                  const isPositive = stock.change >= 0;
                  return (
                    <tr key={stock.symbol} className="hover:bg-surface-elevated/50 transition">
                      <td className="py-3 px-4 font-sans font-semibold text-zinc-100">
                        {stock.symbol}
                        <span className="block text-[11px] text-zinc-500 font-normal">{stock.companyName}</span>
                      </td>
                      <td className="py-3 px-4 text-right font-bold text-sm text-zinc-100">
                        ₹{stock.lastTradedPrice.toFixed(2)}
                      </td>
                      <td className={`py-3 px-4 text-right font-medium ${isPositive ? 'text-emerald-400' : 'text-rose-400'}`}>
                        {isPositive ? '+' : ''}{stock.change.toFixed(2)} ({stock.changePercent.toFixed(2)}%)
                      </td>
                      <td className="py-3 px-4 text-right text-zinc-400 text-[11px]">
                        <span className="text-emerald-400">₹{stock.high.toFixed(2)}</span>
                        <span className="mx-1 text-zinc-600">/</span>
                        <span className="text-rose-400">₹{stock.low.toFixed(2)}</span>
                      </td>
                      <td className="py-3 px-4 text-center">
                        <button
                          onClick={() => onSelectStock(stock.symbol)}
                          className="px-3 py-1.5 rounded-lg bg-zinc-800 hover:bg-emerald-600 hover:text-white text-zinc-300 font-sans font-semibold text-xs border border-zinc-700 transition"
                        >
                          Trade
                        </button>
                      </td>
                      <td className="py-3 px-4 text-center">
                        <button
                          onClick={() => onRemoveWatchlist(stock.symbol)}
                          className="p-1.5 text-zinc-500 hover:text-rose-400 rounded transition"
                          title="Remove from watchlist"
                        >
                          <Trash2 size={14} />
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
            No instruments saved in your watchlist yet. Star any stock in the Market list to track it here.
          </div>
        )}
      </div>
    </div>
  );
};
