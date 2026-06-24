import { apiClient } from './client';

export const goalService = {
  create(goal) {
    return apiClient.post('/goals', goal);
  },

  getAll(userId) {
    return apiClient.get(`/goals?userId=${encodeURIComponent(userId)}`);
  },

  getActive(userId) {
    return apiClient.get(`/goals/active?userId=${encodeURIComponent(userId)}`);
  },

  update(goalId, goal) {
    return apiClient.put(`/goals/${goalId}`, goal);
  },

  delete(goalId) {
    return apiClient.delete(`/goals/${goalId}`);
  },

  getProgress(goalId) {
    return apiClient.get(`/goals/${goalId}/progress`);
  },
};
