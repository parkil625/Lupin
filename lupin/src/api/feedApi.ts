import apiClient from './client';

export const feedApi = {
  getAllFeeds: async (page = 0, size = 10, excludeUserId?: number, excludeFeedId?: number) => {
    try {
      const params = new URLSearchParams({ page: String(page), size: String(size) });
      if (excludeUserId) params.append('excludeUserId', String(excludeUserId));
      if (excludeFeedId) params.append('excludeFeedId', String(excludeFeedId));
      const response = await apiClient.get(`/feeds?${params}`);
      return response.data;
    } catch {
      return { content: [], totalPages: 0, totalElements: 0 };
    }
  },

  getFeedsByUserId: async (userId: number, page = 0, size = 10) => {
    try {
      const response = await apiClient.get(`/feeds/user/${userId}?page=${page}&size=${size}`);
      return response.data;
    } catch {
      return { content: [], totalPages: 0, totalElements: 0 };
    }
  },

  getFeedById: async (feedId: number) => {
    try {
      const response = await apiClient.get(`/feeds/${feedId}`);
      return response.data;
    } catch {
      return null;
    }
  },

  createFeed: async (data: { activityType: string; duration: number; content: string; images: string[] }) => {
    try {
      const response = await apiClient.post('/feeds', data);
      return response.data;
    } catch {
      return { id: Date.now(), ...data };
    }
  },

  updateFeed: async (feedId: number, data: { content: string; activityType: string; images: string[] }) => {
    try {
      const response = await apiClient.put(`/feeds/${feedId}`, data);
      return response.data;
    } catch {
      return { id: feedId, ...data };
    }
  },

  deleteFeed: async (feedId: number) => {
    try {
      const response = await apiClient.delete(`/feeds/${feedId}`);
      return response.data;
    } catch {
      return { success: true, feedId };
    }
  },

  likeFeed: async (feedId: number) => {
    try {
      const response = await apiClient.post(`/feeds/${feedId}/like`);
      return response.data;
    } catch {
      return { success: true, feedId };
    }
  },

  unlikeFeed: async (feedId: number) => {
    try {
      const response = await apiClient.delete(`/feeds/${feedId}/like`);
      return response.data;
    } catch {
      return { success: true, feedId };
    }
  },

  canPostToday: async (userId?: number) => {
    try {
      const response = await apiClient.get('/feeds/can-post-today');
      return response.data;
    } catch {
      return true;
    }
  },
};
