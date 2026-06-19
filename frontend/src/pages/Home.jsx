import React, { useState, useRef, useEffect, useCallback } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { useAuth } from '../context/AuthContext';
import { transactionService } from '../api/transactionService';
import { analyticsService } from '../api/analyticsService';
import { aiService } from '../api/aiService';
import { authService } from '../api/authService';
import './Home.css';

// ─── Summary Card ───
function SummaryCard({ label, value, change, icon, accent, delay = 0 }) {
  const accentColor = accent === 'cyan' ? 'var(--accent-cyan)' :
    accent === 'emerald' ? 'var(--accent-emerald)' :
    accent === 'amber' ? 'var(--accent-amber)' : 'var(--accent-rose)';
  const accentDim = accent === 'cyan' ? 'var(--accent-cyan-dim)' :
    accent === 'emerald' ? 'var(--accent-emerald-dim)' :
    accent === 'amber' ? 'var(--accent-amber-dim)' : 'var(--accent-rose-dim)';

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.5, delay }}
      className="summary-card"
      style={{ '--card-accent': accentColor, '--card-accent-dim': accentDim }}
    >
      <div className="summary-card-header">
        <span className="summary-icon">{icon}</span>
        <span className="summary-label">{label}</span>
      </div>
      <div className="summary-value">{value}</div>
      {change !== undefined && (
        <div className={`summary-change ${change >= 0 ? 'positive' : 'negative'}`}>
          {change >= 0 ? '↑' : '↓'} {Math.abs(change)}% vs last month
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
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.5, delay: 0.1 }}
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
            <p className="upload-text">Processing statement...</p>
            <p className="upload-subtext">Extracting and categorizing transactions</p>
          </>
        ) : (
          <>
            <div className="upload-icon"><span>↑</span></div>
            <p className="upload-text">Drop your bank statement here</p>
            <p className="upload-subtext">Supports CSV, XLSX, XLS files</p>
            <div className="upload-formats">
              <span className="badge badge-cyan">.CSV</span>
              <span className="badge badge-emerald">.XLSX</span>
              <span className="badge badge-amber">.XLS</span>
            </div>
          </>
        )}
      </div>
    </motion.div>
  );
}

// ─── Chat Message ───
function ChatMessage({ message, isUser }) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 10 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.3 }}
      className={`chat-message ${isUser ? 'user' : 'ai'}`}
    >
      <div className="chat-avatar">{isUser ? '◌' : '◬'}</div>
      <div className="chat-bubble">
        <p>{message.content}</p>
        <span className="chat-time">{message.time}</span>
      </div>
    </motion.div>
  );
}

