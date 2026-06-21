import { apiClient } from './client';

export const analyticsService = {
  getSummary(userId) {
    return apiClient.get(`/analytics/summary?userId=${encodeURIComponent(userId)}`);
  },

  getCategories(userId, { start, end } = {}) {
    let url = `/analytics/categories?userId=${encodeURIComponent(userId)}`;
    if (start) url += `&start=${encodeURIComponent(start)}`;
    if (end) url += `&end=${encodeURIComponent(end)}`;
    return apiClient.get(url);
  },

  getTrends(userId, { periods = 6, period = 'MONTHLY' } = {}) {
    return apiClient.get(
      `/analytics/trends?userId=${encodeURIComponent(userId)}&periods=${periods}&period=${period}`
    );
  },

  getComparison(userId, { period = 'MONTHLY' } = {}) {
    return apiClient.get(
      `/analytics/comparison?userId=${encodeURIComponent(userId)}&period=${period}`
    );
  },

  getBudget(userId) {
    return apiClient.get(`/analytics/budget?userId=${encodeURIComponent(userId)}`);
  },
};
