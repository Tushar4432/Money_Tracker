import React, { useState, useEffect, useRef } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { useAuth } from '../context/AuthContext';
import { aiService } from '../api/aiService';
import { analyticsService } from '../api/analyticsService';
import './Coach.css';

function ChatMessage({ message, isUser }) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.3 }}
      className={`coach-message ${isUser ? 'user' : 'ai'}`}
    >
      <div className="coach-msg-avatar">
        {isUser ? '◌' : '◬'}
      </div>
      <div className="coach-msg-content">
        <div className="coach-msg-bubble">
          {message.content.split('\n').map((line, i) => (
            <p key={i}>{line}</p>
          ))}
        </div>
        <span className="coach-msg-time">{message.time}</span>
      </div>
    </motion.div>
  );
}

function HealthScoreCard({ score, breakdown }) {
  const scoreVal = Number(score) || 0;
  const circumference = 2 * Math.PI * 54;
  const offset = circumference - (scoreVal / 100) * circumference;

  const color = scoreVal >= 80 ? '#10b981' :
    scoreVal >= 60 ? '#00f0ff' :
    scoreVal >= 40 ? '#f59e0b' : '#f43f5e';

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      className="health-score-card"
    >
      <h3 className="section-title">Financial Health</h3>
      <div className="health-score-ring">
        <svg width="140" height="140" viewBox="0 0 140 140">
          <circle cx="70" cy="70" r="54" fill="none" stroke="rgba(255,255,255,0.05)" strokeWidth="10" />
          <motion.circle
            cx="70"
            cy="70"
            r="54"
            fill="none"
            stroke={color}
            strokeWidth="10"
            strokeLinecap="round"
            strokeDasharray={circumference}
            initial={{ strokeDashoffset: circumference }}
            animate={{ strokeDashoffset: offset }}
            transition={{ duration: 1.2, ease: 'easeOut' }}
            transform="rotate(-90 70 70)"
          />
        </svg>
        <div className="health-score-value">
          <motion.span
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ delay: 0.5 }}
            style={{ color }}
          >
            {scoreVal}
          </motion.span>
          <small>/ 100</small>
        </div>
      </div>
      {breakdown && (
        <p className="health-breakdown">{breakdown}</p>
      )}
    </motion.div>
  );
}

function RecommendationCard({ recommendations }) {
  if (!recommendations || recommendations.length === 0) return null;

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: 0.2 }}
      className="recommendations-card"
    >
      <h3 className="section-title">AI Recommendations</h3>
      <div className="recommendations-list">
        {recommendations.map((rec, i) => (
          <motion.div
            key={i}
            initial={{ opacity: 0, x: -10 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ delay: 0.3 + i * 0.1 }}
            className="recommendation-item"
          >
            <span className="rec-icon">◈</span>
            <p>{rec}</p>
          </motion.div>
        ))}
      </div>
    </motion.div>
  );
}

