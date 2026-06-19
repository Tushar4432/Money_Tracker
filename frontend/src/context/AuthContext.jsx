import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { authService } from '../api/authService';
import { apiClient } from '../api/client';

export const AuthContext = createContext({
  token: null,
  user: null,
  login: async () => {},
  register: async () => {},
  logout: () => {},
  loading: true,
});

export const useAuth = () => useContext(AuthContext);

export const AuthProvider = ({ children }) => {
  const [token, setToken] = useState(null);
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // apiClient already restored the token from localStorage in its constructor.
    // If a token exists, validate it by fetching the profile.
    const existingToken = apiClient.getToken();
    if (existingToken) {
      setToken(existingToken);
      authService.getProfile()
        .then(profile => {
          setUser({ userId: profile.userId, username: profile.username, email: profile.email });
        })
        .catch(() => {
          // Token is invalid/expired — clear it
          apiClient.clearToken();
          setToken(null);
        })
        .finally(() => setLoading(false));
    } else {
      setLoading(false);
    }
  }, []);

  // Listen for 401 unauthorized events from apiClient
  useEffect(() => {
    const handler = () => {
      setToken(null);
      setUser(null);
    };
    window.addEventListener('auth:unauthorized', handler);
    return () => window.removeEventListener('auth:unauthorized', handler);
  }, []);

  const login = useCallback(async (credentials) => {
    const result = await authService.login(credentials);
    // authService.login calls apiClient.setToken which persists to localStorage
    setToken(result.token);
    setUser({ userId: result.userId, username: result.username, email: result.email });
    return result;
  }, []);

  const register = useCallback(async (data) => {
    await authService.register(data);
  }, []);

  const logout = useCallback(() => {
    authService.logout(); // calls apiClient.clearToken() which removes from localStorage
    setToken(null);
    setUser(null);
  }, []);

  return (
    <AuthContext.Provider value={{ token, user, login, register, logout, loading }}>
      {children}
    </AuthContext.Provider>
  );
};
