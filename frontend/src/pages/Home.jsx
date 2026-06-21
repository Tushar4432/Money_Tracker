import React, { useState, useRef, useCallback, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { useAuth } from '../context/AuthContext';
import { transactionService } from '../api/transactionService';
import { analyticsService } from '../api/analyticsService';
import { authService } from '../api/authService';
import { apiClient } from '../api/client';
import AIChat from '../components/AIChat';
import './Home.css';

// ─── Summary Card ───
function SummaryCard({ label, value, change, icon, delay = 0 }) {
  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      transition={{ duration: 0.3, delay }}
      className="summary-card"
    >
      <div className="summary-card-header">
        <span className="summary-icon">{icon}</span>
        <span className="summary-label">{label}</span>
      </div>
      <div className="summary-value">{value}</div>
      {change !== undefined && (
        <div className={`summary-change ${change >= 0 ? 'positive' : 'negative'}`}>
          {change >= 0 ? '▲' : '▼'} {Math.abs(change)}% vs last month
        </div>
      )}
    </motion.div>
  );
}

// ─── Upload Zone ───
function UploadZone({ onUpload, uploading }) {
  const [dragOver, setDragOver] = useState(false);
  const fileInputRef = useRef(null);

  const handleDrop = useCallback((e) => {
    e.preventDefault();
    setDragOver(false);
    const file = e.dataTransfer.files[0];
    if (file) onUpload(file);
  }, [onUpload]);

  const handleFileSelect = (e) => {
    const file = e.target.files[0];
    if (file) onUpload(file);
  };

  return (
    <div
      className={`upload-zone ${dragOver ? 'drag-over' : ''} ${uploading ? 'uploading' : ''}`}
      onDragOver={(e) => { e.preventDefault(); setDragOver(true); }}
      onDragLeave={() => setDragOver(false)}
      onDrop={handleDrop}
      onClick={() => !uploading && fileInputRef.current?.click()}
    >
      <input ref={fileInputRef} type="file" accept=".csv,.xlsx,.xls" onChange={handleFileSelect} style={{ display: 'none' }} />
      <div className="upload-content">
        {uploading ? (
          <>
            <div className="upload-spinner" />
            <p className="upload-text">PROCESSING STATEMENT...</p>
            <p className="upload-subtext">Extracting and categorizing transactions</p>
          </>
        ) : (
          <>
            <div className="upload-icon">↑</div>
            <p className="upload-text">DROP BANK STATEMENT</p>
            <p className="upload-subtext">Supports CSV, XLSX, XLS</p>
            <div className="upload-formats">
              <span className="badge">.CSV</span>
              <span className="badge">.XLSX</span>
              <span className="badge">.XLS</span>
            </div>
          </>
        )}
      </div>
    </div>
  );
}

// ─── Recent Transactions ───
function RecentTransactions({ transactions }) {
  if (!transactions || transactions.length === 0) {
    return (
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ duration: 0.3 }}
        className="recent-transactions"
      >
        <h3 className="section-title">TRANSACTIONS</h3>
        <div className="empty-state">
          <span className="empty-icon">○</span>
          <p>No transactions. Upload a bank statement.</p>
        </div>
      </motion.div>
    );
  }

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      transition={{ duration: 0.3 }}
      className="recent-transactions"
    >
      <div className="transactions-header">
        <h3 className="section-title">TRANSACTIONS</h3>
        <span className="transactions-count">{transactions.length} entries</span>
      </div>
      <div className="transactions-list">
        {transactions.map((tx, i) => (
          <motion.div
            key={tx.id || i}
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ duration: 0.2, delay: Math.min(i * 0.03, 0.3) }}
            className="transaction-item"
          >
            <div className="tx-left">
              <div className="tx-category-icon">
                {(tx.category && tx.category.charAt(0)) || '?'}
              </div>
              <div className="tx-details">
                <span className="tx-merchant">{tx.merchant || tx.description || 'Unknown'}</span>
                <span className="tx-category">{tx.category || 'Uncategorized'}</span>
              </div>
            </div>
            <div className="tx-right">
              <span className={`tx-amount ${tx.transaction_type === 'CREDIT' ? 'credit' : 'debit'}`}>
                {tx.transaction_type === 'CREDIT' ? '+' : '-'}₹{Number(tx.amount || 0).toLocaleString('en-IN')}
              </span>
              <span className="tx-date">{tx.transaction_date || ''}</span>
            </div>
          </motion.div>
        ))}
      </div>
    </motion.div>
  );
}

