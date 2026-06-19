import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { useAuth } from '../context/AuthContext';
import { goalService } from '../api/goalService';
import './Goals.css';

// ─── Goal Card ───
function GoalCard({ goal, onUpdate, onDelete }) {
  const [expanded, setExpanded] = useState(false);
  const target = Number(goal.targetAmount || 0);
  const current = Number(goal.currentAmount || 0);
  const pct = target > 0 ? Math.min((current / target) * 100, 100) : 0;
  const remaining = Math.max(target - current, 0);

  const statusColor = pct >= 100 ? 'var(--accent-emerald)' :
    pct >= 75 ? 'var(--accent-cyan)' :
    pct >= 50 ? 'var(--accent-amber)' : 'var(--accent-rose)';

  const statusLabel = pct >= 100 ? 'Completed' :
    pct >= 75 ? 'Almost there' :
    pct >= 50 ? 'In progress' : 'Getting started';

  return (
    <motion.div
      layout
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0, scale: 0.95 }}
      className="goal-card"
      style={{ '--goal-accent': statusColor }}
    >
      <div className="goal-card-header">
        <div className="goal-info">
          <h4 className="goal-name">{goal.goalName || goal.category || 'Goal'}</h4>
          <span className="goal-period">{goal.period || 'MONLY'}</span>
        </div>
        <span className="goal-status-badge" style={{ background: `${statusColor}20`, color: statusColor }}>
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
            transition={{ duration: 0.8, ease: 'easeOut' }}
            style={{ background: statusColor }}
          />
        </div>
        <div className="goal-meta">
          <span className="goal-pct">{pct.toFixed(0)}% complete</span>
          <span className="goal-remaining">
            {pct >= 100 ? '✓ Achieved!' : `₹${remaining.toLocaleString('en-IN')} remaining`}
          </span>
        </div>
      </div>

      <div className="goal-card-actions">
        <button
          className="goal-action-btn"
          onClick={() => setExpanded(!expanded)}
        >
          {expanded ? 'Less' : 'More'}
        </button>
        <button
          className="goal-action-btn danger"
          onClick={() => onDelete(goal.id)}
        >
          Delete
        </button>
      </div>

      <AnimatePresence>
        {expanded && (
          <motion.div
            initial={{ height: 0, opacity: 0 }}
            animate={{ height: 'auto', opacity: 1 }}
            exit={{ height: 0, opacity: 0 }}
            transition={{ duration: 0.3 }}
            className="goal-details"
          >
            <div className="goal-detail-row">
              <span>Category</span>
              <span>{goal.category || '—'}</span>
            </div>
            <div className="goal-detail-row">
              <span>Start Date</span>
              <span>{goal.startDate || '—'}</span>
            </div>
            <div className="goal-detail-row">
              <span>Target Date</span>
              <span>{goal.targetDate || '—'}</span>
            </div>
            <div className="goal-detail-row">
              <span>Status</span>
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
      userId: '', // Will be set by parent
    });
  };

  // Must match the `categories.name` values in the database (schema.sql)
  const categories = [
    { value: 'FOOD', label: 'Food & Dining' },
    { value: 'SHOPPING', label: 'Shopping' },
    { value: 'WEB_SHOPPING', label: 'Online Shopping' },
    { value: 'SUBSCRIPTION', label: 'Subscriptions' },
    { value: 'TRANSPORT', label: 'Transport' },
    { value: 'HEALTH', label: 'Health' },
    { value: 'UTILITIES', label: 'Utilities' },
    { value: 'RENT', label: 'Rent' },
    { value: 'EDUCATION', label: 'Education' },
    { value: 'ENTERTAINMENT', label: 'Entertainment' },
    { value: 'INVESTMENT', label: 'Investment' },
    { value: 'TRANSFER', label: 'Transfer' },
    { value: 'OTHER', label: 'Other' },
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
        initial={{ opacity: 0, scale: 0.9, y: 20 }}
        animate={{ opacity: 1, scale: 1, y: 0 }}
        exit={{ opacity: 0, scale: 0.9, y: 20 }}
        transition={{ type: 'spring', stiffness: 300, damping: 30 }}
        className="modal-card"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="modal-header">
          <h3>Create New Goal</h3>
          <button className="modal-close" onClick={onClose}>✕</button>
        </div>

        <form onSubmit={handleSubmit} className="modal-form">
          <div className="input-group">
            <label className="input-label">Category</label>
            <select
              name="category"
              value={form.category}
              onChange={handleChange}
              required
              className="auth-input"
            >
              <option value="">Select category</option>
              {categories.map(c => (
                <option key={c.value} value={c.value}>{c.label}</option>
              ))}
            </select>
          </div>

          <div className="input-group">
            <label className="input-label">Target Amount (₹)</label>
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
            <label className="input-label">Period</label>
            <select
              name="period"
              value={form.period}
              onChange={handleChange}
              className="auth-input"
            >
              <option value="WEEKLY">Weekly</option>
              <option value="MONTHLY">Monthly</option>
              <option value="QUARTERLY">Quarterly</option>
              <option value="YEARLY">Yearly</option>
            </select>
          </div>

          <div className="form-row">
            <div className="input-group">
              <label className="input-label">Start Date</label>
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
              <label className="input-label">End Date</label>
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
              Cancel
            </button>
            <button type="submit" className="btn btn-primary">
              Create Goal
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
  const [filter, setFilter] = useState('all'); // all, active, completed
  const [error, setError] = useState('');

  const userId = user?.userId || 'dev-user';

  const loadGoals = async () => {
    setLoading(true);
    try {
      const data = await goalService.getAll(userId);
      setGoals(data || []);
    } catch (err) {
      console.error('Failed to load goals:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (user) loadGoals();
  }, [userId, user]);

  const handleCreate = async (goalData) => {
    setError('');
    try {
      await goalService.create({ ...goalData, userId });
      setShowCreate(false);
      loadGoals();
    } catch (err) {
      setError(err.message || 'Failed to create goal. Please try again.');
    }
  };

  const handleDelete = async (goalId) => {
    setError('');
    try {
      await goalService.delete(goalId);
      setGoals(prev => prev.filter(g => g.id !== goalId));
    } catch (err) {
      setError(err.message || 'Failed to delete goal. Please try again.');
    }
  };

  const filteredGoals = goals.filter(g => {
    if (filter === 'active') return g.status === 'ACTIVE' || Number(g.currentAmount) < Number(g.targetAmount);
    if (filter === 'completed') return Number(g.currentAmount) >= Number(g.targetAmount);
    return true;
  });

  const totalTarget = goals.reduce((s, g) => s + Number(g.targetAmount || 0), 0);
  const totalSaved = goals.reduce((s, g) => s + Number(g.currentAmount || 0), 0);
  const completedCount = goals.filter(g => Number(g.currentAmount) >= Number(g.targetAmount)).length;

  return (
    <div className="page-container goals-page">
      {/* Header */}
      <motion.div
        initial={{ opacity: 0, y: -10 }}
        animate={{ opacity: 1, y: 0 }}
        className="page-header"
      >
        <div>
          <h1 className="page-title">
            <span className="gradient-text">Goals</span>
          </h1>
          <p className="page-subtitle">
            Track your spending limits and savings targets
          </p>
        </div>
        <motion.button
          className="btn btn-primary"
          onClick={() => setShowCreate(true)}
          whileHover={{ scale: 1.02 }}
          whileTap={{ scale: 0.98 }}
        >
          + New Goal
        </motion.button>
      </motion.div>

      {/* Error */}
      {error && (
        <motion.div
          initial={{ opacity: 0, y: -8 }}
          animate={{ opacity: 1, y: 0 }}
          className="auth-error"
          style={{ marginBottom: 'var(--space-lg)' }}
        >
          <span>⚠</span> {error}
        </motion.div>
      )}

      {/* Stats */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.1 }}
        className="goals-stats"
      >
        <div className="goal-stat-card">
          <span className="goal-stat-value">{goals.length}</span>
          <span className="goal-stat-label">Total Goals</span>
        </div>
        <div className="goal-stat-card">
          <span className="goal-stat-value">{completedCount}</span>
          <span className="goal-stat-label">Completed</span>
        </div>
        <div className="goal-stat-card">
          <span className="goal-stat-value">₹{totalSaved.toLocaleString('en-IN')}</span>
          <span className="goal-stat-label">Total Saved</span>
        </div>
        <div className="goal-stat-card">
          <span className="goal-stat-value">₹{totalTarget.toLocaleString('en-IN')}</span>
          <span className="goal-stat-label">Total Target</span>
        </div>
      </motion.div>

      {/* Filter */}
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ delay: 0.15 }}
        className="goals-filter"
      >
        {['all', 'active', 'completed'].map(f => (
          <button
            key={f}
            className={`filter-btn ${filter === f ? 'active' : ''}`}
            onClick={() => setFilter(f)}
          >
            {f.charAt(0).toUpperCase() + f.slice(1)}
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
            <span className="empty-icon">◎</span>
            <h3>No goals yet</h3>
            <p>Create your first spending goal to start tracking your progress.</p>
            <button className="btn btn-primary" onClick={() => setShowCreate(true)}>
              Create Goal
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
