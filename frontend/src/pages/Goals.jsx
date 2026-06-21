import React, { useState, useEffect, useCallback } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { useAuth } from '../context/AuthContext';
import { goalService } from '../api/goalService';
import './Goals.css';

// ─── Goal Card ───
function GoalCard({ goal, onDelete }) {
  const [expanded, setExpanded] = useState(false);
  const target = Number(goal.targetAmount || 0);
  const current = Number(goal.currentAmount || 0);
  const pct = target > 0 ? Math.min((current / target) * 100, 100) : 0;
  const remaining = Math.max(target - current, 0);

  const statusColor = pct >= 100 ? 'var(--success)' :
    pct >= 75 ? 'var(--accent)' :
    pct >= 50 ? '#60a5fa' : 'var(--danger)';

  const statusLabel = pct >= 100 ? 'COMPLETE' :
    pct >= 75 ? 'NEARLY' :
    pct >= 50 ? 'IN PROGRESS' : 'STARTING';

  return (
    <motion.div
      layout
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      className="goal-card"
    >
      <div className="goal-card-header">
        <div className="goal-info">
          <h4 className="goal-name">{goal.goalName || goal.category || 'Goal'}</h4>
          <span className="goal-period">{goal.period || 'MONTHLY'}</span>
        </div>
        <span className="goal-status-badge" style={{ color: statusColor, borderColor: statusColor }}>
          {statusLabel}
        </span>
      </div>

      <div className="goal-progress-section">
        <div className="goal-amounts">
          <span className="goal-current">₹{current.toLocaleString('en-IN')}</span>
          <span className="goal-target">of ₹{target.toLocaleString('en-IN')}</span>
        </div>
        <div className="goal-progress-bar">
          <motion.div
            className="goal-progress-fill"
            initial={{ width: 0 }}
            animate={{ width: `${pct}%` }}
            transition={{ duration: 0.6, ease: 'linear' }}
            style={{ background: statusColor }}
          />
        </div>
        <div className="goal-meta">
          <span className="goal-pct">{pct.toFixed(0)}%</span>
          <span className="goal-remaining">
            {pct >= 100 ? '✓ ACHIEVED' : `₹${remaining.toLocaleString('en-IN')} left`}
          </span>
        </div>
      </div>

      <div className="goal-card-actions">
        <button
          className="goal-action-btn"
          onClick={() => setExpanded(!expanded)}
        >
          {expanded ? 'LESS' : 'MORE'}
        </button>
        <button
          className="goal-action-btn danger"
          onClick={() => onDelete(goal.id)}
        >
          DELETE
        </button>
      </div>

      <AnimatePresence>
        {expanded && (
          <motion.div
            initial={{ height: 0, opacity: 0 }}
            animate={{ height: 'auto', opacity: 1 }}
            exit={{ height: 0, opacity: 0 }}
            transition={{ duration: 0.2 }}
            className="goal-details"
          >
            <div className="goal-detail-row">
              <span>CATEGORY</span>
              <span>{goal.category || '—'}</span>
            </div>
            <div className="goal-detail-row">
              <span>START</span>
              <span>{goal.startDate || '—'}</span>
            </div>
            <div className="goal-detail-row">
              <span>TARGET_DATE</span>
              <span>{goal.targetDate || '—'}</span>
            </div>
            <div className="goal-detail-row">
              <span>STATUS</span>
              <span style={{ color: statusColor }}>{goal.status || 'ACTIVE'}</span>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </motion.div>
  );
}

// ─── Create Goal Modal ───
function CreateGoalModal({ onClose, onSubmit }) {
  const [form, setForm] = useState({
    category: '',
    targetAmount: '',
    period: 'MONTHLY',
    startDate: new Date().toISOString().split('T')[0],
    endDate: '',
  });

  const handleChange = (e) => {
    const { name, value } = e.target;
    setForm(prev => ({ ...prev, [name]: value }));
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    onSubmit({
      ...form,
      targetAmount: Number(form.targetAmount),
      userId: '',
    });
  };

  const categories = [
    { value: 'FOOD', label: 'FOOD & DINING' },
    { value: 'SHOPPING', label: 'SHOPPING' },
    { value: 'WEB_SHOPPING', label: 'ONLINE SHOPPING' },
    { value: 'SUBSCRIPTION', label: 'SUBSCRIPTIONS' },
    { value: 'TRANSPORT', label: 'TRANSPORT' },
    { value: 'HEALTH', label: 'HEALTH' },
    { value: 'UTILITIES', label: 'UTILITIES' },
    { value: 'RENT', label: 'RENT' },
    { value: 'EDUCATION', label: 'EDUCATION' },
    { value: 'ENTERTAINMENT', label: 'ENTERTAINMENT' },
    { value: 'INVESTMENT', label: 'INVESTMENT' },
    { value: 'TRANSFER', label: 'TRANSFER' },
    { value: 'OTHER', label: 'OTHER' },
  ];

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      className="modal-overlay"
      onClick={onClose}
    >
      <motion.div
        initial={{ opacity: 0, y: 12 }}
        animate={{ opacity: 1, y: 0 }}
        exit={{ opacity: 0, y: 12 }}
        transition={{ duration: 0.2 }}
        className="modal-card"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="modal-header">
          <h3>▣ NEW GOAL</h3>
          <button className="modal-close" onClick={onClose}>✕</button>
        </div>

        <form onSubmit={handleSubmit} className="modal-form">
          <div className="input-group">
            <label className="input-label">CATEGORY</label>
            <select
              name="category"
              value={form.category}
              onChange={handleChange}
              required
              className="auth-input"
            >
              <option value="">SELECT</option>
              {categories.map(c => (
                <option key={c.value} value={c.value}>{c.label}</option>
              ))}
            </select>
          </div>

          <div className="input-group">
            <label className="input-label">TARGET_AMOUNT (₹)</label>
            <input
              type="number"
              name="targetAmount"
              value={form.targetAmount}
              onChange={handleChange}
              placeholder="10000"
              required
              min="1"
              className="auth-input"
            />
          </div>

          <div className="input-group">
            <label className="input-label">PERIOD</label>
            <select
              name="period"
              value={form.period}
              onChange={handleChange}
              className="auth-input"
            >
              <option value="WEEKLY">WEEKLY</option>
              <option value="MONTHLY">MONTHLY</option>
              <option value="QUARTERLY">QUARTERLY</option>
              <option value="YEARLY">YEARLY</option>
            </select>
          </div>

          <div className="form-row">
            <div className="input-group">
              <label className="input-label">START</label>
              <input
                type="date"
                name="startDate"
                value={form.startDate}
                onChange={handleChange}
                required
                className="auth-input"
              />
            </div>
            <div className="input-group">
              <label className="input-label">END</label>
              <input
                type="date"
                name="endDate"
                value={form.endDate}
                onChange={handleChange}
                className="auth-input"
              />
            </div>
          </div>

          <div className="modal-actions">
            <button type="button" className="btn btn-secondary" onClick={onClose}>
              CANCEL
            </button>
            <button type="submit" className="btn btn-primary">
              CREATE
            </button>
          </div>
        </form>
      </motion.div>
    </motion.div>
  );
}

// ─── Main Goals Component ───
export default function Goals() {
  const { user } = useAuth();
  const [goals, setGoals] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showCreate, setShowCreate] = useState(false);
  const [filter, setFilter] = useState('all');
  const [error, setError] = useState('');

  const userId = user?.userId || 'dev-user';

  const loadGoals = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const data = await goalService.getAll(userId);
      // Ensure we always have an array and normalize status
      const goalsArray = Array.isArray(data) ? data : [];
      // Normalize: ensure each goal has a status field
      const normalized = goalsArray.map(g => ({
        ...g,
        status: g.status || 'ACTIVE',
        currentAmount: Number(g.currentAmount || 0),
        targetAmount: Number(g.targetAmount || 0),
      }));
      setGoals(normalized);
    } catch (err) {
      console.error('Failed to load goals:', err);
      setError('Failed to load goals. Please try again.');
    } finally {
      setLoading(false);
    }
  }, [userId]);

  // Load on mount and when user changes
  useEffect(() => {
    loadGoals();
  }, [loadGoals]);

  // Also reload when tab becomes visible
  useEffect(() => {
    const handleVisibility = () => {
      if (document.visibilityState === 'visible') {
        loadGoals();
      }
    };
    document.addEventListener('visibilitychange', handleVisibility);
    return () => document.removeEventListener('visibilitychange', handleVisibility);
  }, [loadGoals]);

  const handleCreate = async (goalData) => {
    setError('');
    try {
      await goalService.create({ ...goalData, userId });
      setShowCreate(false);
      // Reload goals after creation
      await loadGoals();
    } catch (err) {
      setError(err.message || 'Failed to create goal.');
    }
  };

  const handleDelete = async (goalId) => {
    setError('');
    try {
      await goalService.delete(goalId);
      setGoals(prev => prev.filter(g => g.id !== goalId));
    } catch (err) {
      setError(err.message || 'Failed to delete goal.');
    }
  };

  // Filter logic: use status field from backend as primary, progress as fallback
  const filteredGoals = goals.filter(g => {
    const isComplete = g.status === 'COMPLETED' || g.status === 'COMPLETE' ||
      Number(g.currentAmount) >= Number(g.targetAmount);
    const isActive = g.status === 'ACTIVE' || g.status === 'IN_PROGRESS' ||
      Number(g.currentAmount) < Number(g.targetAmount);

    if (filter === 'active') return isActive;
    if (filter === 'completed') return isComplete;
    return true;
  });

  const totalTarget = goals.reduce((s, g) => s + Number(g.targetAmount || 0), 0);
  const totalSaved = goals.reduce((s, g) => s + Number(g.currentAmount || 0), 0);
  const completedCount = goals.filter(g =>
    g.status === 'COMPLETED' || g.status === 'COMPLETE' ||
    Number(g.currentAmount) >= Number(g.targetAmount)
  ).length;

  return (
    <div className="page-container goals-page">
      {/* Header */}
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        className="page-header"
      >
        <div>
          <h1 className="page-title">▣ GOALS</h1>
          <p className="page-subtitle">Spending limits and savings targets</p>
        </div>
        <button
          className="btn btn-primary"
          onClick={() => setShowCreate(true)}
        >
          + NEW GOAL
        </button>
      </motion.div>

      {/* Error */}
      {error && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          className="auth-error"
          style={{ marginBottom: 'var(--sp-6)' }}
        >
          <span>!</span> {error}
        </motion.div>
      )}

      {/* Stats */}
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ delay: 0.05 }}
        className="goals-stats"
      >
        <div className="goal-stat-card">
          <span className="goal-stat-value">{goals.length}</span>
          <span className="goal-stat-label">TOTAL</span>
        </div>
        <div className="goal-stat-card">
          <span className="goal-stat-value">{completedCount}</span>
          <span className="goal-stat-label">COMPLETE</span>
        </div>
        <div className="goal-stat-card">
          <span className="goal-stat-value">₹{totalSaved.toLocaleString('en-IN')}</span>
          <span className="goal-stat-label">SAVED</span>
        </div>
        <div className="goal-stat-card">
          <span className="goal-stat-value">₹{totalTarget.toLocaleString('en-IN')}</span>
          <span className="goal-stat-label">TARGET</span>
        </div>
      </motion.div>

      {/* Filter */}
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ delay: 0.1 }}
        className="goals-filter"
      >
        {['all', 'active', 'completed'].map(f => (
          <button
            key={f}
            className={`filter-btn ${filter === f ? 'active' : ''}`}
            onClick={() => setFilter(f)}
          >
            {f.toUpperCase()}
          </button>
        ))}
      </motion.div>

      {/* Goals List */}
      <div className="goals-list">
        {loading ? (
          <div className="goals-loading">
            {[1, 2, 3].map(i => (
              <div key={i} className="skeleton goal-skeleton" />
            ))}
          </div>
        ) : filteredGoals.length === 0 ? (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            className="goals-empty"
          >
            <span className="empty-icon">▣</span>
            <h3>{filter === 'all' ? 'NO GOALS' : `NO ${filter.toUpperCase()} GOALS`}</h3>
            <p>{filter === 'all'
              ? 'Create a spending goal to start tracking.'
              : `No ${filter} goals found.`}</p>
            <button className="btn btn-primary" onClick={() => setShowCreate(true)}>
              + NEW GOAL
            </button>
          </motion.div>
        ) : (
          <AnimatePresence>
            {filteredGoals.map((goal, i) => (
              <GoalCard
                key={goal.id || i}
                goal={goal}
                onDelete={handleDelete}
              />
            ))}
          </AnimatePresence>
        )}
      </div>

      {/* Create Modal */}
      <AnimatePresence>
        {showCreate && (
          <CreateGoalModal
            onClose={() => setShowCreate(false)}
            onSubmit={handleCreate}
          />
        )}
      </AnimatePresence>
    </div>
  );
}