// ─── Main Home / Dashboard ───
const Home = () => {
  const { user } = useAuth();
  const [uploading, setUploading] = useState(false);
  const [uploadSuccess, setUploadSuccess] = useState(false);
  const [summary, setSummary] = useState(null);
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [dataError, setDataError] = useState(null);
  const [backendStatus, setBackendStatus] = useState('checking');

  const userId = user?.userId;

  const checkBackendHealth = useCallback(async () => {
    if (!apiClient.getToken()) {
      setBackendStatus('offline');
      return;
    }
    try {
      const result = await authService.healthCheck();
      setBackendStatus(result?.status === 'ok' ? 'live' : 'offline');
    } catch {
      setBackendStatus('offline');
    }
  }, []);

  useEffect(() => {
    if (!user) return;
    const timer = setTimeout(() => { checkBackendHealth(); }, 300);
    const interval = setInterval(checkBackendHealth, 15000);
    return () => { clearTimeout(timer); clearInterval(interval); };
  }, [user, checkBackendHealth]);

  useEffect(() => {
    if (loading && summary !== null) {
      setBackendStatus('live');
    }
  }, [loading, summary]);

  const loadData = useCallback(async () => {
    if (!userId) return;
    setLoading(true);
    setDataError(null);
    try {
      const [summaryData, txData] = await Promise.allSettled([
        analyticsService.getSummary(userId),
        transactionService.getAll(userId),
      ]);
      if (summaryData.status === 'fulfilled') setSummary(summaryData.value);
      if (txData.status === 'fulfilled') setTransactions(Array.isArray(txData.value) ? txData.value : []);
      const bothFailed = summaryData.status === 'rejected' && txData.status === 'rejected';
      if (bothFailed) {
        setDataError(summaryData.reason?.message || txData.reason?.message || 'Failed to load dashboard data');
      }
    } catch (err) {
      setDataError(err.message || 'Failed to load dashboard data');
    } finally {
      setLoading(false);
    }
  }, [userId]);

  useEffect(() => { loadData(); }, [loadData]);

  useEffect(() => {
    const handleVisibility = () => {
      if (document.visibilityState === 'visible' && userId) loadData();
    };
    document.addEventListener('visibilitychange', handleVisibility);
    return () => document.removeEventListener('visibilitychange', handleVisibility);
  }, [userId, loadData]);

  const handleUpload = async (file) => {
    setUploading(true);
    setUploadSuccess(false);
    try {
      await transactionService.upload(file, userId);
      setUploadSuccess(true);
      const [summaryData, txData] = await Promise.allSettled([
        analyticsService.getSummary(userId),
        transactionService.getAll(userId),
      ]);
      if (summaryData.status === 'fulfilled') setSummary(summaryData.value);
      if (txData.status === 'fulfilled') setTransactions(Array.isArray(txData.value) ? txData.value : []);
      setTimeout(() => setUploadSuccess(false), 3000);
    } catch (err) {
      console.error('Upload failed:', err);
    } finally {
      setUploading(false);
    }
  };

  const formatCurrency = (val) => {
    if (val === undefined || val === null) return '₹0';
    return `₹${Number(val).toLocaleString('en-IN', { maximumFractionDigits: 0 })}`;
  };

  const totalIncome = summary?.totalIncome;
  const totalExpense = summary?.totalExpense;
  const netSavings = summary?.netSavings;
  const savingsRate = totalIncome > 0 ? ((netSavings / totalIncome) * 100).toFixed(1) : null;

  const statusBadge = backendStatus === 'live'
    ? { className: 'badge badge-success', text: '● LIVE' }
    : backendStatus === 'checking'
    ? { className: 'badge badge-accent', text: '● CONNECTING' }
    : { className: 'badge badge-danger', text: '● OFFLINE' };

  return (
    <div className="page-container home-page">
      {/* Header */}
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ duration: 0.3 }}
        className="page-header"
      >
        <div>
          <h1 className="page-title">
            <span className="accent">◈</span> DASHBOARD
          </h1>
          <p className="page-subtitle">
            User: {user?.username || 'Unknown'} — Financial overview
          </p>
        </div>
        <div className="page-header-right">
          <span className={`badge ${statusBadge.className}`}>{statusBadge.text}</span>
        </div>
      </motion.div>

      {/* Data error */}
      {dataError && (
        <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }}
          className="upload-success" style={{ marginBottom: 'var(--sp-6)', justifyContent: 'space-between' }}>
          <span>! {dataError}</span>
          <button onClick={loadData} className="link-button" style={{ fontSize: '0.7rem' }}>RETRY</button>
        </motion.div>
      )}

      {/* Summary Cards */}
      <div className="summary-grid">
        <SummaryCard label="INCOME" value={formatCurrency(totalIncome)} icon="▲" delay={0.02} />
        <SummaryCard label="EXPENSES" value={formatCurrency(totalExpense)} icon="▼" delay={0.04} />
        <SummaryCard label="NET_SAVINGS" value={formatCurrency(netSavings)} icon="=" delay={0.06} />
        <SummaryCard label="SAVINGS_RATE" value={savingsRate ? `${savingsRate}%` : '—'} icon="%" delay={0.08} />
      </div>

      {/* Main Grid */}
      <div className="home-main-grid">
        <div className="home-left">
          <UploadZone onUpload={handleUpload} uploading={uploading} />
          <AnimatePresence>
            {uploadSuccess && (
              <motion.div
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
                className="upload-success"
              >
                ✓ Statement processed. Data updated.
              </motion.div>
            )}
          </AnimatePresence>
          <RecentTransactions transactions={transactions} />
        </div>

        <div className="home-right">
          <AIChat variant="compact" showSuggestions={true} showStatus={true} showClear={true} />
        </div>
      </div>
    </div>
  );
};

export default Home;
