import apiClient from "./client";
import { getS3Url } from "@/lib/utils";

// Feed 응답 타입 (백엔드 API 응답과 일치)
export interface FeedResponse {
  id: number;
  writerId: number;
  writerName: string;
  writerAvatar?: string;
  writerDepartment?: string;
  writerActiveDays?: number;
  activity: string;
  points?: number;
  content: string;
  images: string[];
  imageCapturedAt?: string[]; // [추가] 이미지 촬영 시간
  likes?: number;
  comments?: number;
  calories?: number;
  createdAt: string;
  updatedAt?: string;
  isLiked?: boolean;
}

// [수정] 페이지네이션(Slice) 응답 타입 - 백엔드 SliceResponse와 일치시킴
export interface FeedSliceResponse {
  content: FeedResponse[];
  hasNext: boolean;
  page: number;
  size: number;
}

// Feed 응답에서 images를 S3 URL로 변환
const transformFeedImages = (feed: FeedResponse | null) => {
  if (!feed) return feed;
  return {
    ...feed,
    images: (feed.images || []).map(getS3Url),
    writerAvatar: feed.writerAvatar
      ? getS3Url(feed.writerAvatar)
      : feed.writerAvatar,
  };
};

// [수정] Slice 응답 변환 함수
const transformSliceFeeds = (response: FeedSliceResponse | null) => {
  if (!response || !response.content) return response;
  return {
    ...response,
    content: response.content.map(transformFeedImages),
  };
};

export const feedApi = {
  getAllFeeds: async (
    page = 0,
    size = 10,
    _excludeUserId?: number,
    _excludeFeedId?: number,
    search?: string // [추가] 검색어 파라미터
  ) => {
    try {
      // 백엔드는 인증된 사용자 기준으로 자동으로 본인 피드를 제외함
      const params = new URLSearchParams({
        page: String(page),
        size: String(size),
      });

      // [추가] 검색어가 있으면 파라미터에 추가
      if (search) {
        params.append("search", search);
      }

      const response = await apiClient.get(`/feeds?${params}`);
      return transformSliceFeeds(response.data);
    } catch {
      // [수정] 에러 시 빈 Slice 반환
      return { content: [], hasNext: false, page: 0, size: 10 };
    }
  },

  getFeedsByUserId: async (_userId: number, page = 0, size = 10) => {
    try {
      const response = await apiClient.get(
        `/feeds/my?page=${page}&size=${size}`
      );
      return transformSliceFeeds(response.data);
    } catch {
      // [수정] 에러 시 빈 Slice 반환
      return { content: [], hasNext: false, page: 0, size: 10 };
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
    // [추가] 시간 필드
    startAt?: string;
    endAt?: string;
  }): Promise<FeedResponse> => {
    const response = await apiClient.post("/feeds", data);
    return transformFeedImages(response.data) as FeedResponse;
  },

  updateFeed: async (
    feedId: number,
    data: {
      activity: string;
      content: string;
      startImage?: string;
      endImage?: string;
      otherImages?: string[];
      // [추가] 변경 여부와 시간 정보 필드 추가
      imagesChanged?: boolean;
      startAt?: string;
      endAt?: string;
    }
  ) => {
    // [디버깅 로그] 요청 데이터 확인
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
      const response = await apiClient.get("/feeds/can-post-today");
      return response.data;
    } catch {
      return true;
    }
  },

  getFeedLikeById: async (
    feedLikeId: number
  ): Promise<{ feedId: number } | null> => {
    try {
      const response = await apiClient.get(`/feeds/likes/${feedLikeId}`);
      return response.data;
    } catch {
      return null;
    }
  },
};
