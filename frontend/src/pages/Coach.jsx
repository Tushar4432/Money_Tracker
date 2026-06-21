import React, { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { useAuth } from '../context/AuthContext';
import { aiService } from '../api/aiService';
import AIChat from '../components/AIChat';
import './Coach.css';

function HealthScoreCard({ score, breakdown }) {
  const scoreVal = Number(score) || 0;
  const circumference = 2 * Math.PI * 54;
  const offset = circumference - (scoreVal / 100) * circumference;

  const color = scoreVal >= 80 ? '#4ade80' :
    scoreVal >= 60 ? '#f0a030' :
    scoreVal >= 40 ? '#60a5fa' : '#f87171';

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      className="health-score-card"
    >
      <h3 className="section-title">FINANCIAL_HEALTH</h3>
      <div className="health-score-ring">
        <svg width="120" height="120" viewBox="0 0 120 120">
          <circle cx="60" cy="60" r="54" fill="none" stroke="var(--border)" strokeWidth="6" />
          <motion.circle
            cx="60"
            cy="60"
            r="54"
            fill="none"
            stroke={color}
            strokeWidth="6"
            strokeLinecap="butt"
            strokeDasharray={circumference}
            initial={{ strokeDashoffset: circumference }}
            animate={{ strokeDashoffset: offset }}
            transition={{ duration: 1, ease: 'linear' }}
            transform="rotate(-90 60 60)"
          />
        </svg>
        <div className="health-score-value">
          <motion.span
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ delay: 0.3 }}
            style={{ color }}
          >
            {scoreVal}
          </motion.span>
          <small>/100</small>
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
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      transition={{ delay: 0.1 }}
      className="recommendations-card"
    >
      <h3 className="section-title">RECOMMENDATIONS</h3>
      <div className="recommendations-list">
        {recommendations.map((rec, i) => (
          <motion.div
            key={i}
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ delay: 0.1 + i * 0.05 }}
            className="recommendation-item"
          >
            <span className="rec-icon">▸</span>
            <p>{rec}</p>
          </motion.div>
        ))}
      </div>
    </motion.div>
  );
}

export default function Coach() {
  const { user } = useAuth();
  const [healthScore, setHealthScore] = useState(null);
  const [recommendations, setRecommendations] = useState([]);
  const [loading, setLoading] = useState(true);
  const userId = user?.userId || 'dev-user';

  // Load health score and recommendations from API
  useEffect(() => {
    if (!user) return;
    const loadData = async () => {
      try {
        const [scoreData, recData] = await Promise.allSettled([
          aiService.getHealthScore(userId),
          aiService.getRecommendations(userId),
        ]);
        if (scoreData.status === 'fulfilled') setHealthScore(scoreData.value);
        if (recData.status === 'fulfilled') setRecommendations(recData.value?.recommendations || []);
      } catch (err) {
        console.error('Failed to load coach data:', err);
      } finally {
        setLoading(false);
      }
    };
    loadData();
  }, [userId, user]);

  // Refresh health score and recommendations when chat sends a message
  // (the shared context handles this automatically now)

  return (
    <div className="page-container coach-page">
      {/* Header */}
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        className="page-header"
      >
        <div>
          <h1 className="page-title">▸ AI COACH</h1>
          <p className="page-subtitle">Personal financial advisor — terminal mode</p>
        </div>
        <div className="coach-header-badge">
          <span className="badge badge-accent">◈ AI POWERED</span>
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

        {/* Chat Area — shared AIChat component */}
        <div className="coach-chat-area">
          <AIChat
            variant="full"
            showSuggestions={true}
            showStatus={true}
            showClear={true}
          />
        </div>
      </div>
    </div>
  );
}
