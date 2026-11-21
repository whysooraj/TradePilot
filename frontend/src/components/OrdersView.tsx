import React, { useState } from 'react';
import { ListOrdered, CheckCircle2, Clock, XCircle, Ban, RefreshCw } from 'lucide-react';
import { Order, Trade } from '../types';
import { orderApi } from '../services/api';

interface OrdersViewProps {
  orders: Order[];
  trades: Trade[];
  onRefresh: () => void;
}

export const OrdersView: React.FC<OrdersViewProps> = ({
  orders,
  trades,
  onRefresh,
}) => {
  const [activeSubTab, setActiveSubTab] = useState<'ORDERS' | 'TRADES'>('ORDERS');
  const [cancellingId, setCancellingId] = useState<string | null>(null);

  const handleCancel = async (orderId: string) => {
    setCancellingId(orderId);
    try {
      await orderApi.cancel(orderId);
      onRefresh();
    } catch (err: any) {
      alert(err.message || 'Failed to cancel order');
    } finally {
      setCancellingId(null);
    }
  };

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'FILLED':
        return (
          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded text-[11px] font-semibold bg-emerald-950/60 border border-emerald-800 text-emerald-400">
            <CheckCircle2 size={11} /> FILLED
          </span>
        );
      case 'OPEN':
        return (
          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded text-[11px] font-semibold bg-amber-950/60 border border-amber-800 text-amber-400">
            <Clock size={11} /> OPEN
          </span>
        );
      case 'CANCELLED':
        return (
          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded text-[11px] font-semibold bg-zinc-800 border border-zinc-700 text-zinc-400">
            <Ban size={11} /> CANCELLED
          </span>
        );
      default:
        return (
          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded text-[11px] font-semibold bg-rose-950/60 border border-rose-800 text-rose-400">
            <XCircle size={11} /> {status}
          </span>
        );
    }
  };

  return (
    <div className="space-y-5">
      {/* Tab Switcher & Refresh */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2 p-1 bg-surface border border-border rounded-xl text-xs font-semibold">
          <button
            onClick={() => setActiveSubTab('ORDERS')}
            className={`px-4 py-2 rounded-lg transition ${
              activeSubTab === 'ORDERS'
                ? 'bg-zinc-800 text-zinc-100 shadow-sm border border-zinc-700/60'
                : 'text-zinc-400 hover:text-zinc-200'
            }`}
          >
            Orders Log ({orders.length})
          </button>
          <button
            onClick={() => setActiveSubTab('TRADES')}
            className={`px-4 py-2 rounded-lg transition ${
              activeSubTab === 'TRADES'
                ? 'bg-zinc-800 text-zinc-100 shadow-sm border border-zinc-700/60'
                : 'text-zinc-400 hover:text-zinc-200'
            }`}
          >
            Executed Trades ({trades.length})
          </button>
        </div>

        <button
          onClick={onRefresh}
          className="p-2 rounded-xl bg-surface hover:bg-surface-elevated border border-border text-zinc-400 hover:text-zinc-100 transition"
          title="Refresh"
        >
          <RefreshCw size={15} />
        </button>
      </div>

      {/* Orders Table */}
      {activeSubTab === 'ORDERS' ? (
        <div className="rounded-2xl bg-surface border border-border overflow-hidden">
          {orders.length > 0 ? (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs font-mono">
                <thead>
                  <tr className="border-b border-border/80 text-zinc-500 bg-surface-elevated/40 font-sans">
                    <th className="py-3 px-4 font-medium">Time</th>
                    <th className="py-3 px-4 font-medium">Instrument</th>
                    <th className="py-3 px-4 font-medium">Side</th>
                    <th className="py-3 px-4 font-medium">Type</th>
                    <th className="py-3 px-4 font-medium text-right">Quantity</th>
                    <th className="py-3 px-4 font-medium text-right">Limit Price</th>
                    <th className="py-3 px-4 font-medium text-right">Executed Price</th>
                    <th className="py-3 px-4 font-medium text-center">Status</th>
                    <th className="py-3 px-4 font-medium text-center">Action</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-border/40">
                  {orders.map((order) => (
                    <tr key={order.orderId} className="hover:bg-surface-elevated/50 transition">
                      <td className="py-3 px-4 text-zinc-400 text-[11px]">
                        {new Date(order.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' })}
                      </td>
                      <td className="py-3 px-4 font-sans font-semibold text-zinc-200">
                        {order.symbol}
                      </td>
                      <td className="py-3 px-4">
                        <span className={`px-2 py-0.5 rounded font-sans font-bold text-[11px] ${
                          order.side === 'BUY' ? 'bg-emerald-950/60 text-emerald-400' : 'bg-rose-950/60 text-rose-400'
                        }`}>
                          {order.side}
                        </span>
                      </td>
                      <td className="py-3 px-4 text-zinc-300 font-sans">{order.orderType}</td>
                      <td className="py-3 px-4 text-right font-semibold text-zinc-200">{order.quantity}</td>
                      <td className="py-3 px-4 text-right text-zinc-400">
                        {order.limitPrice ? `₹${order.limitPrice.toFixed(2)}` : '—'}
                      </td>
                      <td className="py-3 px-4 text-right font-bold text-zinc-100">
                        {order.executedPrice ? `₹${order.executedPrice.toFixed(2)}` : '—'}
                      </td>
                      <td className="py-3 px-4 text-center">
                        {getStatusBadge(order.status)}
                      </td>
                      <td className="py-3 px-4 text-center">
                        {order.status === 'OPEN' ? (
                          <button
                            onClick={() => handleCancel(order.orderId)}
                            disabled={cancellingId === order.orderId}
                            className="px-2.5 py-1 rounded bg-zinc-800 hover:bg-rose-600 hover:text-white border border-zinc-700 text-zinc-300 font-sans text-xs transition"
                          >
                            {cancellingId === order.orderId ? '...' : 'Cancel'}
                          </button>
                        ) : (
                          <span className="text-zinc-600">—</span>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <div className="py-12 text-center text-zinc-500 text-xs">
              No orders placed yet.
            </div>
          )}
        </div>
      ) : (
        /* Executed Trades Table */
        <div className="rounded-2xl bg-surface border border-border overflow-hidden">
          {trades.length > 0 ? (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs font-mono">
                <thead>
                  <tr className="border-b border-border/80 text-zinc-500 bg-surface-elevated/40 font-sans">
                    <th className="py-3 px-4 font-medium">Executed At</th>
                    <th className="py-3 px-4 font-medium">Instrument</th>
                    <th className="py-3 px-4 font-medium">Side</th>
                    <th className="py-3 px-4 font-medium text-right">Shares Filled</th>
                    <th className="py-3 px-4 font-medium text-right">Execution Price</th>
                    <th className="py-3 px-4 font-medium text-right">Gross Amount</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-border/40">
                  {trades.map((trade) => (
                    <tr key={trade.tradeId} className="hover:bg-surface-elevated/50 transition">
                      <td className="py-3 px-4 text-zinc-400 text-[11px]">
                        {new Date(trade.executedAt).toLocaleString()}
                      </td>
                      <td className="py-3 px-4 font-sans font-semibold text-zinc-200">
                        {trade.symbol}
                      </td>
                      <td className="py-3 px-4">
                        <span className={`px-2 py-0.5 rounded font-sans font-bold text-[11px] ${
                          trade.side === 'BUY' ? 'bg-emerald-950/60 text-emerald-400' : 'bg-rose-950/60 text-rose-400'
                        }`}>
                          {trade.side}
                        </span>
                      </td>
                      <td className="py-3 px-4 text-right font-semibold text-zinc-200">{trade.executedQuantity}</td>
                      <td className="py-3 px-4 text-right font-bold text-zinc-100">₹{trade.executedPrice.toFixed(2)}</td>
                      <td className="py-3 px-4 text-right font-bold text-emerald-400">₹{trade.totalAmount.toLocaleString(undefined, { minimumFractionDigits: 2 })}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <div className="py-12 text-center text-zinc-500 text-xs">
              No executed trades recorded yet.
            </div>
          )}
        </div>
      )}
    </div>
  );
};
