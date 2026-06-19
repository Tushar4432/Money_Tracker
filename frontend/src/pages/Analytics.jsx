import React, { useState, useEffect, useCallback } from 'react';
import { motion } from 'framer-motion';
import { useAuth } from '../context/AuthContext';
import { analyticsService } from '../api/analyticsService';
import './Analytics.css';

// ─── Period Filter ───
function PeriodFilter({ value, onChange }) {
  const periods = [
    { label: 'Monthly', value: 'MONTHLY' },
    { label: 'Quarterly', value: 'QUARTERLY' },
    { label: 'Half-Yearly', value: 'HALF_YEARLY' },
    { label: 'Yearly', value: 'YEARLY' },
  ];
  return (
    <div className="period-filter">
      {periods.map(p => (
        <button key={p.value} className={`period-btn ${value === p.value ? 'active' : ''}`} onClick={() => onChange(p.value)}>
          {p.label}
        </button>
      ))}
    </div>
  );
}

// ─── Donut Chart ───
// Backend: CategoryBreakdown { category, amount, percentage }
function DonutChart({ data }) {
  if (!Array.isArray(data) || data.length === 0) {
    return (
      <div className="chart-empty">
        <span>◯</span>
        <p>No data available yet. Upload a statement to see your breakdown.</p>
      </div>
    );
  }

  const total = data.reduce((sum, d) => sum + Number(d.amount || 0), 0);
  if (total <= 0) {
    return (
      <div className="chart-empty"><span>◯</span><p>No spending data to display.</p></div>
    );
  }

  const cx = 100, cy = 100, r = 70;
  const circumference = 2 * Math.PI * r;
  const stroke = 28;
  const colors = ['#00f0ff','#10b981','#f59e0b','#f43f5e','#8b5cf6','#3b82f6','#ec4899','#14b8a6'];

  const segments = [];
  data.forEach((d, i) => {
    const val = Number(d.amount || 0);
    if (val <= 0) return;
    segments.push({ val, pct: val / total, color: colors[i % colors.length], label: d.category || 'Other', idx: i });
  });

  return (
    <div className="donut-chart-container">
      <svg width="200" height="200" viewBox="0 0 200 200" className="donut-chart">
        <g transform="rotate(-90 100 100)">
          {segments.map((seg, i) => {
            const dashLen = seg.pct * circumference;
            const gapLen = circumference - dashLen;
            const offset = -segments.slice(0, i).reduce((s, prev) => s + prev.pct, 0) * circumference;
            return (
              <circle key={i} cx={cx} cy={cy} r={r} fill="none" stroke={seg.color}
                strokeWidth={stroke} strokeDasharray={`${dashLen} ${gapLen}`}
                strokeDashoffset={offset} strokeLinecap="round" />
            );
          })}
        </g>
        <text x="100" y="95" textAnchor="middle" className="donut-total">
          ₹{total.toLocaleString('en-IN', { maximumFractionDigits: 0 })}
        </text>
        <text x="100" y="115" textAnchor="middle" className="donut-label">Total</text>
      </svg>
      <div className="donut-legend">
        {segments.map((seg, i) => (
          <div key={i} className="legend-item">
            <span className="legend-dot" style={{ background: seg.color }} />
            <span className="legend-name">{seg.label}</span>
            <span className="legend-value">₹{seg.val.toLocaleString('en-IN')}</span>
            <span className="legend-pct">{(seg.pct * 100).toFixed(1)}%</span>
          </div>
        ))}
      </div>
    </div>
  );
}

// ─── Bar Chart ───
// Backend: MonthlyTrend { month, income, expense, net }
// We show expense per month
function BarChart({ data }) {
  if (!Array.isArray(data) || data.length === 0) {
    return (
      <div className="chart-empty"><span>◯</span><p>No trend data available yet.</p></div>
    );
  }

  const safeData = data.filter(d => d != null);
  if (safeData.length === 0) {
    return (
      <div className="chart-empty"><span>◯</span><p>No trend data available.</p></div>
    );
  }

  // Use expense for the bar chart (spending per month)
  const values = safeData.map(d => Number(d.expense || d.amount || d.totalSpending || 0));
  const maxVal = Math.max(...values, 1);
  const barWidth = Math.max(24, Math.min(60, (500 / safeData.length) - 12));
  const gap = 12;
  const chartW = Math.max(safeData.length * (barWidth + gap) + 40, 360);
  const chartH = 200;

  return (
    <div className="bar-chart-wrapper">
      <svg width="100%" height={chartH + 40} viewBox={`0 0 ${chartW} ${chartH + 40}`}
        className="bar-chart" preserveAspectRatio="xMidYMid meet">
        <defs>
          <linearGradient id="barGradient" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="#00f0ff" stopOpacity="0.9" />
            <stop offset="100%" stopColor="#00a0e0" stopOpacity="0.4" />
          </linearGradient>
        </defs>
        {safeData.map((d, i) => {
          const val = Number(d.expense || d.amount || d.totalSpending || 0);
          const barH = maxVal > 0 ? (val / maxVal) * (chartH - 50) : 0;
          const x = 30 + i * (barWidth + gap);
          const y = chartH - barH - 25;
          const label = d.month || d.period || d.label || `${i + 1}`;
          return (
            <g key={i}>
              <rect x={x} y={y} width={barWidth} height={Math.max(barH, 0)} rx="6"
                fill="url(#barGradient)" className="bar-rect" />
              <text x={x + barWidth / 2} y={y - 6} textAnchor="middle" className="bar-value">
                {val > 0 ? `₹${(val / 1000).toFixed(0)}k` : '₹0'}
              </text>
              <text x={x + barWidth / 2} y={chartH + 2} textAnchor="middle" className="bar-label">
                {label.length > 7 ? label.substring(0, 7) : label}
              </text>
            </g>
          );
        })}
      </svg>
    </div>
  );
}

