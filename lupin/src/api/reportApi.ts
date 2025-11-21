import apiClient from './client';

/**
 * 신고 관련 API 요청
 */
export const reportApi = {
  /**
   * 피드 신고
   */
  reportFeed: async (feedId: number, userId: number) => {
    const response = await apiClient.post(`/reports/feeds/${feedId}`, null, {
      params: { userId }
    });
    return response.data;
  },

  /**
   * 댓글 신고
   */
  reportComment: async (commentId: number, userId: number) => {
    const response = await apiClient.post(`/reports/comments/${commentId}`, null, {
      params: { userId }
    });
    return response.data;
  },

  /**
   * 피드 신고 여부 확인
   */
  checkFeedReportStatus: async (feedId: number, userId: number) => {
    const response = await apiClient.get(`/reports/feeds/${feedId}/status`, {
      params: { userId }
    });
    return response.data;
  },

  /**
   * 댓글 신고 여부 확인
   */
  checkCommentReportStatus: async (commentId: number, userId: number) => {
    const response = await apiClient.get(`/reports/comments/${commentId}/status`, {
      params: { userId }
    });
    return response.data;
  },
};
