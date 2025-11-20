import apiClient from './client';
import { Comment } from '../types/dashboard.types';

/**
 * 댓글 관련 API 요청
 */
export const commentApi = {
  /**
   * 특정 피드의 최상위 댓글 조회 (페이징)
   */
  getCommentsByFeedId: async (feedId: number, page: number = 0, size: number = 20) => {
    const response = await apiClient.get(`/comments/feeds/${feedId}`, {
      params: { page, size },
    });
    return response.data;
  },

  /**
   * 특정 댓글의 답글 조회
   */
  getRepliesByCommentId: async (commentId: number) => {
    const response = await apiClient.get(`/comments/${commentId}/replies`);
    return response.data;
  },

  /**
   * 댓글 생성
   */
  createComment: async (commentData: {
    content: string;
    feedId: number;
    writerId: number;
    parentId?: number;
  }) => {
    const response = await apiClient.post(`/comments/feeds/${commentData.feedId}`, {
      content: commentData.content,
      parentId: commentData.parentId,
    }, {
      params: { userId: commentData.writerId }
    });
    return response.data;
  },

  /**
   * 댓글 수정
   */
  updateComment: async (commentId: number, content: string) => {
    const response = await apiClient.put(`/comments/${commentId}`, { content });
    return response.data;
  },

  /**
   * 댓글 삭제
   */
  deleteComment: async (commentId: number, userId: number) => {
    const response = await apiClient.delete(`/comments/${commentId}`, {
      params: { userId }
    });
    return response.data;
  },

  /**
   * 특정 피드의 댓글 수 조회
   */
  getCommentCountByFeedId: async (feedId: number) => {
    const response = await apiClient.get(`/comments/feeds/${feedId}/count`);
    return response.data;
  },
};
