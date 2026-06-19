// Base API client with JWT auth support + persistent token storage
const BASE_URL = import.meta.env.PROD
  ? '/money_tracker/api/v1'
  : 'http://localhost:8080/money_tracker/api/v1';
const TOKEN_KEY = 'mt_jwt_token';

class ApiClient {
  #token = null;

  constructor() {
    try {
      const saved = localStorage.getItem(TOKEN_KEY);
      if (saved) this.#token = saved;
    } catch {
      // localStorage unavailable
    }
  }

  setToken(token) {
    this.#token = token;
    try {
      if (token) {
        localStorage.setItem(TOKEN_KEY, token);
      } else {
        localStorage.removeItem(TOKEN_KEY);
      }
    } catch {
      // localStorage unavailable
    }
  }

  getToken() {
    return this.#token;
  }

  clearToken() {
    this.#token = null;
    try {
      localStorage.removeItem(TOKEN_KEY);
    } catch {
      // localStorage unavailable
    }
  }

  async request(endpoint, options = {}) {
    const url = `${BASE_URL}${endpoint}`;
    const headers = { ...(options.headers || {}) };

    if (this.#token) {
      headers.Authorization = `Bearer ${this.#token}`;
    }

    if (!(options.body instanceof FormData)) {
      headers['Content-Type'] = headers['Content-Type'] || 'application/json';
    }

    const config = { ...options, headers };

    let response;
    try {
      response = await fetch(url, config);
    } catch (networkError) {
      throw new Error('Cannot connect to server. Make sure the backend is running on http://localhost:8080');
    }

    if (response.status === 401) {
      this.clearToken();
      window.dispatchEvent(new CustomEvent('auth:unauthorized'));
      throw new Error('Session expired. Please log in again.');
    }

    const text = await response.text();
    if (!text) return null;

    let data;
    try {
      data = JSON.parse(text);
    } catch {
      data = text;
    }

    if (!response.ok) {
      let errorMsg;
      if (typeof data === 'object' && data !== null) {
        errorMsg = data.message || data.error || text;
      } else {
        errorMsg = text;
      }
      throw new Error(errorMsg || `Request failed: ${response.status}`);
    }

    return data;
  }

  get(endpoint) {
    return this.request(endpoint, { method: 'GET' });
  }

  post(endpoint, body) {
    return this.request(endpoint, {
      method: 'POST',
      body: body instanceof FormData ? body : JSON.stringify(body),
    });
  }

  put(endpoint, body) {
    return this.request(endpoint, {
      method: 'PUT',
      body: JSON.stringify(body),
    });
  }

  delete(endpoint) {
    return this.request(endpoint, { method: 'DELETE' });
  }
}

export const apiClient = new ApiClient();