// ─── AI Chat ───
function AIChat({ userId, onStatusChange }) {
  const [messages, setMessages] = useState([
    {
      content: "Hi! I'm your AI Financial Coach. Upload a bank statement to get started, or ask me anything about your spending habits.",
      isUser: false,
      time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
    }
  ]);
  const [input, setInput] = useState('');
  const [typing, setTyping] = useState(false);
  const [aiOnline, setAiOnline] = useState(null); // null = unknown, true = online, false = offline
  const messagesEndRef = useRef(null);
  const inputRef = useRef(null);

  const suggestedQuestions = [
    "How much did I spend last month?",
    "Where do I spend the most?",
    "Am I saving enough?",
    "Can I afford a ₹50,000 purchase?",
  ];

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => { scrollToBottom(); }, [messages]);

  // Check AI connectivity on mount by loading chat history (no fake message sent)
  useEffect(() => {
    const checkAi = async () => {
      try {
        await aiService.getChatHistory(userId);
        setAiOnline(true);
        onStatusChange?.(true);
      } catch (err) {
        const msg = err?.message || '';
        if (!msg.includes('Session expired') && !msg.includes('Not authenticated')) {
          setAiOnline(false);
          onStatusChange?.(false);
        }
      }
    };
    checkAi();
    const interval = setInterval(checkAi, 30000);
    return () => clearInterval(interval);
  }, [userId, onStatusChange]);

  const sendMessage = async (text) => {
    if (!text.trim() || typing) return;

    const userMsg = {
      content: text.trim(),
      isUser: true,
      time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
    };
    setMessages(prev => [...prev, userMsg]);
    setInput('');
    setTyping(true);

    try {
      const response = await aiService.chat(userId, text.trim());
      const aiMsg = {
        content: response.reply || "I'm analyzing your request. Please try again.",
        isUser: false,
        time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      };
      setMessages(prev => [...prev, aiMsg]);
      if (!aiOnline) { setAiOnline(true); onStatusChange?.(true); }
    } catch (err) {
      const errMsg = {
        content: err.message || "I'm having trouble connecting right now. Please try again in a moment.",
        isUser: false,
        time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      };
      setMessages(prev => [...prev, errMsg]);
      if (aiOnline) { setAiOnline(false); onStatusChange?.(false); }
    } finally {
      setTyping(false);
      inputRef.current?.focus();
    }
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    sendMessage(input);
  };

  const aiStatusLabel = aiOnline === null ? 'Checking...' : aiOnline ? 'Online' : 'Offline';
  const aiStatusClass = aiOnline === null ? '' : aiOnline ? 'online' : 'offline';

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.5, delay: 0.3 }}
      className="chat-container"
    >
      <div className="chat-header">
        <div className="chat-header-icon">◬</div>
        <div>
          <h3>AI Financial Coach</h3>
          <span className={`chat-status ${aiStatusClass}`}>
            <span className={`status-dot ${aiStatusClass}`} />
            {aiStatusLabel}
          </span>
        </div>
      </div>

      <div className="chat-messages">
        {messages.map((msg, i) => (
          <ChatMessage key={i} message={msg} isUser={msg.isUser} />
        ))}
        {typing && (
          <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="chat-message ai">
            <div className="chat-avatar">◬</div>
            <div className="chat-bubble typing">
              <span className="typing-dot" />
              <span className="typing-dot" />
              <span className="typing-dot" />
            </div>
          </motion.div>
        )}
        <div ref={messagesEndRef} />
      </div>

      {messages.length <= 1 && (
        <div className="chat-suggestions">
          {suggestedQuestions.map((q, i) => (
            <button key={i} className="suggestion-chip" onClick={() => sendMessage(q)}>{q}</button>
          ))}
        </div>
      )}

      <form onSubmit={handleSubmit} className="chat-input-form">
        <input
          ref={inputRef} type="text" value={input}
          onChange={(e) => setInput(e.target.value)}
          placeholder="Ask about your finances..."
          className="chat-input" disabled={typing}
        />
        <button type="submit" className="chat-send-btn" disabled={!input.trim() || typing}>↑</button>
      </form>
    </motion.div>
  );
}

