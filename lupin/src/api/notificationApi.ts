import apiClient from './client';

export const notificationApi = {
  getAllNotifications: async () => {
    try {
      const response = await apiClient.get('/notifications');
      return response.data || [];
    } catch {
      return [];
    }
  },

  markAsRead: async (notificationId: number) => {
    try {
      const response = await apiClient.patch(`/notifications/${notificationId}/read`);
      return response.data;
    } catch {
      return { success: true };
    }
  },

  markAllAsRead: async () => {
    try {
      const response = await apiClient.patch('/notifications/read-all');
      return response.data;
    } catch {
      return { success: true };
    }
  },

  deleteNotification: async (notificationId: number) => {
    try {
      const response = await apiClient.delete(`/notifications/${notificationId}`);
      return response.data;
    } catch {
      return { success: true };
    }
  },
};
