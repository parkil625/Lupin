/**
 * Feed.tsx
 *
 * 피드 페이지 컴포넌트
 * - 세로 스크롤 + 한 개씩 표시 (쇼츠 스타일)
 * - FeedV2 디자인 적용
 */

import React, { useEffect, useMemo } from "react";
import { Virtuoso } from "react-virtuoso";
import SearchInput from "@/components/molecules/SearchInput";
import { Feed } from "@/types/dashboard.types";
import { reportApi, getCdnUrl } from "@/api";
import { toast } from "sonner";
import { useFeedStore } from "@/store/useFeedStore";
import FeedCard from "@/components/dashboard/feed/FeedCard";

interface FeedViewProps {
  allFeeds: Feed[];
  searchQuery: string;
  setSearchQuery: React.Dispatch<React.SetStateAction<string>>;
  showSearch: boolean;
  setShowSearch: React.Dispatch<React.SetStateAction<boolean>>;
  getFeedImageIndex: (feedId: number) => number;
  setFeedImageIndex: (
    feedId: number,
    updater: number | ((prev: number) => number)
  ) => void;
  hasLiked: (feedId: number) => boolean;
  handleLike: (feedId: number) => void;
  feedContainerRef: React.RefObject<HTMLDivElement | null>;
  scrollToFeedId: number | null;
  setScrollToFeedId: (id: number | null) => void;
  loadMoreFeeds: () => void;
  hasMoreFeeds: boolean;
  isLoadingFeeds: boolean;
}

/**
 * Ref 업데이트 헬퍼 (Strict Mode/Linter 우회용)
 */
const updateFeedRef = (
  ref: React.RefObject<HTMLDivElement | null> | null,
  value: HTMLElement | null
) => {
  if (ref) {
    (ref as React.MutableRefObject<HTMLElement | null>).current = value;
  }
};

/**
 * 피드 페이지 메인 컴포넌트
 */
