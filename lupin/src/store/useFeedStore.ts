/**
 * src/store/useFeedStore.ts
 *
 * 피드 상태 관리 Zustand 스토어
 * - 내 피드, 전체 피드 데이터
 * - 선택된 피드, 수정 중인 피드
 * - 페이지네이션 상태
 * - 피드 CRUD 액션
 */
import { create } from "zustand";
import { Feed } from "@/types/dashboard.types";
import { feedApi, FeedResponse } from "@/api";
import { getRelativeTime } from "@/lib/utils";

// [수정] FeedResponse 타입 확장
// reported: 백엔드 직렬화 이슈로 키가 'reported'로 들어올 경우를 대비해 타입에 추가
// 명시적으로 타입을 선언하여 any 사용을 제거하고 Lint 에러를 해결합니다.
type BackendFeed = FeedResponse & {
  isReported?: boolean;
  reported?: boolean;
};

interface FeedState {
  // 피드 데이터
  myFeeds: Feed[];
  allFeeds: Feed[];
  selectedFeed: Feed | null;
  editingFeed: Feed | null;

  // 페이지네이션
  feedPage: number;
  hasMoreFeeds: boolean;
  isLoadingFeeds: boolean;

  // 알림에서 피드로 이동 시 사용
  pivotFeedId: number | null;
  pivotFeed: Feed | null;
  targetCommentIdForFeed: number | null; // 피드 메뉴에서 댓글 하이라이트용

  // 데이터 새로고침 트리거
  refreshTrigger: number;

  // Actions
  setMyFeeds: (feeds: Feed[]) => void;
  setAllFeeds: (feeds: Feed[]) => void;
  setSelectedFeed: (feed: Feed | null) => void;
  setEditingFeed: (feed: Feed | null) => void;
  setPivotFeed: (
    feedId: number | null,
    feed: Feed | null,
    targetCommentId?: number | null
  ) => void;
  setTargetCommentIdForFeed: (commentId: number | null) => void;
  triggerRefresh: () => void;

  // 피드 로드 액션
  loadMyFeeds: () => Promise<void>;
  loadFeeds: (
    page: number,
    reset?: boolean,
    excludeFeedId?: number
  ) => Promise<void>;
  loadMoreFeeds: () => void;

  // 피드 CRUD 액션
  addFeed: (feed: Feed) => void;
  addFeedToAll: (feed: Feed) => void;
  updateFeed: (feedId: number, updates: Partial<Feed>) => void;
  deleteFeed: (feedId: number) => void;

  // 피드 좋아요
  toggleLike: (feedId: number, liked: boolean) => void;

  // 내 피드들의 아바타 업데이트 (프로필 사진 변경 시 호출)
  updateMyFeedsAvatar: (newAvatarUrl: string | null) => void;
}

// 백엔드 응답을 프론트엔드 Feed 타입으로 변환 (export하여 다른 곳에서도 사용 가능)
export const mapBackendFeed = (backendFeed: BackendFeed): Feed => ({
  id: backendFeed.id,
  writerId: backendFeed.writerId,
  writerName: backendFeed.writerName,
  writerAvatar: backendFeed.writerAvatar,
  writerDepartment: backendFeed.writerDepartment,
  writerActiveDays: backendFeed.writerActiveDays,
  activity: backendFeed.activity,
  points: backendFeed.points || 0,
  content: backendFeed.content,
  images: backendFeed.images || [],
  likes: backendFeed.likes || 0,
  comments: backendFeed.comments || 0,
  calories: backendFeed.calories,
  createdAt: backendFeed.createdAt,
  updatedAt: backendFeed.updatedAt,
  isLiked: backendFeed.isLiked || false,

  // [해결] 타입 정의에 reported를 추가했으므로 any 없이 안전하게 접근 가능합니다.
  // 서버가 "isReported" 또는 "reported" 중 어떤 키로 보내든 처리됩니다.
  isReported: backendFeed.isReported || backendFeed.reported || false,

  time: getRelativeTime(backendFeed.createdAt),
  author: backendFeed.writerName,
});