// ─── Recent Transactions (scrollable) ───
function RecentTransactions({ transactions }) {
  const listRef = useRef(null);

  if (!transactions || transactions.length === 0) {
    return (
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5, delay: 0.4 }}
        className="recent-transactions"
      >
        <h3 className="section-title">Recent Transactions</h3>
        <div className="empty-state">
          <span className="empty-icon">◯</span>
          <p>No transactions yet. Upload a bank statement to get started.</p>
        </div>
      </motion.div>
    );
  }

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.5, delay: 0.4 }}
      className="recent-transactions"
    >
      <div className="transactions-header">
        <h3 className="section-title">Recent Transactions</h3>
        <span className="transactions-count">{transactions.length} total</span>
      </div>
      <div className="transactions-list" ref={listRef}>
        {transactions.map((tx, i) => (
          <motion.div
            key={tx.id || i}
            initial={{ opacity: 0, x: -10 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ duration: 0.3, delay: 0.4 + Math.min(i * 0.04, 0.3) }}
            className="transaction-item"
          >
            <div className="tx-left">
              <div className="tx-category-icon">
                {(tx.category && tx.category.charAt(0)) || '◯'}
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
  const [aiStatus, setAiStatus] = useState('checking');

  const userId = user?.userId;

  // ── Backend health check ──
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

  // Run health check on mount + periodically
  useEffect(() => {
    if (!user) return;
    // Small delay to let token settle after login redirect
    const timer = setTimeout(() => {
      checkBackendHealth();
    }, 300);
    const interval = setInterval(checkBackendHealth, 15000);
    return () => {
      clearTimeout(timer);
      clearInterval(interval);
    };
  }, [user, checkBackendHealth]);

  // Mark as live once data loads successfully
  useEffect(() => {
    if (!loading && summary !== null) {
      setBackendStatus('live');
    }
  }, [loading, summary]);

  // ── Load dashboard data ──
  const loadData = useCallback(async () => {
    if (!userId) return;
    setLoading(true);
    setDataError(null);
    try {
      const [summaryData, txData] = await Promise.allSettled([
        analyticsService.getSummary(userId),
        transactionService.getAll(userId),
      ]);

      if (summaryData.status === 'fulfilled') {
        setSummary(summaryData.value);
      }
      if (txData.status === 'fulfilled') {
        setTransactions(Array.isArray(txData.value) ? txData.value : []);
      }

      // Show error if both failed
      const bothFailed = summaryData.status === 'rejected' && txData.status === 'rejected';
      if (bothFailed) {
        const errMsg = summaryData.reason?.message || txData.reason?.message || 'Failed to load dashboard data';
        setDataError(errMsg);
      }
    } catch (err) {
      setDataError(err.message || 'Failed to load dashboard data');
    } finally {
      setLoading(false);
    }
  }, [userId]);

  // Load on mount and whenever userId changes
  useEffect(() => {
    loadData();
  }, [loadData]);

  // Also reload when user becomes available (e.g. after login redirect)
  useEffect(() => {
    if (user && userId) {
      loadData();
    }
  }, [user, userId, loadData]);

  // Reload when tab becomes visible again
  useEffect(() => {
    const handleVisibility = () => {
      if (document.visibilityState === 'visible' && userId) {
        loadData();
      }
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

  // Backend SpendingSummary fields: totalIncome, totalExpense, netSavings, topCategory, transactionCount
  const totalIncome = summary?.totalIncome;
  const totalExpense = summary?.totalExpense;
  const netSavings = summary?.netSavings;
  const savingsRate = totalIncome > 0 ? ((netSavings / totalIncome) * 100).toFixed(1) : null;

  // Status badge
  const statusBadge = backendStatus === 'live'
    ? { className: 'badge-emerald', text: '● Live', dot: true }
    : backendStatus === 'checking'
    ? { className: 'badge-amber', text: '● Connecting...', dot: true }
    : { className: 'badge-rose', text: '● Offline', dot: false };

  return (
    <div className="page-container home-page">
      {/* Header */}
      <motion.div
        initial={{ opacity: 0, y: -10 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.4 }}
        className="page-header"
      >
        <div>
          <h1 className="page-title"><span className="gradient-text">Dashboard</span></h1>
          <p className="page-subtitle">
            Welcome back, {user?.username || 'User'} — here's your financial overview
          </p>
        </div>
        <div className="page-header-right">
          <span className={`badge ${statusBadge.className}`}>{statusBadge.text}</span>
        </div>
      </motion.div>

      {/* Data error */}
      {dataError && (
        <motion.div initial={{ opacity: 0, y: -8 }} animate={{ opacity: 1, y: 0 }}
          className="upload-success" style={{ marginBottom: 'var(--space-lg)', justifyContent: 'space-between' }}>
          <span><span style={{ color: 'var(--accent-rose)' }}>⚠</span> {dataError}</span>
          <button onClick={loadData} className="link-button" style={{ fontSize: '0.8rem' }}>Retry</button>
        </motion.div>
      )}

      {/* Summary Cards */}
      <div className="summary-grid">
        <SummaryCard
          label="Total Income"
          value={formatCurrency(totalIncome)}
          icon="◈"
          accent="emerald"
          delay={0.05}
        />
        <SummaryCard
          label="Total Expenses"
          value={formatCurrency(totalExpense)}
          icon="◉"
          accent="rose"
          delay={0.1}
        />
        <SummaryCard
          label="Net Savings"
          value={formatCurrency(netSavings)}
          icon="◎"
          accent="cyan"
          delay={0.15}
        />
        <SummaryCard
          label="Savings Rate"
          value={savingsRate ? `${savingsRate}%` : '—'}
          icon="◬"
          accent="amber"
          delay={0.2}
        />
      </div>

      {/* Main Grid */}
      <div className="home-main-grid">
        <div className="home-left">
          <UploadZone onUpload={handleUpload} uploading={uploading} />
          <AnimatePresence>
            {uploadSuccess && (
              <motion.div
                initial={{ opacity: 0, y: 8 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -8 }}
                className="upload-success"
              >
                <span>✓</span> Statement processed successfully! Your data has been updated.
              </motion.div>
            )}
          </AnimatePresence>
          <RecentTransactions transactions={transactions} />
        </div>

        <div className="home-right">
          <AIChat userId={userId} onStatusChange={(online) => setAiStatus(online ? 'online' : 'offline')} />
        </div>
      </div>
    </div>
  );
};

export default Home;
