import apiClient from './client';

export const commentApi = {
  getCommentById: async (commentId: number) => {
    try {
      const response = await apiClient.get(`/comments/${commentId}`);
      return response.data;
    } catch {
      return null;
    }
  },

  getComments: async (feedId: number) => {
    try {
      const response = await apiClient.get(`/feeds/${feedId}/comments`);
      return response.data;
    } catch {
      return [];
    }
  },

  getCommentsByFeedId: async (feedId: number, page = 0, size = 100) => {
    try {
      const response = await apiClient.get(`/feeds/${feedId}/comments?page=${page}&size=${size}`);
      return response.data;
    } catch {
      return { content: [], totalElements: 0, totalPages: 0 };
    }
  },

  getRepliesByCommentId: async (commentId: number) => {
    try {
      const response = await apiClient.get(`/comments/${commentId}/replies`);
      return response.data;
    } catch {
      return [];
    }
  },

  createComment: async (data: { content: string; feedId: number; writerId: number; parentId?: number }) => {
    try {
      const response = await apiClient.post(`/feeds/${data.feedId}/comments`, data);
      return response.data;
    } catch {
      return { id: Date.now(), ...data, writerName: '사용자', createdAt: new Date().toISOString() };
    }
  },

  updateComment: async (commentId: number, content: string) => {
    try {
      const response = await apiClient.put(`/comments/${commentId}`, { content });
      return response.data;
    } catch {
      return { id: commentId, content };
    }
  },

  deleteComment: async (commentId: number) => {
    try {
      const response = await apiClient.delete(`/comments/${commentId}`);
      return response.data;
    } catch {
      return { success: true };
    }
  },

  likeComment: async (commentId: number) => {
    const response = await apiClient.post(`/comments/${commentId}/like`);
    return response.data;
  },

  unlikeComment: async (commentId: number) => {
    const response = await apiClient.delete(`/comments/${commentId}/like`);
    return response.data;
  },

  reportComment: async (commentId: number) => {
    try {
      const response = await apiClient.post(`/comments/${commentId}/report`);
      return response.data;
    } catch {
      return { success: true };
    }
  },
};