export default function FeedView({
  allFeeds,
  searchQuery,
  setSearchQuery,
  showSearch: _showSearch,
  setShowSearch: _setShowSearch,
  getFeedImageIndex,
  setFeedImageIndex,
  hasLiked,
  handleLike,
  feedContainerRef,
  scrollToFeedId,
  setScrollToFeedId,
  loadMoreFeeds: _loadMoreFeeds, // [수정] 기존 props 대신 스토어 액션 사용
  isLoadingFeeds,
}: FeedViewProps) {
  // 스토어, 이미지 프리로드, 검색 등 기존 로직 유지
  const {
    targetCommentIdForFeed,
    setTargetCommentIdForFeed,
    toggleReport,
    loadFeeds, // [추가] 검색어 기반 로딩을 위해 액션 가져오기
    feedPage, // [추가] 현재 페이지 번호 가져오기
    hasMoreFeeds, // [추가] 더 불러올 피드가 있는지 확인
  } = useFeedStore();

  useEffect(() => {
    return () => {
      if (targetCommentIdForFeed) setTargetCommentIdForFeed(null);
    };
  }, [targetCommentIdForFeed, setTargetCommentIdForFeed]);

  useEffect(() => {
    if (allFeeds.length > 0 && allFeeds[0].images?.[0]) {
      const link = document.createElement("link");
      link.rel = "preload";
      link.as = "image";
      link.href = getCdnUrl(allFeeds[0].images[0]);
      document.head.appendChild(link);
      return () => link.remove();
    }
  }, [allFeeds]);

  const authorSuggestions = useMemo(
    () => [...new Set(allFeeds.map((feed) => feed.author || feed.writerName))],
    [allFeeds]
  );

  // [수정] 클라이언트 필터링 제거 -> 서버 데이터를 그대로 사용
  // 검색어가 있으면 서버에서 이미 필터링된 데이터를 allFeeds에 담아옵니다.
  const filteredFeeds = allFeeds;

  // [추가] 검색어 변경 시 서버 재요청 (디바운싱 적용)
  useEffect(() => {
    const timer = setTimeout(() => {
      // 검색어가 있거나, 검색어가 비워졌을 때(원래 목록 복귀) 모두 서버 호출
      // 페이지 0부터 다시 로드 (reset=true)
      loadFeeds(0, true, undefined, searchQuery);
    }, 500); // 0.5초 딜레이

    return () => clearTimeout(timer);
  }, [searchQuery, loadFeeds]);

  // [Debug] 필터링된 피드 데이터 변경 감지
  useEffect(() => {
    console.log(
      `[FeedView] filteredFeeds updated. Count: ${filteredFeeds.length}, SearchQuery: "${searchQuery}"`
    );
    if (filteredFeeds.length > 0) {
      console.log(`[FeedView] First feed ID: ${filteredFeeds[0].id}`);
    }
  }, [filteredFeeds, searchQuery]);

  useEffect(() => {
    if (scrollToFeedId) {
      // [수정] DOM이 렌더링될 시간을 살짝 줌 (검색 후 리스트 변경 시 안정성 확보)
      setTimeout(() => {
        const element = document.getElementById(`feed-${scrollToFeedId}`);
        if (element) {
          element.scrollIntoView({ behavior: "smooth", block: "start" });
          setScrollToFeedId(null);
        }
      }, 100);
    }
  }, [scrollToFeedId, setScrollToFeedId, filteredFeeds]);

  // [추가] 신고 핸들러 (낙관적 업데이트)
  const handleReport = async (feedId: number, currentStatus: boolean) => {
    console.log(
      `[FeedView] handleReport called. FeedId: ${feedId}, CurrentStatus: ${currentStatus}, Action: ${
        !currentStatus ? "Report" : "Cancel Report"
      }`
    );

    // 1. UI 즉시 업데이트 (Toggle)
    toggleReport(feedId, !currentStatus);

    try {
      // 2. API 호출
      console.log(`[FeedView] Calling reportApi.reportFeed(${feedId})...`);
      await reportApi.reportFeed(feedId);
      console.log(`[FeedView] Report API success for FeedId: ${feedId}`);

      toast.success(
        !currentStatus ? "신고가 접수되었습니다." : "신고가 취소되었습니다."
      );
    } catch (error) {
      console.error(
        `[FeedView] Report API failed for FeedId: ${feedId}`,
        error
      );

      // 3. 실패 시 롤백
      toggleReport(feedId, currentStatus);
      toast.error(
        !currentStatus ? "신고에 실패했습니다." : "신고 취소에 실패했습니다."
      );
    }
  };

  return (
    <div className="h-full flex flex-col p-2 gap-4 relative">
      {/* 검색바 */}
      <div className="mx-auto max-w-2xl w-full flex-shrink-0 z-10">
        <SearchInput
          value={searchQuery}
          onChange={setSearchQuery}
          placeholder="작성자 이름으로 검색..."
          suggestions={authorSuggestions}
        />
      </div>

      {/* 피드 리스트 컨테이너 (윈도윙 적용) */}
      <div className="flex-1 w-full h-full overflow-hidden relative">
        <Virtuoso
          // [수정] 스크롤바 숨김 & 스냅 기능 복구
          className="scrollbar-hide snap-y snap-mandatory"
          style={{ height: "100%", width: "100%" }}
          data={filteredFeeds}
          // [핵심] 바닥에 닿으면 다음 페이지 로드 (검색어 상태 유지)
          endReached={() => {
            if (hasMoreFeeds && !isLoadingFeeds) {
              loadFeeds(
                feedPage + 1, // 현재 페이지 + 1
                false, // reset=false (기존 데이터 유지)
                undefined,
                searchQuery // 검색어 전달
              );
            }
          }}
          // [핵심] 미리 렌더링할 픽셀 범위 (스크롤 끊김 방지)
          overscan={1000}
          // index를 사용하지 않으므로 _로 변경하여 사용 안 함을 명시
          itemContent={(_, feed: Feed) => {
            return (
              <div
                key={feed.id}
                id={`feed-${feed.id}`}
                // [수정] 높이를 100dvh - 70px(검색바)로 설정하여 한 화면에 하나만 표시
                // [수정] items-center로 내부 카드를 수직 중앙 정렬, 상하 여백(py-2) 추가
                className="h-[calc(100dvh-70px)] md:h-[calc(100vh-80px)] w-full snap-center snap-always flex items-center justify-center py-2 md:py-4"
              >
                {/* FeedItem을 FeedCard로 교체하여 원본 파일들과 다시 연결합니다 */}
                <FeedCard
                  feed={feed}
                  currentImageIndex={getFeedImageIndex(feed.id)}
                  liked={hasLiked(feed.id)}
                  isReported={feed.isReported || false}
                  onReport={(id) => handleReport(id, feed.isReported || false)}
                  onImageIndexChange={(fid, idx) => setFeedImageIndex(fid, idx)}
                  onLike={(id) => handleLike(id)}
                />
              </div>
            );
          }}
          // [수정] 스크롤 컨테이너 ref 연결 (헬퍼 함수 사용)
          scrollerRef={(ref: HTMLElement | Window | null) => {
            updateFeedRef(feedContainerRef, ref as HTMLElement);
          }}
        />
      </div>

      {/* [핵심 2] 플로팅 로더 (Floating Loader)
          - 리스트 바닥에 박혀있는 게 아니라 화면 하단에 둥둥 떠 있습니다.
          - 스크롤이 튕겨 올라가서 바닥이 안 보여도 "로딩 중"임을 확실히 알 수 있습니다.
          - Loader2 컴포넌트 대신 순수 CSS로 구현하여 에러를 없앴습니다.
      */}
      {isLoadingFeeds && (
        <div className="absolute bottom-8 left-1/2 -translate-x-1/2 z-50 pointer-events-none">
          <div className="bg-white/90 backdrop-blur-md shadow-[0_4px_12px_rgba(0,0,0,0.15)] rounded-full py-2 px-4 flex items-center gap-2 border border-gray-100">
            {/* 순수 CSS 스피너 (에러 없음) */}
            <div className="w-4 h-4 border-2 border-gray-200 border-t-[#C93831] rounded-full animate-spin" />
            <span className="text-xs font-bold text-gray-700">
              더 불러오는 중...
            </span>
          </div>
        </div>
      )}
    </div>
  );
}