export const useFeedStore = create<FeedState>((set, get) => ({
  // 초기 상태
  myFeeds: [],
  allFeeds: [],
  selectedFeed: null,
  editingFeed: null,
  feedPage: 0,
  hasMoreFeeds: true,
  isLoadingFeeds: false,
  pivotFeedId: null,
  pivotFeed: null,
  targetCommentIdForFeed: null,
  refreshTrigger: 0,

  // 기본 setter
  setMyFeeds: (feeds) => set({ myFeeds: feeds }),
  setAllFeeds: (feeds) => set({ allFeeds: feeds }),
  setSelectedFeed: (feed) => set({ selectedFeed: feed }),
  setEditingFeed: (feed) => set({ editingFeed: feed }),
  setPivotFeed: (feedId, feed, targetCommentId = null) =>
    set({
      pivotFeedId: feedId,
      pivotFeed: feed,
      targetCommentIdForFeed: targetCommentId,
    }),
  setTargetCommentIdForFeed: (commentId) =>
    set({ targetCommentIdForFeed: commentId }),
  triggerRefresh: () =>
    set((state) => ({ refreshTrigger: state.refreshTrigger + 1 })),

  // 내 피드 로드
  loadMyFeeds: async () => {
    try {
      const currentUserId = parseInt(localStorage.getItem("userId") || "0");
      const response = await feedApi.getFeedsByUserId(currentUserId, 0, 100);
      if (!response) return;
      const feeds = (response.content || []).filter(
        (f): f is FeedResponse => f !== null
      );
      const mappedFeeds = feeds.map(mapBackendFeed);
      set({ myFeeds: mappedFeeds });
    } catch (error) {
      console.error("내 피드 로드 실패:", error);
    }
  },

  // 다른 사람 피드 로드 (페이지네이션)
  loadFeeds: async (page: number, reset = false, excludeFeedId?: number) => {
    const { isLoadingFeeds } = get();
    if (isLoadingFeeds) return;

    set({ isLoadingFeeds: true });
    try {
      // [수정] 한 번에 5개씩 불러오도록 변경
      const pageSize = 5;
      const currentUserId = parseInt(localStorage.getItem("userId") || "0");
      const response = await feedApi.getAllFeeds(
        page,
        pageSize,
        currentUserId,
        excludeFeedId
      );
      if (!response) return;
      const feeds = (response.content || []).filter(
        (f): f is FeedResponse => f !== null
      );
      const mappedFeeds = feeds.map(mapBackendFeed);

      if (reset) {
        set({ allFeeds: mappedFeeds.sort((a: Feed, b: Feed) => b.id - a.id) });
      } else {
        set((state) => ({
          allFeeds: [...state.allFeeds, ...mappedFeeds].sort(
            (a, b) => b.id - a.id
          ),
        }));
      }

      // [수정] 백엔드의 hasNext 값을 직접 사용하여 정확하게 다음 페이지 여부 판단
      set({
        hasMoreFeeds: response.hasNext,
        feedPage: page,
      });
    } catch (error) {
      console.error("피드 데이터 로드 실패:", error);
    } finally {
      set({ isLoadingFeeds: false });
    }
  },

  // 추가 피드 로드
  loadMoreFeeds: () => {
    const { hasMoreFeeds, isLoadingFeeds, feedPage, loadFeeds } = get();
    // 로딩 중이 아니고 더 불러올 피드가 있을 때만 실행
    if (hasMoreFeeds && !isLoadingFeeds) {
      loadFeeds(feedPage + 1);
    }
  },

  // 피드 추가 (내 피드)
  addFeed: (feed) => {
    set((state) => ({
      myFeeds: [feed, ...state.myFeeds],
    }));
  },

  // 피드 추가 (전체 피드)
  addFeedToAll: (feed) => {
    set((state) => ({
      allFeeds: [feed, ...state.allFeeds],
    }));
  },

  // 피드 업데이트
  updateFeed: (feedId, updates) => {
    set((state) => ({
      myFeeds: state.myFeeds.map((feed) =>
        feed.id === feedId ? { ...feed, ...updates } : feed
      ),
      allFeeds: state.allFeeds.map((feed) =>
        feed.id === feedId ? { ...feed, ...updates } : feed
      ),
    }));
  },

  // 피드 삭제
  deleteFeed: (feedId) => {
    set((state) => ({
      myFeeds: state.myFeeds.filter((feed) => feed.id !== feedId),
      allFeeds: state.allFeeds.filter((feed) => feed.id !== feedId),
    }));
  },

  // 좋아요 토글
  toggleLike: (feedId, liked) => {
    set((state) => ({
      allFeeds: state.allFeeds.map((feed) =>
        feed.id === feedId
          ? {
              ...feed,
              likes: liked ? feed.likes + 1 : feed.likes - 1,
              isLiked: liked,
            }
          : feed
      ),
      myFeeds: state.myFeeds.map((feed) =>
        feed.id === feedId
          ? {
              ...feed,
              likes: liked ? feed.likes + 1 : feed.likes - 1,
              isLiked: liked,
            }
          : feed
      ),
    }));
  },

  // 내 피드들의 아바타 업데이트 (프로필 사진 변경 시 호출)
  updateMyFeedsAvatar: (newAvatarUrl) => {
    const currentUserId = parseInt(localStorage.getItem("userId") || "0");
    set((state) => ({
      myFeeds: state.myFeeds.map((feed) => ({
        ...feed,
        writerAvatar: newAvatarUrl || undefined,
      })),
      // allFeeds에서 내 피드도 업데이트
      allFeeds: state.allFeeds.map((feed) =>
        feed.writerId === currentUserId
          ? { ...feed, writerAvatar: newAvatarUrl || undefined }
          : feed
      ),
      // 선택된 피드가 내 피드면 업데이트
      selectedFeed:
        state.selectedFeed && state.selectedFeed.writerId === currentUserId
          ? { ...state.selectedFeed, writerAvatar: newAvatarUrl || undefined }
          : state.selectedFeed,
    }));
  },
}));
