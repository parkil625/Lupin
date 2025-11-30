import apiClient from './client';

// 백엔드 NotificationResponse를 그대로 사용 (필드명 일치: id, type, title, content, isRead, refId, createdAt)

export const notificationApi = {
  getAllNotifications: async (_userId?: number) => {
    try {
      const response = await apiClient.get('/notifications');
      return response.data || [];
    } catch {
      return [];
    }
  },

  markAsRead: async (notificationId: number, _userId?: number) => {
    try {
      const response = await apiClient.patch(`/notifications/${notificationId}/read`);
      return response.data;
    } catch {
      return { success: true };
    }
  },

  markAllAsRead: async (_userId?: number) => {
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
