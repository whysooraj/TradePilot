import React, { useState } from 'react';
import { X, TrendingUp, TrendingDown, AlertCircle, CheckCircle2 } from 'lucide-react';
import { OrderSide, OrderType, StockQuote, Holding } from '../types';
import { orderApi } from '../services/api';
import { TradingViewChart } from './TradingViewChart';

interface TradeTicketModalProps {
  quote: StockQuote;
  candles: any[];
  availableCash: number;
  userHolding?: Holding;
  onClose: () => void;
  onOrderComplete: () => void;
}

export const TradeTicketModal: React.FC<TradeTicketModalProps> = ({
  quote,
  candles,
  availableCash,
  userHolding,
  onClose,
  onOrderComplete,
}) => {
  const [side, setSide] = useState<OrderSide>('BUY');
  const [orderType, setOrderType] = useState<OrderType>('MARKET');
  const [quantity, setQuantity] = useState<number>(1);
  const [limitPrice, setLimitPrice] = useState<string>(quote.lastTradedPrice.toFixed(2));
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const priceToUse =
    orderType === 'LIMIT'
      ? parseFloat(limitPrice) || quote.lastTradedPrice
      : side === 'BUY'
      ? quote.askPrice || quote.lastTradedPrice
      : quote.bidPrice || quote.lastTradedPrice;

  const estimatedTotal = (quantity || 0) * priceToUse;
  const ownedShares = userHolding ? userHolding.quantity : 0;

  const handleQuickQuantity = (qty: number) => {
    setQuantity(qty);
  };

  const handleMaxQuantity = () => {
    if (side === 'BUY') {
      const max = Math.floor(availableCash / (priceToUse || 1));
      setQuantity(Math.max(1, max));
    } else {
      setQuantity(Math.max(1, ownedShares));
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setSuccess(null);

    if (quantity <= 0) {
      setError('Quantity must be greater than zero');
      return;
    }

    if (side === 'BUY' && estimatedTotal > availableCash) {
      setError(`Insufficient available cash (Required: ₹${estimatedTotal.toLocaleString()}, Available: ₹${availableCash.toLocaleString()})`);
      return;
    }

    if (side === 'SELL' && quantity > ownedShares) {
      setError(`Insufficient shares (Owned: ${ownedShares}, Requested: ${quantity})`);
      return;
    }

    setIsSubmitting(true);
    try {
      const res = await orderApi.place({
        symbol: quote.symbol,
        exchange: quote.exchange,
        side,
        orderType,
        quantity,
        limitPrice: orderType === 'LIMIT' ? parseFloat(limitPrice) : null,
        idempotencyKey: `client-${Date.now()}-${Math.random().toString(36).substring(2, 9)}`,
      });

      setSuccess(`Order ${res.status.toLowerCase()}! ${side} ${quantity} ${quote.symbol} at ₹${res.executedPrice || res.limitPrice}`);
      setTimeout(() => {
        onOrderComplete();
        onClose();
      }, 1200);
    } catch (err: any) {
      setError(err.message || 'Failed to place order');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-sm p-4">
      <div className="relative w-full max-w-4xl bg-surface border border-border rounded-2xl shadow-2xl overflow-hidden flex flex-col md:flex-row max-h-[92vh]">
        {/* Left Side: Chart & Info */}
        <div className="flex-1 p-6 border-b md:border-b-0 md:border-r border-border flex flex-col justify-between overflow-y-auto">
          <div>
            <div className="flex items-center justify-between mb-4">
              <div>
                <div className="flex items-center gap-2">
                  <h2 className="text-xl font-bold tracking-tight text-zinc-100">{quote.symbol}</h2>
                  <span className="text-xs px-2 py-0.5 rounded bg-zinc-800 text-zinc-300 font-mono">
                    {quote.exchange}
                  </span>
                  <span className="text-xs px-2 py-0.5 rounded bg-emerald-950/60 border border-emerald-800 text-emerald-400 font-mono">
                    {quote.dataMode}
                  </span>
                </div>
                <p className="text-xs text-zinc-400 mt-0.5">{quote.companyName}</p>
              </div>

              <div className="text-right">
                <div className="text-2xl font-bold font-mono tracking-tight text-zinc-100">
                  ₹{quote.lastTradedPrice.toFixed(2)}
                </div>
                <div
                  className={`flex items-center justify-end gap-1 text-xs font-mono font-medium ${
                    quote.change >= 0 ? 'text-emerald-400' : 'text-rose-400'
                  }`}
                >
                  {quote.change >= 0 ? <TrendingUp size={13} /> : <TrendingDown size={13} />}
                  <span>
                    {quote.change >= 0 ? '+' : ''}
                    {quote.change.toFixed(2)} ({quote.changePercent.toFixed(2)}%)
                  </span>
                </div>
              </div>
            </div>

            {/* TradingView Chart */}
            <div className="my-3">
              <TradingViewChart candles={candles} symbol={quote.symbol} />
            </div>

            {/* Market stats bar */}
            <div className="grid grid-cols-4 gap-2 text-center text-xs bg-surface-elevated/70 p-3 rounded-lg border border-border/80">
              <div>
                <div className="text-zinc-500">Day Open</div>
                <div className="font-mono text-zinc-200 mt-0.5">₹{quote.open.toFixed(2)}</div>
              </div>
              <div>
                <div className="text-zinc-500">Day High</div>
                <div className="font-mono text-emerald-400 mt-0.5">₹{quote.high.toFixed(2)}</div>
              </div>
              <div>
                <div className="text-zinc-500">Day Low</div>
                <div className="font-mono text-rose-400 mt-0.5">₹{quote.low.toFixed(2)}</div>
              </div>
              <div>
                <div className="text-zinc-500">Volume</div>
                <div className="font-mono text-zinc-200 mt-0.5">{quote.volume.toLocaleString()}</div>
              </div>
            </div>
          </div>
        </div>

        {/* Right Side: Order Ticket Form */}
        <div className="w-full md:w-80 p-6 flex flex-col justify-between bg-surface-elevated/40">
          <div>
            <div className="flex items-center justify-between mb-5">
              <h3 className="text-sm font-semibold text-zinc-300 uppercase tracking-wider">Order Ticket</h3>
              <button
                onClick={onClose}
                className="text-zinc-400 hover:text-zinc-100 transition-colors p-1"
              >
                <X size={18} />
              </button>
            </div>

            {/* Buy / Sell Toggle */}
            <div className="grid grid-cols-2 p-1 bg-surface border border-border rounded-lg mb-4">
              <button
                type="button"
                onClick={() => setSide('BUY')}
                className={`py-2 text-xs font-semibold rounded-md transition-all ${
                  side === 'BUY'
                    ? 'bg-emerald-600 text-white shadow-sm'
                    : 'text-zinc-400 hover:text-zinc-200'
                }`}
              >
                BUY
              </button>
              <button
                type="button"
                onClick={() => setSide('SELL')}
                className={`py-2 text-xs font-semibold rounded-md transition-all ${
                  side === 'SELL'
                    ? 'bg-rose-600 text-white shadow-sm'
                    : 'text-zinc-400 hover:text-zinc-200'
                }`}
              >
                SELL
              </button>
            </div>

            {/* Order Type Toggle */}
            <div className="grid grid-cols-2 p-1 bg-surface border border-border rounded-lg mb-4 text-xs font-medium">
              <button
                type="button"
                onClick={() => setOrderType('MARKET')}
                className={`py-1.5 rounded ${
                  orderType === 'MARKET' ? 'bg-zinc-800 text-zinc-100' : 'text-zinc-400'
                }`}
              >
                Market Order
              </button>
              <button
                type="button"
                onClick={() => setOrderType('LIMIT')}
                className={`py-1.5 rounded ${
                  orderType === 'LIMIT' ? 'bg-zinc-800 text-zinc-100' : 'text-zinc-400'
                }`}
              >
                Limit Order
              </button>
            </div>

            <form onSubmit={handleSubmit} className="space-y-4">
              {/* Limit Price Input */}
              {orderType === 'LIMIT' && (
                <div>
                  <label className="block text-xs font-medium text-zinc-400 mb-1">Limit Price (₹)</label>
                  <input
                    type="number"
                    step="0.05"
                    value={limitPrice}
                    onChange={(e) => setLimitPrice(e.target.value)}
                    className="w-full bg-surface border border-border rounded-lg px-3 py-2 text-sm font-mono text-zinc-100 focus:outline-none focus:border-zinc-500"
                    placeholder="Enter trigger price"
                    required
                  />
                </div>
              )}

              {/* Quantity Input */}
              <div>
                <div className="flex justify-between items-center mb-1">
                  <label className="text-xs font-medium text-zinc-400">Quantity (Shares)</label>
                  <span className="text-xs text-zinc-500 font-mono">
                    {side === 'BUY' ? `Cash: ₹${availableCash.toLocaleString()}` : `Owned: ${ownedShares}`}
                  </span>
                </div>
                <input
                  type="number"
                  min="1"
                  value={quantity || ''}
                  onChange={(e) => setQuantity(parseInt(e.target.value) || 0)}
                  className="w-full bg-surface border border-border rounded-lg px-3 py-2 text-sm font-mono text-zinc-100 focus:outline-none focus:border-zinc-500"
                  required
                />

                {/* Quick quantity presets */}
                <div className="grid grid-cols-5 gap-1.5 mt-2">
                  {[1, 5, 10, 25].map((preset) => (
                    <button
                      key={preset}
                      type="button"
                      onClick={() => handleQuickQuantity(preset)}
                      className="py-1 text-xs bg-surface border border-border rounded text-zinc-400 hover:text-zinc-200 hover:border-zinc-600 transition"
                    >
                      +{preset}
                    </button>
                  ))}
                  <button
                    type="button"
                    onClick={handleMaxQuantity}
                    className="py-1 text-xs bg-surface border border-border rounded text-zinc-300 font-medium hover:border-zinc-500 transition"
                  >
                    Max
                  </button>
                </div>
              </div>

              {/* Cost calculation overview */}
              <div className="p-3 bg-surface border border-border rounded-lg space-y-1.5 text-xs">
                <div className="flex justify-between text-zinc-400">
                  <span>Price per share</span>
                  <span className="font-mono text-zinc-200">₹{priceToUse.toFixed(2)}</span>
                </div>
                <div className="flex justify-between text-zinc-400">
                  <span>Brokerage / Fee</span>
                  <span className="font-mono text-emerald-400">₹0.00 (Virtual)</span>
                </div>
                <div className="border-t border-border/80 pt-1.5 flex justify-between font-semibold text-zinc-200">
                  <span>Estimated Total</span>
                  <span className="font-mono text-sm text-zinc-100">₹{estimatedTotal.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</span>
                </div>
              </div>

              {/* Feedback messages */}
              {error && (
                <div className="flex items-start gap-2 p-2.5 rounded-lg bg-rose-950/40 border border-rose-800/80 text-rose-300 text-xs">
                  <AlertCircle size={15} className="shrink-0 mt-0.5" />
                  <span>{error}</span>
                </div>
              )}

              {success && (
                <div className="flex items-start gap-2 p-2.5 rounded-lg bg-emerald-950/40 border border-emerald-800/80 text-emerald-300 text-xs">
                  <CheckCircle2 size={15} className="shrink-0 mt-0.5" />
                  <span>{success}</span>
                </div>
              )}

              {/* Submit CTA */}
              <button
                type="submit"
                disabled={isSubmitting}
                className={`w-full py-2.5 text-sm font-semibold rounded-lg transition-all ${
                  side === 'BUY'
                    ? 'bg-emerald-600 hover:bg-emerald-500 text-white'
                    : 'bg-rose-600 hover:bg-rose-500 text-white'
                } disabled:opacity-50 disabled:cursor-not-allowed`}
              >
                {isSubmitting ? 'Transacting...' : `${side} ${quantity} ${quote.symbol}`}
              </button>
            </form>
          </div>
        </div>
      </div>
    </div>
  );
};
