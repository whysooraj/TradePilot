import React, { useState } from 'react';
import { X, Lock, Mail, User, ShieldCheck, Zap } from 'lucide-react';
import { authApi, setAuthToken, setStoredUser } from '../services/api';
import { AuthState } from '../types';

interface AuthModalProps {
  isOpen: boolean;
  onClose: () => void;
  onAuthSuccess: (authState: AuthState) => void;
}

export const AuthModal: React.FC<AuthModalProps> = ({
  isOpen,
  onClose,
  onAuthSuccess,
}) => {
  const [mode, setMode] = useState<'LOGIN' | 'REGISTER'>('LOGIN');
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);

    try {
      let res;
      if (mode === 'REGISTER') {
        res = await authApi.register({ username, email, password });
      } else {
        res = await authApi.login({ usernameOrEmail: username, password });
      }

      setAuthToken(res.token);
      setStoredUser(res);
      onAuthSuccess({
        token: res.token,
        user: {
          userId: res.userId,
          username: res.username,
          email: res.email,
          role: res.role,
          availableCash: res.availableCash,
        },
      });
      onClose();
    } catch (err: any) {
      setError(err.message || 'Authentication failed');
    } finally {
      setLoading(false);
    }
  };

  const handleQuickDemo = async () => {
    setError(null);
    setLoading(true);
    const demoUser = `demotrader_${Math.floor(Math.random() * 8999 + 1000)}`;
    const demoEmail = `${demoUser}@tradelab.internal`;
    const demoPass = 'TradeLabPass123!';

    try {
      const res = await authApi.register({
        username: demoUser,
        email: demoEmail,
        password: demoPass,
      });

      setAuthToken(res.token);
      setStoredUser(res);
      onAuthSuccess({
        token: res.token,
        user: {
          userId: res.userId,
          username: res.username,
          email: res.email,
          role: res.role,
          availableCash: res.availableCash,
        },
      });
      onClose();
    } catch (err: any) {
      setError(err.message || 'Demo provisioning failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-sm p-4">
      <div className="relative w-full max-w-md bg-surface border border-border rounded-2xl shadow-2xl p-6">
        <button
          onClick={onClose}
          className="absolute right-4 top-4 text-zinc-400 hover:text-zinc-100 transition"
        >
          <X size={18} />
        </button>

        {/* Header and Toggle */}
        <div className="text-center mb-6">
          <div className="inline-flex items-center justify-center w-10 h-10 rounded-xl bg-zinc-800 border border-border text-emerald-400 font-mono font-bold text-base mb-2">
            TL
          </div>
          <h2 className="text-xl font-bold tracking-tight text-zinc-100">TradeLab Platform</h2>
          <p className="text-xs text-zinc-400 mt-1">Real-market-data virtual stock simulator</p>

          <div className="grid grid-cols-2 p-1 bg-surface-elevated border border-border rounded-xl mt-4 text-xs font-semibold">
            <button
              onClick={() => {
                setMode('LOGIN');
                setError(null);
              }}
              className={`py-1.5 rounded-lg transition ${
                mode === 'LOGIN' ? 'bg-zinc-800 text-zinc-100 shadow-sm border border-zinc-700/60' : 'text-zinc-400 hover:text-zinc-200'
              }`}
            >
              Sign In
            </button>
            <button
              onClick={() => {
                setMode('REGISTER');
                setError(null);
              }}
              className={`py-1.5 rounded-lg transition ${
                mode === 'REGISTER' ? 'bg-zinc-800 text-zinc-100 shadow-sm border border-zinc-700/60' : 'text-zinc-400 hover:text-zinc-200'
              }`}
            >
              Create Account
            </button>
          </div>
        </div>

        {/* 1-Click Demo Account Action */}
        <div className="mb-4">
          <button
            type="button"
            onClick={handleQuickDemo}
            disabled={loading}
            className="w-full flex items-center justify-center gap-2 py-2 px-3 rounded-xl bg-emerald-950/60 hover:bg-emerald-900/60 border border-emerald-800 text-emerald-300 text-xs font-semibold transition"
          >
            <Zap size={14} className="text-emerald-400" />
            <span>Launch Instant Demo Account (₹1,00,000 Cash)</span>
          </button>
        </div>

        <div className="relative flex py-2 items-center">
          <div className="flex-grow border-t border-border"></div>
          <span className="flex-shrink mx-3 text-[11px] text-zinc-500 uppercase tracking-widest font-mono">or continue with</span>
          <div className="flex-grow border-t border-border"></div>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className="space-y-3 mt-2">
          <div>
            <label className="block text-xs font-medium text-zinc-400 mb-1">
              {mode === 'LOGIN' ? 'Username or Email' : 'Username'}
            </label>
            <div className="relative">
              <User size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-zinc-500" />
              <input
                type="text"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                required
                className="w-full bg-surface-elevated border border-border rounded-xl pl-9 pr-3 py-2 text-xs font-mono text-zinc-100 focus:outline-none focus:border-zinc-500"
                placeholder={mode === 'LOGIN' ? 'trader or trader@example.com' : 'e.g. rohit_trade'}
              />
            </div>
          </div>

          {mode === 'REGISTER' && (
            <div>
              <label className="block text-xs font-medium text-zinc-400 mb-1">Email Address</label>
              <div className="relative">
                <Mail size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-zinc-500" />
                <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  required
                  className="w-full bg-surface-elevated border border-border rounded-xl pl-9 pr-3 py-2 text-xs font-mono text-zinc-100 focus:outline-none focus:border-zinc-500"
                  placeholder="name@domain.com"
                />
              </div>
            </div>
          )}

          <div>
            <label className="block text-xs font-medium text-zinc-400 mb-1">Password</label>
            <div className="relative">
              <Lock size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-zinc-500" />
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                className="w-full bg-surface-elevated border border-border rounded-xl pl-9 pr-3 py-2 text-xs font-mono text-zinc-100 focus:outline-none focus:border-zinc-500"
                placeholder="••••••••"
              />
            </div>
          </div>

          {mode === 'REGISTER' && (
            <div className="flex items-center gap-2 p-2.5 rounded-lg bg-zinc-900 border border-zinc-800 text-[11px] text-zinc-400">
              <ShieldCheck size={16} className="text-emerald-400 shrink-0" />
              <span>Includes automatic ₹1,00,000 virtual INR paper-trading deposit.</span>
            </div>
          )}

          {error && (
            <div className="p-2.5 rounded-lg bg-rose-950/60 border border-rose-800 text-rose-300 text-xs">
              {error}
            </div>
          )}

          <button
            type="submit"
            disabled={loading}
            className="w-full py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-semibold transition mt-2 disabled:opacity-50"
          >
            {loading ? 'Authenticating...' : mode === 'LOGIN' ? 'Sign In to TradeLab' : 'Create Account & Claim ₹1 Lakh'}
          </button>
        </form>
      </div>
    </div>
  );
};
