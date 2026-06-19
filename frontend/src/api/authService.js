import { apiClient } from './client';

class AuthService {
  async login({ username, password }) {
    const response = await apiClient.post('/auth/login', { username, password });
    apiClient.setToken(response.token);
    return response;
  }

  async register({ username, password, email }) {
    await apiClient.post('/auth/register', { username, password, email });
  }

  async getProfile() {
    return apiClient.get('/auth/me');
  }

  /**
   * Authenticated health check.
   * Returns { status: "ok", userId, username } if backend is reachable and token is valid.
   * Returns { status: "unauthenticated" } if token is expired.
   * Throws if backend is unreachable.
   */
  async healthCheck() {
    return apiClient.get('/auth/health');
  }

  logout() {
    apiClient.clearToken();
  }
}

export const authService = new AuthService();
