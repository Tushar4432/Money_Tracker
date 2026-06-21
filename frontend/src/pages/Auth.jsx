import React, { useState, useEffect, useCallback } from 'react';
import { motion } from 'framer-motion';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import './Auth.css';

// ─── DEV CREDENTIALS (remove later) ───
// Press Ctrl+Shift+D on the auth page to auto-login as dev user
const DEV_USERNAME = 'dev';
const DEV_PASSWORD = 'devpass';

const Auth = () => {
  const navigate = useNavigate();
  const { login, register, token, user, logout } = useAuth();
  const [mode, setMode] = useState('login');
  const [form, setForm] = useState({ username: '', password: '', email: '' });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [showPassword, setShowPassword] = useState(false);

  // Hidden dev shortcut: Ctrl+Shift+D
  const handleDevLogin = useCallback(async (e) => {
    if (e.ctrlKey && e.shiftKey && e.key === 'D') {
      e.preventDefault();
      setError('');
      setLoading(true);
      try {
        await login({ username: DEV_USERNAME, password: DEV_PASSWORD });
        navigate('/');
      } catch (err) {
        setError('Dev login failed — check backend has dev user seeded.');
      } finally {
        setLoading(false);
      }
    }
  }, [login, navigate]);

  useEffect(() => {
    window.addEventListener('keydown', handleDevLogin);
    return () => window.removeEventListener('keydown', handleDevLogin);
  }, [handleDevLogin]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setForm((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      if (mode === 'login') {
        await login({ username: form.username, password: form.password });
        navigate('/');
      } else {
        await register({ username: form.username, password: form.password, email: form.email });
        setMode('login');
        setForm({ username: '', password: '', email: '' });
        setShowPassword(false);
      }
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleLogout = () => {
    logout();
  };

  const togglePassword = () => setShowPassword((prev) => !prev);

  // If logged in, show profile card
  if (token && user) {
    return (
      <div className="auth-page">
        <motion.div
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.3 }}
          className="auth-card profile-card"
        >
          <div className="profile-avatar">
            {user.username.charAt(0).toUpperCase()}
          </div>
          <h2 className="profile-name">{user.username}</h2>
          <p className="profile-email">{user.email}</p>
          <div className="profile-badge">
            <span className="badge badge-success">● SESSION ACTIVE</span>
          </div>
          <div className="profile-actions">
            <button onClick={() => navigate('/')} className="btn btn-primary btn-full">
              ▸ DASHBOARD
            </button>
            <button onClick={handleLogout} className="btn btn-danger btn-full">
              ◂ SIGN OUT
            </button>
          </div>
        </motion.div>
      </div>
    );
  }

  return (
    <div className="auth-page">
      <motion.div
        initial={{ opacity: 0, y: 12 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.3 }}
        className="auth-card"
      >
        {/* Header */}
        <div className="auth-header">
          <div className="auth-system-text">MONEYTRACKER // AUTH</div>
          <h2 className="auth-title">
            {mode === 'login' ? '> AUTHENTICATE' : '> NEW_IDENTITY'}
          </h2>
          <p className="auth-subtitle">
            {mode === 'login'
              ? 'Enter credentials to access terminal'
              : 'Create a new identity in the system'}
          </p>
        </div>

        {error && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            className="auth-error"
          >
            <span>!</span> {error}
          </motion.div>
        )}

        <form onSubmit={handleSubmit} className="auth-form">
          <div className="input-group">
            <label htmlFor="username" className="input-label">USER_ID</label>
            <div className="input-wrapper">
              <span className="input-icon">&gt;</span>
              <input
                id="username"
                type="text"
                name="username"
                placeholder="username"
                value={form.username}
                onChange={handleChange}
                required
                className="auth-input"
                autoComplete="username"
              />
            </div>
          </div>

          {mode === 'register' && (
            <motion.div
              initial={{ opacity: 0, height: 0 }}
              animate={{ opacity: 1, height: 'auto' }}
              exit={{ opacity: 0, height: 0 }}
              className="input-group"
            >
              <label htmlFor="email" className="input-label">EMAIL</label>
              <div className="input-wrapper">
                <span className="input-icon">@</span>
                <input
                  id="email"
                  type="email"
                  name="email"
                  placeholder="user@domain.com"
                  value={form.email}
                  onChange={handleChange}
                  required
                  className="auth-input"
                  autoComplete="email"
                />
              </div>
            </motion.div>
          )}

          <div className="input-group">
            <label htmlFor="password" className="input-label">PASS_KEY</label>
            <div className="input-wrapper">
              <span className="input-icon">#</span>
              <input
                id="password"
                type={showPassword ? 'text' : 'password'}
                name="password"
                placeholder="••••••••"
                value={form.password}
                onChange={handleChange}
                required
                className="auth-input auth-input-password"
                autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
              />
              <button
                type="button"
                className="password-toggle"
                onClick={togglePassword}
                tabIndex={-1}
                aria-label={showPassword ? 'Hide password' : 'Show password'}
              >
                {showPassword ? (
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94" />
                    <path d="M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19" />
                    <path d="M14.12 14.12a3 3 0 1 1-4.24-4.24" />
                    <line x1="1" y1="1" x2="23" y2="23" />
                  </svg>
                ) : (
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                    <circle cx="12" cy="12" r="3" />
                  </svg>
                )}
              </button>
            </div>
          </div>

          <button
            type="submit"
            className="btn btn-primary btn-full"
            disabled={loading}
          >
            {loading ? (
              <span className="btn-loading">
                <span className="spinner" />
                {mode === 'login' ? 'VERIFYING...' : 'CREATING...'}
              </span>
            ) : (
              mode === 'login' ? '> AUTHENTICATE' : '> CREATE_IDENTITY'
            )}
          </button>
        </form>

        <div className="auth-switch">
          {mode === 'login' ? (
            <p>
              NO_ACCOUNT?{' '}
              <button
                onClick={() => { setMode('register'); setError(''); setShowPassword(false); }}
                className="link-button"
              >
                CREATE_NEW →
              </button>
            </p>
          ) : (
            <p>
              HAS_ACCOUNT?{' '}
              <button
                onClick={() => { setMode('login'); setError(''); setShowPassword(false); }}
                className="link-button"
              >
                ← AUTHENTICATE
              </button>
            </p>
          )}
        </div>
      </motion.div>
    </div>
  );
};

export default Auth;
