import React, { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { useAIChat } from '../context/AIChatContext';
import './AIChat.css';

const SUGGESTED_QUESTIONS = [
  "How much did I spend last month?",
  "Where do I spend the most?",
  "Am I saving enough?",
  "Can I afford a ₹50,000 purchase?",
];

function ChatMessage({ message, isUser }) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.2 }}
      className={`ai-chat-msg ${isUser ? 'user' : 'ai'}`}
    >
      <div className="ai-chat-avatar">{isUser ? '>' : 'AI'}</div>
      <div className="ai-chat-bubble-wrap">
        <div className="ai-chat-bubble">
          {message.content.split('\n').map((line, i) => (
            <p key={i}>{line}</p>
          ))}
        </div>
        <span className="ai-chat-time">{message.time}</span>
      </div>
    </motion.div>
  );
}

function TypingIndicator() {
  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      className="ai-chat-msg ai"
    >
      <div className="ai-chat-avatar">AI</div>
      <div className="ai-chat-bubble-wrap">
        <div className="ai-chat-bubble typing">
          <span className="ai-chat-typing-dot" />
          <span className="ai-chat-typing-dot" />
          <span className="ai-chat-typing-dot" />
        </div>
      </div>
    </motion.div>
  );
}

/**
 * Shared AI Chat component — works on both Home page and Coach page.
 * Uses AIChatContext for shared state (messages, typing, send).
 *
 * Props:
 *   variant: 'compact' (for Home sidebar) | 'full' (for Coach page)
 *   showSuggestions: boolean — show suggestion chips
 *   showStatus: boolean — show online/offline badge
 *   showClear: boolean — show clear chat button
 *   height: CSS height string — overrides default height
 */
export default function AIChat({
  variant = 'compact',
  showSuggestions = true,
  showStatus = true,
  showClear = true,
  height,
}) {
  const {
    messages, typing, aiOnline,
    sendMessage, clearChat,
    messagesEndRef, inputRef,
  } = useAIChat();

  const [input, setInput] = useState('');

  const handleSubmit = (e) => {
    e.preventDefault();
    sendMessage(input);
    setInput('');
  };

  const handleSuggestion = (text) => {
    sendMessage(text);
  };

  const aiStatusLabel = aiOnline === null ? 'CHECKING' : aiOnline ? 'ONLINE' : 'OFFLINE';
  const aiStatusClass = aiOnline === null ? '' : aiOnline ? 'online' : 'offline';

  const containerStyle = height ? { height } : {};

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      transition={{ duration: 0.3 }}
      className={`ai-chat-container ai-chat-${variant}`}
      style={containerStyle}
    >
      {/* Header */}
      <div className="ai-chat-header">
        <div className="ai-chat-header-icon">AI</div>
        <div className="ai-chat-header-info">
          <h3>FINANCIAL_COACH</h3>
          {showStatus && (
            <span className={`ai-chat-status ${aiStatusClass}`}>
              <span className={`ai-chat-status-dot ${aiStatusClass}`} />
              {aiStatusLabel}
            </span>
          )}
        </div>
        {showClear && (
          <button
            onClick={clearChat}
            className="ai-chat-clear-btn"
            title="Clear chat"
          >
            CLR
          </button>
        )}
      </div>

      {/* Messages — touch-scrollable, prevent Lenis from intercepting */}
      <div className="ai-chat-messages" data-lenis-prevent>
        {messages.map((msg, i) => (
          <ChatMessage key={i} message={msg} isUser={msg.isUser} />
        ))}
        {typing && <TypingIndicator />}
        <div ref={messagesEndRef} />
      </div>

      {/* Suggestions */}
      {showSuggestions && messages.length <= 1 && (
        <div className="ai-chat-suggestions">
          <p className="ai-chat-suggestions-label">TRY:</p>
          <div className="ai-chat-suggestion-chips">
            {SUGGESTED_QUESTIONS.map((q, i) => (
              <button
                key={i}
                className="ai-chat-suggestion-chip"
                onClick={() => handleSuggestion(q)}
              >
                {q}
              </button>
            ))}
          </div>
        </div>
      )}

      {/* Input */}
      <form onSubmit={handleSubmit} className="ai-chat-input-form">
        <input
          ref={inputRef}
          type="text"
          value={input}
          onChange={(e) => setInput(e.target.value)}
          placeholder="Ask about your finances..."
          className="ai-chat-input"
          disabled={typing}
        />
        <button
          type="submit"
          className="ai-chat-send-btn"
          disabled={!input.trim() || typing}
        >
          ↑
        </button>
      </form>
    </motion.div>
  );
}
