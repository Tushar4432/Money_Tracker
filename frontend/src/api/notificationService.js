import { apiClient } from './client';

export const notificationService = {
  getAll(userId) {
    return apiClient.get(`/notifications?userId=${encodeURIComponent(userId)}`);
  },

  getUnread(userId) {
    return apiClient.get(`/notifications/unread?userId=${encodeURIComponent(userId)}`);
  },

  getUnreadCount(userId) {
    return apiClient.get(`/notifications/unread/count?userId=${encodeURIComponent(userId)}`);
  },

  markAsRead(notificationId) {
    return apiClient.put(`/notifications/${notificationId}/read`);
  },

  markAllAsRead(userId) {
    return apiClient.put(`/notifications/read-all?userId=${encodeURIComponent(userId)}`);
  },

  delete(notificationId) {
    return apiClient.delete(`/notifications/${notificationId}`);
  },
};
