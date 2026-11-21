import React, { useState, useEffect } from 'react';
import { Trophy, Users, Calendar, Award, CheckCircle, RefreshCw } from 'lucide-react';
import { Competition, LeaderboardEntry } from '../types';
import { competitionApi } from '../services/api';

interface CompetitionsViewProps {
  onRefreshPortfolio: () => void;
}

export const CompetitionsView: React.FC<CompetitionsViewProps> = ({ onRefreshPortfolio }) => {
  const [competitions, setCompetitions] = useState<Competition[]>([]);
  const [selectedCompId, setSelectedCompId] = useState<string | null>(null);
  const [leaderboard, setLeaderboard] = useState<LeaderboardEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [joining, setJoining] = useState<string | null>(null);

  const loadCompetitions = async () => {
    try {
      const data = await competitionApi.list();
      setCompetitions(data);
      if (data.length > 0 && !selectedCompId) {
        setSelectedCompId(data[0].id);
      }
    } catch (err) {
      console.error('Failed to load competitions', err);
    } finally {
      setLoading(false);
    }
  };

  const loadLeaderboard = async (compId: string) => {
    try {
      const entries = await competitionApi.leaderboard(compId);
      setLeaderboard(entries);
    } catch (err) {
      console.error('Failed to load leaderboard', err);
    }
  };

  useEffect(() => {
    loadCompetitions();
  }, []);

  useEffect(() => {
    if (selectedCompId) {
      loadLeaderboard(selectedCompId);
    }
  }, [selectedCompId]);

  const handleJoin = async (id: string) => {
    setJoining(id);
    try {
      await competitionApi.join(id);
      await loadCompetitions();
      await loadLeaderboard(id);
      onRefreshPortfolio();
    } catch (err: any) {
      alert(err.message || 'Failed to join competition');
    } finally {
      setJoining(null);
    }
  };

  return (
    <div className="space-y-6">
      {/* Competitions Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {competitions.map((comp) => {
          const isSelected = selectedCompId === comp.id;
          return (
            <div
              key={comp.id}
              onClick={() => setSelectedCompId(comp.id)}
              className={`p-5 rounded-2xl border transition cursor-pointer flex flex-col justify-between ${
                isSelected
                  ? 'bg-surface-elevated border-emerald-500/60 shadow-lg'
                  : 'bg-surface border-border hover:border-zinc-700'
              }`}
            >
              <div>
                <div className="flex items-center justify-between">
                  <span className="text-xs px-2.5 py-0.5 rounded-full font-mono bg-emerald-950/60 border border-emerald-800 text-emerald-400 font-semibold">
                    {comp.status}
                  </span>
                  <div className="flex items-center gap-1 text-xs text-zinc-400 font-mono">
                    <Users size={13} />
                    <span>{comp.participantCount} Participants</span>
                  </div>
                </div>

                <h3 className="text-base font-bold text-zinc-100 mt-3">{comp.name}</h3>
                <p className="text-xs text-zinc-400 mt-1">
                  Starting Virtual Capital: <strong className="text-zinc-200 font-mono">₹{Number(comp.startingBalance).toLocaleString()}</strong>
                </p>
              </div>

              <div className="mt-5 pt-3 border-t border-border/80 flex items-center justify-between">
                <span className="text-[11px] text-zinc-500 font-mono flex items-center gap-1">
                  <Calendar size={12} /> Ends {new Date(comp.endTime).toLocaleDateString()}
                </span>

                {comp.hasJoined ? (
                  <span className="flex items-center gap-1 text-xs text-emerald-400 font-semibold">
                    <CheckCircle size={14} /> Registered
                  </span>
                ) : (
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      handleJoin(comp.id);
                    }}
                    disabled={joining === comp.id}
                    className="px-4 py-1.5 rounded-lg bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-semibold transition"
                  >
                    {joining === comp.id ? 'Joining...' : 'Enter Contest'}
                  </button>
                )}
              </div>
            </div>
          );
        })}
      </div>

      {/* Leaderboard Table */}
      <div className="rounded-2xl bg-surface border border-border p-5 space-y-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Trophy size={18} className="text-amber-400" />
            <h3 className="text-sm font-semibold text-zinc-200">
              Championship Leaderboard Standings
            </h3>
          </div>
          <button
            onClick={() => selectedCompId && loadLeaderboard(selectedCompId)}
            className="p-1.5 rounded-lg bg-surface-elevated hover:bg-zinc-800 border border-border text-zinc-400 hover:text-zinc-200"
            title="Refresh Leaderboard"
          >
            <RefreshCw size={14} />
          </button>
        </div>

        {leaderboard.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs font-mono">
              <thead>
                <tr className="border-b border-border/80 text-zinc-500 bg-surface-elevated/40 font-sans">
                  <th className="py-3 px-4 font-medium w-16 text-center">Rank</th>
                  <th className="py-3 px-4 font-medium">Participant</th>
                  <th className="py-3 px-4 font-medium text-right">Account Valuation</th>
                  <th className="py-3 px-4 font-medium text-right">Portfolio Return</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border/40">
                {leaderboard.map((entry) => {
                  const isTop3 = entry.rank <= 3;
                  return (
                    <tr key={entry.userId} className="hover:bg-surface-elevated/50 transition">
                      <td className="py-3 px-4 text-center">
                        <span
                          className={`inline-flex items-center justify-center w-6 h-6 rounded-full font-bold text-xs ${
                            entry.rank === 1
                              ? 'bg-amber-500 text-black'
                              : entry.rank === 2
                              ? 'bg-zinc-300 text-black'
                              : entry.rank === 3
                              ? 'bg-amber-800 text-white'
                              : 'bg-zinc-800 text-zinc-400'
                          }`}
                        >
                          {entry.rank}
                        </span>
                      </td>
                      <td className="py-3 px-4 font-sans font-semibold text-zinc-200 flex items-center gap-2">
                        {entry.username}
                        {isTop3 && <Award size={14} className="text-amber-400" />}
                      </td>
                      <td className="py-3 px-4 text-right font-bold text-zinc-100">
                        ₹{Number(entry.totalAccountValue).toLocaleString(undefined, { minimumFractionDigits: 2 })}
                      </td>
                      <td className={`py-3 px-4 text-right font-bold ${
                        entry.returnPercentage >= 0 ? 'text-emerald-400' : 'text-rose-400'
                      }`}>
                        {entry.returnPercentage >= 0 ? '+' : ''}{entry.returnPercentage.toFixed(2)}%
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        ) : (
          <div className="py-10 text-center text-zinc-500 text-xs">
            No participants ranked yet. Click "Enter Contest" above to join the championship!
          </div>
        )}
      </div>
    </div>
  );
};