export default function Coach() {
  const { user } = useAuth();
  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState('');
  const [typing, setTyping] = useState(false);
  const [healthScore, setHealthScore] = useState(null);
  const [recommendations, setRecommendations] = useState([]);
  const [loading, setLoading] = useState(true);
  const messagesEndRef = useRef(null);
  const inputRef = useRef(null);

  const userId = user?.userId || 'dev-user';

  const suggestedQuestions = [
    "How am I doing financially?",
    "Where can I cut spending?",
    "Can I afford ₹1,00,000 vacation?",
    "How much should I save each month?",
    "Am I on track with my goals?",
    "What subscriptions should I cancel?",
  ];

  useEffect(() => {
    const loadData = async () => {
      try {
        const [scoreData, recData, historyData] = await Promise.allSettled([
          aiService.getHealthScore(userId),
          aiService.getRecommendations(userId),
          aiService.getChatHistory(userId),
        ]);

        if (scoreData.status === 'fulfilled') {
          setHealthScore(scoreData.value);
        }
        if (recData.status === 'fulfilled') {
          setRecommendations(recData.value?.recommendations || []);
        }

        // Load chat history if available
        if (historyData.status === 'fulfilled' && Array.isArray(historyData.value) && historyData.value.length > 0) {
          const historyMsgs = historyData.value.map(msg => ({
            content: msg.content,
            isUser: msg.role === 'USER',
            time: new Date(msg.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
          }));
          setMessages(historyMsgs);
        } else {
          // No history — show welcome message
          setMessages([{
            content: "Hello! I'm your AI Financial Coach. I have access to your spending data, goals, and financial patterns. Ask me anything — whether it's about affordability, savings strategies, or spending optimization.",
            isUser: false,
            time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
          }]);
        }
      } catch (err) {
        console.error('Failed to load coach data:', err);
        setMessages([{
          content: "Hello! I'm your AI Financial Coach. Ask me anything about your finances.",
          isUser: false,
          time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
        }]);
      } finally {
        setLoading(false);
      }
    };

    if (user) {
      loadData();
    }
  }, [userId, user]);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

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
        content: response.reply || "I'm analyzing your financial data. Please try again.",
        isUser: false,
        time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      };
      setMessages(prev => [...prev, aiMsg]);

      // Refresh health score and recommendations after chat
      const [scoreData, recData] = await Promise.allSettled([
        aiService.getHealthScore(userId),
        aiService.getRecommendations(userId),
      ]);
      if (scoreData.status === 'fulfilled') setHealthScore(scoreData.value);
      if (recData.status === 'fulfilled') setRecommendations(recData.value?.recommendations || []);
    } catch (err) {
      const errMsg = {
        content: "I'm having trouble connecting right now. Please try again in a moment.",
        isUser: false,
        time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      };
      setMessages(prev => [...prev, errMsg]);
    } finally {
      setTyping(false);
      inputRef.current?.focus();
    }
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    sendMessage(input);
  };

  return (
    <div className="page-container coach-page">
      {/* Header */}
      <motion.div
        initial={{ opacity: 0, y: -10 }}
        animate={{ opacity: 1, y: 0 }}
        className="page-header"
      >
        <div>
          <h1 className="page-title">
            <span className="gradient-text">AI Coach</span>
          </h1>
          <p className="page-subtitle">
            Your personal financial advisor, powered by AI
          </p>
        </div>
        <div className="coach-header-badge">
          <span className="badge badge-cyan">◬ AI Powered</span>
        </div>
      </motion.div>

      {/* Main Layout */}
      <div className="coach-layout">
        {/* Sidebar */}
        <div className="coach-sidebar">
          <HealthScoreCard
            score={healthScore?.score}
            breakdown={healthScore?.breakdown}
          />
          <RecommendationCard recommendations={recommendations} />
        </div>

        {/* Chat Area */}
        <div className="coach-chat-area">
          <div className="coach-chat-messages">
            {messages.map((msg, i) => (
              <ChatMessage key={i} message={msg} isUser={msg.isUser} />
            ))}
            {typing && (
              <motion.div
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                className="coach-message ai"
              >
                <div className="coach-msg-avatar">◬</div>
                <div className="coach-msg-content">
                  <div className="coach-msg-bubble typing">
                    <span className="typing-dot" />
                    <span className="typing-dot" />
                    <span className="typing-dot" />
                  </div>
                </div>
              </motion.div>
            )}
            <div ref={messagesEndRef} />
          </div>

          {messages.length <= 1 && (
            <div className="coach-suggestions">
              <p className="suggestions-label">Try asking:</p>
              <div className="suggestions-grid">
                {suggestedQuestions.map((q, i) => (
                  <button
                    key={i}
                    className="suggestion-chip"
                    onClick={() => sendMessage(q)}
                  >
                    {q}
                  </button>
                ))}
              </div>
            </div>
          )}

          <form onSubmit={handleSubmit} className="coach-input-form">
            <input
              ref={inputRef}
              type="text"
              value={input}
              onChange={(e) => setInput(e.target.value)}
              placeholder="Ask about your finances, goals, or affordability..."
              className="coach-input"
              disabled={typing}
            />
            <button
              type="submit"
              className="coach-send-btn"
              disabled={!input.trim() || typing}
            >
              ↑
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}
