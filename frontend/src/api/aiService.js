import { apiClient } from './client';

// AI chat can take a while (LLM processing), so we use a longer timeout
const AI_TIMEOUT_MS = 120000; // 2 minutes

function fetchWithTimeout(url, options, timeout) {
  return new Promise((resolve, reject) => {
    const controller = new AbortController();
    const timer = setTimeout(() => {
      controller.abort();
      reject(new Error('Request timed out. The AI is taking too long to respond.'));
    }, timeout);

    fetch(url, { ...options, signal: controller.signal })
      .then(resolve)
      .catch(reject)
      .finally(() => clearTimeout(timer));
  });
}

class AiService {
  async chat(userId, message) {
    // Use the base client's request but with a longer timeout
    const url = `http://localhost:8080/money_tracker/api/v1/ai/chat`;
    const headers = {
      'Content-Type': 'application/json',
    };
    const token = apiClient.getToken();
    if (token) {
      headers.Authorization = `Bearer ${token}`;
    }
    const body = JSON.stringify({ userId, message });

    let response;
    try {
      response = await fetchWithTimeout(url, { method: 'POST', headers, body }, AI_TIMEOUT_MS);
    } catch (networkError) {
      if (networkError.name === 'AbortError') {
        throw new Error('AI request timed out. Please try again.');
      }
      throw new Error('Cannot connect to server. Make sure the backend is running.');
    }

    if (response.status === 401) {
      apiClient.clearToken();
      window.dispatchEvent(new CustomEvent('auth:unauthorized'));
      throw new Error('Session expired. Please log in again.');
    }

    const text = await response.text();
    if (!text) throw new Error('Empty response from AI.');

    let data;
    try {
      data = JSON.parse(text);
    } catch {
      throw new Error('Invalid response from server.');
    }

    if (!response.ok) {
      const errorMsg = typeof data === 'object' ? (data.message || data.error || text) : text;
      throw new Error(errorMsg || `Request failed: ${response.status}`);
    }

    return data;
  }

  getRecommendations(userId) {
    return apiClient.get(`/ai/recommendations?userId=${encodeURIComponent(userId)}`);
  }

  getHealthScore(userId) {
    return apiClient.get(`/ai/health-score?userId=${encodeURIComponent(userId)}`);
  }

  checkAffordability(userId, itemName, cost) {
    return apiClient.post('/ai/affordability', { userId, itemName, cost });
  }

  getChatHistory(userId) {
    return apiClient.get(`/ai/history?userId=${encodeURIComponent(userId)}`);
  }
}

export const aiService = new AiService();
