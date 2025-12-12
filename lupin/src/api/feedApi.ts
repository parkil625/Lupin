import apiClient from './client';
import { getS3Url } from '@/lib/utils';

// Feed 응답 타입 (백엔드 API 응답과 일치)
export interface FeedResponse {
  id: number;
  writerId: number;
  writerName: string;
  writerAvatar?: string;
  activity: string;
  points?: number;
  content: string;
  images: string[];
  likes?: number;
  comments?: number;
  calories?: number;
  createdAt: string;
  updatedAt?: string;
  isLiked?: boolean;
}

// 페이지네이션 응답 타입
export interface PagedFeedResponse {
  content: FeedResponse[];
  totalPages: number;
  totalElements: number;
}

// Feed 응답에서 images를 S3 URL로 변환
const transformFeedImages = (feed: FeedResponse | null) => {
  if (!feed) return feed;
  return {
    ...feed,
    images: (feed.images || []).map(getS3Url),
    writerAvatar: feed.writerAvatar ? getS3Url(feed.writerAvatar) : feed.writerAvatar,
  };
};

// 페이지네이션 응답에서 모든 피드의 images를 변환
const transformPagedFeeds = (response: PagedFeedResponse | null) => {
  if (!response || !response.content) return response;
  return {
    ...response,
    content: response.content.map(transformFeedImages),
  };
};

export const feedApi = {
  getAllFeeds: async (page = 0, size = 10, _excludeUserId?: number, _excludeFeedId?: number) => {
    try {
      // 백엔드는 인증된 사용자 기준으로 자동으로 본인 피드를 제외함
      const params = new URLSearchParams({ page: String(page), size: String(size) });
      const response = await apiClient.get(`/feeds?${params}`);
      return transformPagedFeeds(response.data);
    } catch {
      return { content: [], totalPages: 0, totalElements: 0 };
    }
  },

  getFeedsByUserId: async (_userId: number, page = 0, size = 10) => {
    try {
      // 현재 로그인한 사용자의 피드를 가져옴 (백엔드는 JWT 토큰에서 사용자 식별)
      const response = await apiClient.get(`/feeds/my?page=${page}&size=${size}`);
      return transformPagedFeeds(response.data);
    } catch {
      return { content: [], totalPages: 0, totalElements: 0 };
    }
  },

  getFeedById: async (feedId: number) => {
    try {
      const response = await apiClient.get(`/feeds/${feedId}`);
      return transformFeedImages(response.data);
    } catch {
      return null;
    }
  },

  createFeed: async (data: {
    activity: string;
    content: string;
    startImage: string;
    endImage: string;
    otherImages?: string[];
  }): Promise<FeedResponse> => {
    const response = await apiClient.post('/feeds', data);
    return transformFeedImages(response.data) as FeedResponse;
  },

  updateFeed: async (feedId: number, data: {
    activity: string;
    content: string;
    startImage?: string;
    endImage?: string;
    otherImages?: string[];
  }) => {
    const response = await apiClient.put(`/feeds/${feedId}`, data);
    return response.data;
  },

  deleteFeed: async (feedId: number) => {
    const response = await apiClient.delete(`/feeds/${feedId}`);
    return response.data;
  },

  likeFeed: async (feedId: number) => {
    const response = await apiClient.post(`/feeds/${feedId}/like`);
    return response.data;
  },

  unlikeFeed: async (feedId: number) => {
    const response = await apiClient.delete(`/feeds/${feedId}/like`);
    return response.data;
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

  getFeedLikeById: async (feedLikeId: number): Promise<{ feedId: number } | null> => {
    try {
      const response = await apiClient.get(`/feeds/likes/${feedLikeId}`);
      return response.data;
    } catch {
      return null;
    }
  },
};
