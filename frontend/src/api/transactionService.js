import { apiClient } from './client';

export const transactionService = {
  upload(file, userId) {
    const formData = new FormData();
    formData.append('bank_statements', file);
    formData.append('userId', userId);
    return apiClient.post('/transaction/upload', formData);
  },

  getAll(userId) {
    return apiClient.get(`/transaction?userId=${encodeURIComponent(userId)}`);
  },

  getSummary(userId) {
    return apiClient.get(`/transaction/summary?userId=${encodeURIComponent(userId)}`);
  },
};
