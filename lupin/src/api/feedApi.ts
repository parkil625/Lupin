import apiClient from './client';
import { Feed } from '../types/dashboard.types';

/**
 * 백엔드 API 응답을 프론트엔드 Feed 타입으로 변환
 */
const mapBackendFeedToFrontend = (backendFeed: any): Feed => {
  // statsJson을 파싱
  let stats = {};
  try {
    stats = backendFeed.statsJson ? JSON.parse(backendFeed.statsJson) : {};
  } catch (e) {
    console.error('Failed to parse statsJson:', e);
  }

  // duration 기반으로 points 계산 (5분당 5점, 최대 30점)
  const points = Math.min(Math.floor(backendFeed.duration / 5) * 5, 30);

  return {
    id: backendFeed.id,
    author: backendFeed.authorName,
    avatar: '', // 백엔드에 없으면 기본값
    activity: backendFeed.activityType,
    duration: `${backendFeed.duration}분`,
    points: points,
    content: backendFeed.content,
    images: backendFeed.images || [],
    likes: backendFeed.likesCount || 0,
    comments: backendFeed.commentsCount || 0,
    time: new Date(backendFeed.createdAt).toLocaleString('ko-KR', {
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    }),
    stats: stats,
    likedBy: [],
  };
};

/**
 * 피드 관련 API 요청
 */
export const feedApi = {
  /**
   * 모든 피드 조회 (페이징)
   */
  getAllFeeds: async (page: number = 0, size: number = 20) => {
    const response = await apiClient.get('/feeds', {
      params: { page, size },
    });

    // Page 객체인 경우 content 배열을 매핑
    if (response.data.content) {
      return {
        ...response.data,
        content: response.data.content.map(mapBackendFeedToFrontend),
      };
    }

    // 배열인 경우 직접 매핑
    if (Array.isArray(response.data)) {
      return response.data.map(mapBackendFeedToFrontend);
    }

    return response.data;
  },

  /**
   * 특정 피드 조회
   */
  getFeedById: async (feedId: number) => {
    const response = await apiClient.get(`/feeds/${feedId}`);
    return response.data;
  },

  /**
   * 피드 생성
   */
  createFeed: async (feedData: Partial<Feed>) => {
    const response = await apiClient.post('/feeds', feedData);
    return response.data;
  },

  /**
   * 피드 수정
   */
  updateFeed: async (feedId: number, feedData: Partial<Feed>) => {
    const response = await apiClient.put(`/feeds/${feedId}`, feedData);
    return response.data;
  },

  /**
   * 피드 삭제
   */
  deleteFeed: async (feedId: number) => {
    const response = await apiClient.delete(`/feeds/${feedId}`);
    return response.data;
  },

  /**
   * 특정 사용자의 피드 조회
   */
  getFeedsByUserId: async (userId: number, page: number = 0, size: number = 20) => {
    const response = await apiClient.get(`/feeds/users/${userId}`, {
      params: { page, size },
    });
    return response.data;
  },

  /**
   * 피드 좋아요
   */
  likeFeed: async (feedId: number, userId: number) => {
    const response = await apiClient.post(`/feeds/${feedId}/like`, { userId });
    return response.data;
  },

  /**
   * 피드 좋아요 취소
   */
  unlikeFeed: async (feedId: number, userId: number) => {
    const response = await apiClient.delete(`/feeds/${feedId}/like`, {
      data: { userId },
    });
    return response.data;
  },

  /**
   * 오늘 피드 작성 가능 여부 확인
   */
  canPostToday: async (userId: number): Promise<boolean> => {
    const response = await apiClient.get('/feeds/can-post', {
      params: { userId },
    });
    return response.data;
  },
};
