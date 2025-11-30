import apiClient from './client';

export const feedApi = {
  getAllFeeds: async (page = 0, size = 10, _excludeUserId?: number, _excludeFeedId?: number) => {
    try {
      // 백엔드는 인증된 사용자 기준으로 자동으로 본인 피드를 제외함
      const params = new URLSearchParams({ page: String(page), size: String(size) });
      const response = await apiClient.get(`/feeds?${params}`);
      return response.data;
    } catch {
      return { content: [], totalPages: 0, totalElements: 0 };
    }
  },

  getFeedsByUserId: async (_userId: number, page = 0, size = 10) => {
    try {
      // 현재 로그인한 사용자의 피드를 가져옴 (백엔드는 JWT 토큰에서 사용자 식별)
      const response = await apiClient.get(`/feeds/my?page=${page}&size=${size}`);
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

  canPostToday: async (_userId?: number) => {
    try {
      // 백엔드는 JWT 토큰에서 사용자 식별
      const response = await apiClient.get('/feeds/can-post-today');
      return response.data;
    } catch {
      return true;
    }
  },
};
