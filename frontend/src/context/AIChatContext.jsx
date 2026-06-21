import React, { createContext, useContext, useState, useCallback, useEffect, useRef } from 'react';
import { aiService } from '../api/aiService';

const AIChatContext = createContext(null);

const STORAGE_KEY = 'mt_ai_chat_messages';

function loadMessages() {
  try {
    const stored = localStorage.getItem(STORAGE_KEY);
    if (stored) {
      const parsed = JSON.parse(stored);
      if (Array.isArray(parsed) && parsed.length > 0) return parsed;
    }
  } catch { /* ignore */ }
  return null;
}

function persistMessages(messages) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(messages));
  } catch { /* ignore */ }
}

function clearStorage() {
  try {
    localStorage.removeItem(STORAGE_KEY);
  } catch { /* ignore */ }
}

const WELCOME_MSG = {
  content: "I'm your AI Financial Coach. Upload a bank statement to get started, or ask me anything about your spending habits.",
  isUser: false,
  time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
};

export function AIChatProvider({ children, userId }) {
  const [messages, setMessages] = useState(() => loadMessages() || [WELCOME_MSG]);
  const [typing, setTyping] = useState(false);
  const [aiOnline, setAiOnline] = useState(null);
  const messagesEndRef = useRef(null);
  const inputRef = useRef(null);

  // Persist to localStorage on every change
  useEffect(() => {
    persistMessages(messages);
  }, [messages]);

  // Scroll to bottom on new messages
  const scrollToBottom = useCallback(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, []);

  useEffect(() => { scrollToBottom(); }, [messages, scrollToBottom]);

  // Check AI health periodically
  useEffect(() => {
    if (!userId) return;
    const check = async () => {
      try {
        await aiService.getChatHistory(userId);
        setAiOnline(true);
      } catch (err) {
        const msg = err?.message || '';
        if (!msg.includes('Session expired') && !msg.includes('Not authenticated')) {
          setAiOnline(false);
        }
      }
    };
    check();
    const interval = setInterval(check, 30000);
    return () => clearInterval(interval);
  }, [userId]);

  const sendMessage = useCallback(async (text) => {
    if (!text?.trim() || typing) return;

    const userMsg = {
      content: text.trim(),
      isUser: true,
      time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
    };
    setMessages(prev => [...prev, userMsg]);
    setTyping(true);

    try {
      const response = await aiService.chat(userId, text.trim());
      const aiMsg = {
        content: response.reply || "I'm analyzing your request. Please try again.",
        isUser: false,
        time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      };
      setMessages(prev => [...prev, aiMsg]);
      if (!aiOnline) setAiOnline(true);
    } catch (err) {
      const errMsg = {
        content: err.message || "I'm having trouble connecting right now. Please try again in a moment.",
        isUser: false,
        time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      };
      setMessages(prev => [...prev, errMsg]);
      if (aiOnline) setAiOnline(false);
    } finally {
      setTyping(false);
      // Don't auto-focus on mobile — it causes keyboard popups
    }
  }, [userId, typing, aiOnline]);

  const clearChat = useCallback(() => {
    const welcome = [{
      content: "Chat cleared. How can I help?",
      isUser: false,
      time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
    }];
    setMessages(welcome);
    clearStorage();
  }, []);

  const value = {
    messages,
    typing,
    aiOnline,
    sendMessage,
    clearChat,
    messagesEndRef,
    inputRef,
  };

  return (
    <AIChatContext.Provider value={value}>
      {children}
    </AIChatContext.Provider>
  );
}

export function useAIChat() {
  const ctx = useContext(AIChatContext);
  if (!ctx) throw new Error('useAIChat must be used within AIChatProvider');
  return ctx;
}
