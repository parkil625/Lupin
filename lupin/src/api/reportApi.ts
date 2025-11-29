import apiClient from './client';

export const reportApi = {
  // 피드 신고 토글
  reportFeed: async (feedId: number) => {
    const response = await apiClient.post(`/feeds/${feedId}/report`);
    return response.data;
  },

  // 댓글 신고 토글
  reportComment: async (commentId: number) => {
    const response = await apiClient.post(`/comments/${commentId}/report`);
    return response.data;
  },
};
