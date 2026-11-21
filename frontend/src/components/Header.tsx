import React from 'react';
import { TrendingUp, Wallet, Trophy, LineChart, Bookmark, ListOrdered, LogOut, User as UserIcon } from 'lucide-react';
import { AuthState } from '../types';

interface HeaderProps {
  activeTab: string;
  setActiveTab: (tab: string) => void;
  authState: AuthState;
  availableCash: number;
  onOpenAuth: () => void;
  onLogout: () => void;
}

export const Header: React.FC<HeaderProps> = ({
  activeTab,
  setActiveTab,
  authState,
  availableCash,
  onOpenAuth,
  onLogout,
}) => {
  const tabs = [
    { id: 'dashboard', label: 'Dashboard', icon: LineChart },
    { id: 'explore', label: 'Market', icon: TrendingUp },
    { id: 'portfolio', label: 'Portfolio', icon: Wallet },
    { id: 'orders', label: 'Orders & Trades', icon: ListOrdered },
    { id: 'competitions', label: 'Competitions', icon: Trophy },
    { id: 'watchlist', label: 'Watchlist', icon: Bookmark },
  ];

  return (
    <header className="sticky top-0 z-40 w-full border-b border-border bg-background/95 backdrop-blur-md">
      <div className="max-w-7xl mx-auto px-4 h-16 flex items-center justify-between gap-4">
        {/* Logo and Market status */}
        <div className="flex items-center gap-3">
          <div className="flex items-center gap-2.5 cursor-pointer" onClick={() => setActiveTab('dashboard')}>
            <img src="/favicon.svg" alt="TradeLab" className="w-8 h-8 rounded-lg" />
            <span className="font-bold text-lg tracking-tight text-zinc-100">Trade<span className="text-emerald-400">Lab</span></span>
          </div>
          <div className="hidden sm:flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-zinc-900 border border-zinc-800 text-[11px] font-mono text-zinc-400">
            <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse"></span>
            NSE: OPEN • SIMULATED
          </div>
        </div>

        {/* Navigation tabs */}
        <nav className="hidden md:flex items-center gap-1 bg-surface-elevated/50 p-1 rounded-xl border border-border">
          {tabs.map((tab) => {
            const Icon = tab.icon;
            const isActive = activeTab === tab.id;
            return (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id)}
                className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium transition-all ${
                  isActive
                    ? 'bg-zinc-800 text-zinc-100 shadow-sm border border-zinc-700/60'
                    : 'text-zinc-400 hover:text-zinc-200 hover:bg-zinc-800/40'
                }`}
              >
                <Icon size={14} className={isActive ? 'text-emerald-400' : ''} />
                <span>{tab.label}</span>
              </button>
            );
          })}
        </nav>

        {/* Right action bar: Cash badge and Auth */}
        <div className="flex items-center gap-3">
          {authState.user ? (
            <div className="flex items-center gap-3">
              <div className="hidden sm:flex flex-col items-end">
                <span className="text-[10px] text-zinc-400 uppercase tracking-wider font-semibold">Available Cash</span>
                <span className="text-sm font-bold font-mono text-emerald-400">
                  ₹{availableCash.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                </span>
              </div>

              <div className="flex items-center gap-2 pl-2 border-l border-border">
                <div className="flex items-center gap-1.5 bg-surface border border-border px-3 py-1.5 rounded-lg text-xs text-zinc-200">
                  <UserIcon size={13} className="text-zinc-400" />
                  <span className="font-medium">{authState.user.username}</span>
                </div>
                <button
                  onClick={onLogout}
                  title="Logout"
                  className="p-1.5 text-zinc-400 hover:text-zinc-100 rounded-lg hover:bg-surface border border-transparent hover:border-border transition"
                >
                  <LogOut size={16} />
                </button>
              </div>
            </div>
          ) : (
            <button
              onClick={onOpenAuth}
              className="px-4 py-1.5 rounded-lg bg-emerald-600 hover:bg-emerald-500 text-xs font-semibold text-white transition shadow-sm"
            >
              Sign In / Register
            </button>
          )}
        </div>
      </div>

      {/* Mobile nav */}
      <div className="md:hidden flex items-center justify-around border-t border-border/80 bg-surface px-2 py-2 overflow-x-auto">
        {tabs.map((tab) => {
          const Icon = tab.icon;
          const isActive = activeTab === tab.id;
          return (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              className={`flex flex-col items-center gap-1 px-2.5 py-1 text-[11px] rounded transition ${
                isActive ? 'text-emerald-400 font-semibold' : 'text-zinc-400'
              }`}
            >
              <Icon size={16} />
              <span>{tab.label}</span>
            </button>
          );
        })}
      </div>
    </header>
  );
};
