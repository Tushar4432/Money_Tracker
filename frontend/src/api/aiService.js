import { apiClient } from './client';

// AI chat timeout — GPU inference is fast (~5s), but allow headroom for large prompts
const AI_TIMEOUT_MS = 30000; // 30 seconds

class AiService {
  async chat(userId, message) {
    const url = `/ai/chat`;
    const body = JSON.stringify({ userId, message });

    const controller = new AbortController();
    const timer = setTimeout(() => {
      controller.abort();
    }, AI_TIMEOUT_MS);

    try {
      const response = await apiClient.request(url, {
        method: 'POST',
        body,
        signal: controller.signal,
      });
      return response;
    } catch (err) {
      if (err.name === 'AbortError') {
        throw new Error('AI request timed out. Please try again.');
      }
      throw err;
    } finally {
      clearTimeout(timer);
    }
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
