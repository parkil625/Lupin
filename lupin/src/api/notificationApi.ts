import apiClient from './client';

export const notificationApi = {
  getAllNotifications: async (userId: number) => {
    const response = await apiClient.get(`/notifications?userId=${userId}`);
    return response.data;
  },

  markAsRead: async (notificationId: number, userId: number) => {
    const response = await apiClient.put(`/notifications/${notificationId}/read?userId=${userId}`);
    return response.data;
  },

  markAllAsRead: async (userId: number) => {
    const response = await apiClient.put(`/notifications/read-all?userId=${userId}`);
    return response.data;
  },

  deleteNotification: async (notificationId: number) => {
    const response = await apiClient.delete(`/notifications/${notificationId}`);
    return response.data;
  },
};