// ─── Comparison Table ───
// Backend: SpendingComparison { currentPeriod, previousPeriod, currentTotal, previousTotal, changeAmount, changePercentage, categoryComparisons: [{category, currentAmount, previousAmount, changeAmount, changePercentage}] }
function ComparisonTable({ data }) {
  // data is the full SpendingComparison object
  if (!data || typeof data !== 'object') {
    return (
      <div className="chart-empty"><span>◯</span><p>No comparison data available yet.</p></div>
    );
  }

  const categories = Array.isArray(data.categoryComparisons) ? data.categoryComparisons : [];

  return (
    <div className="comparison-table-wrapper">
      {/* Period summary */}
      <div className="comparison-summary">
        <div className="comparison-summary-row">
          <span>Current Period</span>
          <span className="comparison-value">{data.currentPeriod || '—'}</span>
        </div>
        <div className="comparison-summary-row">
          <span>Previous Period</span>
          <span className="comparison-value">{data.previousPeriod || '—'}</span>
        </div>
        <div className="comparison-summary-row">
          <span>Change</span>
          <span className={`comparison-value ${(Number(data.changeAmount) || 0) > 0 ? 'negative' : 'positive'}`}>
            {(Number(data.changeAmount) || 0) > 0 ? '↑' : '↓'} ₹{Math.abs(Number(data.changeAmount) || 0).toLocaleString('en-IN')}
            {data.changePercentage != null ? ` (${Number(data.changePercentage).toFixed(1)}%)` : ''}
          </span>
        </div>
      </div>

      {categories.length > 0 && (
        <>
          <h4 className="comparison-subtitle">By Category</h4>
          <table className="comparison-table">
            <thead>
              <tr>
                <th>Category</th>
                <th>Current</th>
                <th>Previous</th>
                <th>Change</th>
              </tr>
            </thead>
            <tbody>
              {categories.map((row, i) => {
                const current = Number(row.currentAmount || row.current || 0);
                const previous = Number(row.previousAmount || row.previous || 0);
                const change = Number(row.changeAmount || row.change || (current - previous) || 0);
                const isPositive = change > 0;
                return (
                  <tr key={i}>
                    <td className="category-cell">
                      <span className="category-name">{row.category || '—'}</span>
                    </td>
                    <td>₹{current.toLocaleString('en-IN')}</td>
                    <td className="muted">₹{previous.toLocaleString('en-IN')}</td>
                    <td className={`change-cell ${isPositive ? 'negative' : 'positive'}`}>
                      {isPositive ? '↑' : '↓'} ₹{Math.abs(change).toLocaleString('en-IN')}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </>
      )}
    </div>
  );
}

// ─── Budget Status ───
// Backend: BudgetStatus { category, targetAmount, spentAmount, remainingAmount, percentageUsed, period }
function BudgetStatus({ data }) {
  if (!Array.isArray(data) || data.length === 0) {
    return (
      <div className="chart-empty">
        <span>◯</span>
        <p>No budget goals set. Create goals to track your budget.</p>
      </div>
    );
  }

  return (
    <div className="budget-list">
      {data.map((item, i) => {
        const spent = Number(item.spentAmount || item.spent || item.currentAmount || 0);
        const budget = Number(item.targetAmount || item.budget || 0);
        const pct = budget > 0 ? Math.min((spent / budget) * 100, 100) : 0;
        const isOver = budget > 0 && spent > budget;
        const remaining = Math.max(budget - spent, 0);

        return (
          <motion.div key={i} initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }}
            transition={{ delay: i * 0.1 }} className="budget-item">
            <div className="budget-header">
              <span className="budget-category">{item.category || 'Category'}</span>
              <span className={`budget-amount ${isOver ? 'over' : ''}`}>
                ₹{spent.toLocaleString('en-IN')} / ₹{budget.toLocaleString('en-IN')}
              </span>
            </div>
            <div className="budget-bar">
              <motion.div className={`budget-fill ${isOver ? 'over' : ''}`}
                initial={{ width: 0 }} animate={{ width: `${pct}%` }}
                transition={{ duration: 0.8, delay: i * 0.1 }} />
            </div>
            <span className={`budget-pct ${isOver ? 'over' : ''}`}>
              {isOver ? 'Over budget! ' : ''}{pct.toFixed(0)}% used
              {!isOver && remaining > 0 ? ` · ₹${remaining.toLocaleString('en-IN')} left` : ''}
            </span>
          </motion.div>
        );
      })}
    </div>
  );
}

// ─── Main Analytics Component ───
const Analytics = () => {
  const { user } = useAuth();
  const [period, setPeriod] = useState('MONTHLY');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [categories, setCategories] = useState([]);
  const [trends, setTrends] = useState([]);
  const [comparison, setComparison] = useState(null);  // single object, not array
  const [budget, setBudget] = useState([]);
  const [summary, setSummary] = useState(null);

  const userId = user?.userId;

  const loadData = useCallback(async () => {
    if (!userId) return;
    setLoading(true);
    setError(null);

    const results = await Promise.allSettled([
      analyticsService.getCategories(userId),
      analyticsService.getTrends(userId, { period, periods: 6 }),
      analyticsService.getComparison(userId, { period }),
      analyticsService.getBudget(userId),
      analyticsService.getSummary(userId),
    ]);

    const [catRes, trendRes, compRes, budgetRes, summaryRes] = results;

    if (catRes.status === 'fulfilled') {
      setCategories(Array.isArray(catRes.value) ? catRes.value : []);
    } else {
      setCategories([]);
    }

    if (trendRes.status === 'fulfilled') {
      setTrends(Array.isArray(trendRes.value) ? trendRes.value : []);
    } else {
      setTrends([]);
    }

    // Comparison is a single SpendingComparison object (not an array)
    if (compRes.status === 'fulfilled') {
      setComparison(compRes.value && typeof compRes.value === 'object' ? compRes.value : null);
    } else {
      setComparison(null);
    }

    if (budgetRes.status === 'fulfilled') {
      setBudget(Array.isArray(budgetRes.value) ? budgetRes.value : []);
    } else {
      setBudget([]);
    }

    if (summaryRes.status === 'fulfilled') {
      setSummary(summaryRes.value && typeof summaryRes.value === 'object' ? summaryRes.value : null);
    } else {
      setSummary(null);
    }

    const allFailed = results.every(r => r.status === 'rejected');
    if (allFailed) {
      setError('Failed to load analytics data. Please try again.');
    }

    setLoading(false);
  }, [userId, period]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const fmt = (val) => {
    if (val == null) return '—';
    const n = Number(val);
    if (isNaN(n)) return '—';
    return `₹${n.toLocaleString('en-IN', { maximumFractionDigits: 0 })}`;
  };

  return (
    <div className="page-container analytics-page">
      <motion.div initial={{ opacity: 0, y: -10 }} animate={{ opacity: 1, y: 0 }} className="page-header">
        <div>
          <h1 className="page-title"><span className="gradient-text">Analytics</span></h1>
          <p className="page-subtitle">Deep dive into your spending patterns and trends</p>
        </div>
        <PeriodFilter value={period} onChange={setPeriod} />
      </motion.div>

      {error && (
        <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="analytics-error">
          <span>⚠</span> {error}
        </motion.div>
      )}

      {/* Quick Stats from SpendingSummary */}
      {summary && (
        <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.1 }} className="analytics-quick-stats">
          <div className="quick-stat">
            <span className="quick-stat-label">Total Income</span>
            <span className="quick-stat-value">{fmt(summary.totalIncome)}</span>
          </div>
          <div className="quick-stat">
            <span className="quick-stat-label">Total Expenses</span>
            <span className="quick-stat-value">{fmt(summary.totalExpense)}</span>
          </div>
          <div className="quick-stat">
            <span className="quick-stat-label">Net Savings</span>
            <span className="quick-stat-value">{fmt(summary.netSavings)}</span>
          </div>
          <div className="quick-stat">
            <span className="quick-stat-label">Transactions</span>
            <span className="quick-stat-value">{summary.transactionCount ?? '—'}</span>
          </div>
        </motion.div>
      )}

      {/* Loading skeletons */}
      {loading && !summary && (
        <div className="analytics-grid">
          {[1, 2, 3, 4].map(i => (
            <div key={i} className="analytics-card">
              <div className="skeleton" style={{ height: 260 }} />
            </div>
          ))}
        </div>
      )}

      {/* Charts Grid */}
      {!loading && (
        <div className="analytics-grid">
          <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.15 }} className="analytics-card">
            <h3 className="card-title">Spending by Category</h3>
            <DonutChart data={categories} />
          </motion.div>

          <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.2 }} className="analytics-card">
            <h3 className="card-title">Spending Trends</h3>
            <BarChart data={trends} />
          </motion.div>

          <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.25 }} className="analytics-card">
            <h3 className="card-title">Period Comparison</h3>
            <ComparisonTable data={comparison} />
          </motion.div>

          <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.3 }} className="analytics-card">
            <h3 className="card-title">Budget Status</h3>
            <BudgetStatus data={budget} />
          </motion.div>
        </div>
      )}
    </div>
  );
};

export default Analytics;
