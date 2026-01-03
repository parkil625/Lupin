/**
 * Home.tsx - Ultimate Performance Optimized
 *
 * 6대 최적화 전략 적용:
 * 1. 렌더링 파이프라인: FeedItem 컴포넌트 분리 + React.memo + useCallback
 * 2. 비즈니스 로직: useMemo로 계산 최적화 (이상적으로는 백엔드 이관)
 * 3. 네트워크 워터폴: Promise.all 병렬 요청
 * 4. 이미지 최적화: loading/fetchPriority + aspect-ratio
 * 5. 리스트 가상화: content-visibility CSS
 * 6. 이미지 페이로드 최적화: srcset + blur placeholder + WebP
 */

import { useState, useEffect, useMemo, useCallback, memo } from "react";
import { Card } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Avatar } from "@/components/ui/avatar";
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import {
  MessageCircle,
  Sparkles,
  Flame,
  Award,
  User,
  Plus,
  Coins,
  Bell,
} from "lucide-react";
import { Feed } from "@/types/dashboard.types";
import { userApi, feedApi, getThumbnailUrl, getCdnUrl } from "@/api";
import { useIsMobile } from "@/components/ui/use-mobile";

// ============================================================================
// [0] Image Optimization Utilities
// ============================================================================

/**
 * 이미지 URL 최적화 헬퍼 함수
 *
 * Cloudinary 또는 CloudFront + Lambda@Edge 설정 시 자동으로 활용
 * 미설정 시 원본 URL 반환 (Graceful Degradation)
 *
 * @param url - 원본 이미지 URL
 * @param width - 원하는 너비 (px)
 * @param quality - 품질 (1-100, 기본값 80)
 * @param format - 이미지 포맷 ('webp' | 'auto')
 */
function getOptimizedImageUrl(
  url: string,
  width: number,
  quality = 80,
  format: "webp" | "auto" = "auto"
): string {
  if (!url) return url;

  // Cloudinary URL 감지 및 변환
  if (url.includes("cloudinary.com")) {
    const parts = url.split("/upload/");
    if (parts.length === 2) {
      const transformations = `w_${width},q_${quality},f_${format},c_fill`;
      return `${parts[0]}/upload/${transformations}/${parts[1]}`;
    }
  }

  // CloudFront URL 감지 및 쿼리 파라미터 추가
  if (url.includes("cloudfront.net") || url.includes("cdn.")) {
    const separator = url.includes("?") ? "&" : "?";
    return `${url}${separator}w=${width}&q=${quality}&f=${format}`;
  }

  // S3 URL 감지 및 쿼리 파라미터 추가 (Lambda@Edge 설정 시)
  if (url.includes("s3.amazonaws.com") || url.includes("s3-")) {
    const separator = url.includes("?") ? "&" : "?";
    return `${url}${separator}w=${width}&q=${quality}&f=${format}`;
  }

  // 미설정 시 원본 URL 반환
  return url;
}

// getThumbnailUrl은 @/api에서 import

/**
 * Blur Placeholder 데이터 URI (16x16 회색 블러)
 * 이미지 로딩 전 보여줄 초경량 플레이스홀더
 */
const BLUR_DATA_URL =
  "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 16 16'%3E" +
  "%3Cfilter id='b' color-interpolation-filters='sRGB'%3E" +
  "%3CfeGaussianBlur stdDeviation='1'/%3E%3C/filter%3E" +
  "%3Crect width='100%25' height='100%25' fill='%23f3f4f6' filter='url(%23b)'/%3E%3C/svg%3E";

// ============================================================================
// [1] Types & Interfaces
// ============================================================================

interface HomeProps {
  profileImage: string | null;
  myFeeds: Feed[];
  setSelectedFeed: (feed: Feed) => void;
  setFeedImageIndex: (
    feedId: number,
    updater: number | ((prev: number) => number)
  ) => void;
  setShowFeedDetailInHome: (show: boolean) => void;
  onCreateClick: () => void;
  refreshTrigger?: number;
  unreadNotificationCount?: number;
  onNotificationClick?: () => void;
}

interface UserApiResponse {
  id: number;
  userId: string;
  name: string;
  realName?: string;
  currentPoints: number;
  status: string;
  hasFeedPenalty: boolean; // 핵심 필드
}

interface UserStats {
  points: number;
  rank: number;
  has7DayStreak: boolean;
  isTop10: boolean;
  isTop100: boolean;
  name: string;
  isBanned: boolean;
  hasFeedPenalty: boolean;
}

// ============================================================================
// [2] Sub-Component: Optimized Feed Item (React.memo)
// ============================================================================

/**
 * 개별 피드 아이템 - React.memo로 불필요한 리렌더링 방지
 *
 * 최적화 포인트:
 * - React.memo: props가 변경되지 않으면 리렌더링 스킵
 * - aspect-ratio CSS: CLS(Layout Shift) 방지
 * - loading/fetchPriority: 상위 4개 이미지 우선 로딩
 * - srcset: 디바이스 해상도에 맞는 이미지 제공 (1x, 2x, 3x)
 * - blur placeholder: 로딩 전 UX 개선
 */
const FeedItem = memo(
  ({
    feed,
    index,
    onFeedClick,
    isPriority, // [추가] 부모로부터 우선순위 여부 주입
  }: {
    feed: Feed;
    index: number;
    onFeedClick: (feedId: number) => void;
    isPriority: boolean; // [추가] 타입 정의
  }) => {
    // [제거] 내부 하드코딩 로직 삭제 (const isPriority = index < 4;)

    // [수정] 원본 이미지 URL 확인
    const originalUrl = feed.images?.[0];

    // [수정] 1. 에러 상태와 이전 URL을 추적 (Derived State 패턴)
    const [hasError, setHasError] = useState(false);
    const [prevUrl, setPrevUrl] = useState(originalUrl);

    // [수정] 2. 피드가 변경되면(originalUrl 변경) 에러 상태를 즉시 초기화
    // useEffect 대신 렌더링 도중 상태를 업데이트하여 경고 해결 및 성능 최적화
    if (originalUrl !== prevUrl) {
      setPrevUrl(originalUrl);
      setHasError(false);
    }

    // [수정] 3. 최종 이미지 URL 결정 (변수명 imgSrc 유지)
    // 에러가 발생했으면(hasError) 원본(CDN) URL, 아니면 썸네일 URL 사용
    const imgSrc = originalUrl
      ? hasError
        ? getCdnUrl(originalUrl)
        : getThumbnailUrl(originalUrl)
      : undefined;

    // [수정] 4. 에러 핸들러: 썸네일 로드 실패 시 에러 상태만 true로 변경
    const handleImgError = () => {
      if (!hasError) setHasError(true);
    };

    return (
      <div
        className="cursor-pointer group"
        onClick={() => onFeedClick(feed.id)}
        role="button"
        tabIndex={0}
        onKeyDown={(e) => e.key === "Enter" && onFeedClick(feed.id)}
        aria-label={`${feed.activity} 피드, 포인트 ${feed.points}점`}
        style={{ contentVisibility: "auto", containIntrinsicSize: "1px 400px" }}
      >
        <div className="aspect-[3/4] w-full">
          <Card className="h-full w-full overflow-hidden rounded-none bg-white border-0 hover:opacity-90 transition-all relative">
            <div className="w-full h-full bg-white">
              {imgSrc ? (
                <img
                  src={imgSrc} // [수정] state 변수 사용
                  alt={feed.activity}
                  width="300"
                  height="400"
                  // [수정] 에러 핸들러 연결
                  onError={handleImgError}
                  style={{
                    backgroundImage: `url("${BLUR_DATA_URL}")`,
                    backgroundSize: "cover",
                  }}
                  loading={isPriority ? "eager" : "lazy"}
                  decoding="async"
                  fetchPriority={isPriority ? "high" : "auto"}
                  className="w-full h-full object-cover"
                />
              ) : (
                <div className="w-full h-full flex items-center justify-center bg-gradient-to-br from-gray-100 to-gray-200">
                  <div className="text-center p-4">
                    <Sparkles
                      className="w-12 h-12 mx-auto text-gray-400 mb-2"
                      aria-hidden="true"
                    />
                    <p className="text-sm font-bold text-gray-600">
                      {feed.activity}
                    </p>
                  </div>
                </div>
              )}
            </div>

            {/* Hover Overlay - CSS로 처리하여 JS 부하 없음 */}
            <div className="absolute inset-0 bg-black/70 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center backdrop-blur-sm">
              <div className="text-center text-white space-y-2">
                <div className="flex items-center justify-center gap-4">
                  {(feed.points ?? 0) > 0 && (
                    <span className="flex items-center gap-1 font-bold text-base">
                      <Coins className="w-5 h-5" />+{feed.points}
                    </span>
                  )}
                  <span className="flex items-center gap-1 font-bold text-base">
                    <MessageCircle className="w-5 h-5" />
                    {feed.comments}
                  </span>
                </div>
              </div>
            </div>
          </Card>
        </div>
      </div>
    );
  }
);
FeedItem.displayName = "FeedItem";

// ============================================================================
// [3] Custom Hook: Business Logic Separation
// ============================================================================

/**
 * 데이터 로딩 로직 분리
 *
 * 최적화 포인트:
 * - Promise.all: 병렬 요청으로 워터폴 제거
 * - useMemo: 7일 연속 계산 캐싱
 */
function useHomeData(myFeeds: Feed[], refreshTrigger: number | undefined) {
  const [stats, setStats] = useState<UserStats | null>(null);
  const [canPost, setCanPost] = useState(false);
  const [loading, setLoading] = useState(true);

  // [최적화 2] 7일 연속 체크를 useMemo로 캐싱
  // 이상적으로는 백엔드에서 계산해서 내려주는 것이 최선
  const has7DayStreak = useMemo(() => {
    if (myFeeds.length < 7) return false;

    // Set을 사용하여 O(1) 조회 성능 확보
    const uniqueDates = new Set(
      myFeeds.map((f) => new Date(f.createdAt || f.time || "").toDateString())
    );

    const today = new Date();
    // 최근 7일(오늘 포함) 체크
    for (let i = 0; i < 7; i++) {
      const targetDate = new Date();
      targetDate.setDate(today.getDate() - i);
      if (!uniqueDates.has(targetDate.toDateString())) return false;
    }
    return true;
  }, [myFeeds]);

  useEffect(() => {
    let isMounted = true;

    const fetchData = async () => {
      // 리프레시 시에는 로딩 표시 생략 (UX 개선)
      if (!refreshTrigger) setLoading(true);

      try {
        const userId = parseInt(localStorage.getItem("userId") || "1");

        // [최적화 3] Promise.all로 병렬 요청
        const [rawUserData, rankingContext, postStatus] = await Promise.all([
          userApi.getUserById(userId),
          userApi.getUserRankingContext(userId),
          feedApi.canPostToday(userId),
        ]);

        if (!isMounted) return;

        // [수정] 타입 단언(Type Assertion)을 사용하여 'any' 에러 해결
        const userData = rawUserData as UserApiResponse;

        const myRanking = rankingContext.find(
          (r: { id: number; rank?: number }) => r.id === userId
        );
        const rank = myRanking?.rank || 999;

        setStats({
          points: userData.currentPoints || 0,
          rank,
          has7DayStreak,
          isTop10: rank <= 10,
          isTop100: rank <= 100,
          name:
            userData.realName || localStorage.getItem("userName") || "사용자",
          isBanned: userData.status === "BANNED",
          // [수정] 이제 userData.hasFeedPenalty를 안전하게 사용할 수 있습니다.
          hasFeedPenalty: userData.hasFeedPenalty === true,
        });
        setCanPost(postStatus);
      } catch {
        // 홈 데이터 로드 실패 (조용히 처리)
      } finally {
        if (isMounted) setLoading(false);
      }
    };

    fetchData();
    return () => {
      isMounted = false;
    };
  }, [refreshTrigger, has7DayStreak]);

  return { stats, canPost, loading };
}

// ============================================================================
// [4] Main Component
// ============================================================================

export default function Home({
  profileImage,
  myFeeds,
  setSelectedFeed,
  setFeedImageIndex,
  setShowFeedDetailInHome,
  onCreateClick,
  refreshTrigger,
  unreadNotificationCount = 0,
  onNotificationClick,
}: HomeProps) {
  const { stats, canPost, loading } = useHomeData(myFeeds, refreshTrigger);
  const isMobile = useIsMobile(); // [추가] 현재 뷰포트가 모바일인지 확인

  // [최적화 7] LCP 이미지 Preload - 뷰포트에 따라 동적 로딩
  // 모바일(2열): 2개, PC(최대 5열): 5개 우선 로드
  useEffect(() => {
    // [디버깅] 현재 감지된 환경 및 프리로딩 개수 확인
    const preloadCount = isMobile ? 2 : 5;
    console.debug(
      `[Home] Image Preloading Strategy: ${isMobile ? "Mobile(2)" : "PC(5)"}`
    );

    const feedsToPreload = myFeeds.slice(0, preloadCount);
    const links: HTMLLinkElement[] = [];

    feedsToPreload.forEach((feed) => {
      const imageUrl = feed.images?.[0];
      if (imageUrl) {
        const link = document.createElement("link");
        link.rel = "preload";
        link.as = "image";
        link.href = getThumbnailUrl(imageUrl);
        document.head.appendChild(link);
        links.push(link);
      }
    });

    return () => {
      links.forEach((link) => link.remove());
    };
  }, [myFeeds]);

  // [최적화 1] useCallback으로 핸들러 참조 고정 - 메모이제이션 완성
  // 이전 문제: onClick={() => ...}가 매번 새로운 함수를 생성하여 FeedItem의 memo가 무력화됨
  // 해결: 핸들러를 useCallback으로 감싸고, feedId만 인자로 받아 처리
  const handleFeedClick = useCallback(
    (feedId: number) => {
      const feed = myFeeds.find((f) => f.id === feedId);
      if (!feed) return;
      setSelectedFeed(feed);
      setFeedImageIndex(feedId, 0);
      setShowFeedDetailInHome(true);
    },
    [myFeeds, setSelectedFeed, setFeedImageIndex, setShowFeedDetailInHome]
  );

  return (
    <div className="h-full overflow-auto p-4 md:p-8 relative scroll-smooth">
      {/* Mobile Notification Button */}
      {onNotificationClick && (
        <button
          onClick={onNotificationClick}
          className="md:hidden fixed top-4 right-4 z-40 w-10 h-10 rounded-full bg-white/80 backdrop-blur-xl shadow-lg flex items-center justify-center hover:bg-white transition-colors"
          aria-label={
            unreadNotificationCount > 0
              ? `${unreadNotificationCount}개의 읽지 않은 알림`
              : "알림 확인"
          }
        >
          <Bell className="w-5 h-5 text-gray-700" />
          {unreadNotificationCount > 0 && (
            <div className="absolute top-1 right-1 w-2.5 h-2.5 bg-red-500 rounded-full border-2 border-white" />
          )}
        </button>
      )}

      <div className="max-w-6xl mx-auto space-y-6 md:space-y-8">
        {/* Profile Header */}
        <div className="p-4 md:p-8">
          <div className="flex flex-col items-center gap-4 mb-6 md:mb-8">
            {/* Profile Avatar - 고정 크기로 CLS 방지 */}
            <div className="w-[110px] h-[110px]">
              <Avatar className="w-full h-full border-4 border-white shadow-xl bg-gray-100">
                {loading ? (
                  <div
                    className="w-full h-full animate-pulse"
                    style={{ backgroundColor: "rgba(201, 56, 49, 0.15)" }}
                  />
                ) : profileImage ? (
                  <img
                    src={getOptimizedImageUrl(profileImage, 220, 85, "webp")}
                    srcSet={`${getOptimizedImageUrl(
                      profileImage,
                      110,
                      85,
                      "webp"
                    )} 1x,
                             ${getOptimizedImageUrl(
                               profileImage,
                               220,
                               85,
                               "webp"
                             )} 2x,
                             ${getOptimizedImageUrl(
                               profileImage,
                               330,
                               85,
                               "webp"
                             )} 3x`}
                    alt={`${stats?.name || "사용자"} 프로필`}
                    width="110"
                    height="110"
                    style={{
                      backgroundImage: `url("${BLUR_DATA_URL}")`,
                      backgroundSize: "cover",
                    }}
                    loading="eager"
                    decoding="async"
                    fetchPriority="high"
                    className="w-full h-full object-cover"
                  />
                ) : (
                  <div className="w-full h-full flex items-center justify-center bg-white">
                    <User className="w-12 h-12 text-gray-400" />
                  </div>
                )}
              </Avatar>
            </div>

            <div className="text-center w-full">
              {/* Name - 고정 높이로 CLS 방지 */}
              <div className="h-10 flex items-center justify-center mb-3 md:mb-4">
                {loading ? (
                  <div
                    className="h-9 w-24 rounded-lg animate-pulse"
                    style={{ backgroundColor: "rgba(201, 56, 49, 0.15)" }}
                  />
                ) : (
                  <h1 className="text-xl md:text-3xl font-black text-gray-900">
                    {stats?.name}
                  </h1>
                )}
              </div>

              {/* Stats - 고정 높이로 CLS 방지 */}
              <div className="h-6 flex items-center justify-center mb-3 md:mb-4">
                {loading ? (
                  <div
                    className="h-5 w-56 rounded-lg animate-pulse mx-auto"
                    style={{ backgroundColor: "rgba(201, 56, 49, 0.15)" }}
                  />
                ) : (
                  <div className="flex justify-center gap-4 md:gap-8">
                    <div>
                      <span className="text-sm text-gray-600 font-bold">
                        피드{" "}
                      </span>
                      <span className="text-sm font-black text-[#C93831]">
                        {myFeeds.length}
                      </span>
                    </div>
                    <div>
                      <span className="text-sm text-gray-600 font-bold">
                        포인트{" "}
                      </span>
                      <span className="text-sm font-black text-[#C93831]">
                        {stats?.points.toLocaleString()}
                      </span>
                    </div>
                    <div>
                      <span className="text-sm text-gray-600 font-bold">
                        순위{" "}
                      </span>
                      <span className="text-sm font-black text-[#C93831]">
                        #{stats?.rank || "-"}
                      </span>
                    </div>
                  </div>
                )}
              </div>

              {/* Badges - 고정 높이로 CLS 완전 방지 */}
              <div className="h-7 flex justify-center gap-2 flex-wrap">
                {loading ? (
                  <>
                    <div
                      className="h-6 w-20 rounded-full animate-pulse"
                      style={{ backgroundColor: "rgba(201, 56, 49, 0.15)" }}
                    />
                    <div
                      className="h-6 w-16 rounded-full animate-pulse"
                      style={{ backgroundColor: "rgba(201, 56, 49, 0.15)" }}
                    />
                  </>
                ) : (
                  <>
                    {stats?.has7DayStreak && (
                      <Badge className="bg-orange-500 text-white px-3 py-1.5 font-bold border-0 text-xs">
                        <Flame className="w-3 h-3 mr-1" />
                        7일 연속
                      </Badge>
                    )}
                    {stats?.isTop10 ? (
                      <Badge className="bg-yellow-500 text-white px-3 py-1.5 font-bold border-0 text-xs">
                        <Award className="w-3 h-3 mr-1" />
                        TOP 10
                      </Badge>
                    ) : (
                      stats?.isTop100 && (
                        <Badge className="bg-purple-700 text-white px-3 py-1.5 font-bold border-0 text-xs">
                          <Award className="w-3 h-3 mr-1" />
                          TOP 100
                        </Badge>
                      )
                    )}
                  </>
                )}
              </div>
            </div>
          </div>
        </div>

        {/* Posts Section */}
        <div>
          {/* Posts Header */}
          <div className="flex items-center justify-between mb-4 md:mb-6 px-4 md:px-8">
            <h2 className="text-xl md:text-2xl font-black text-gray-900">
              피드
            </h2>
            <TooltipProvider>
              <Tooltip>
                <TooltipTrigger asChild>
                  {/* [수정] isBanned 또는 hasFeedPenalty가 있으면 버튼 비활성화 */}
                  <button
                    onClick={
                      canPost && !stats?.isBanned && !stats?.hasFeedPenalty
                        ? onCreateClick
                        : undefined
                    }
                    disabled={
                      !canPost || stats?.isBanned || stats?.hasFeedPenalty
                    }
                    className={`flex items-center gap-2 px-4 py-2 rounded-lg transition-all font-bold ${
                      canPost && !stats?.isBanned && !stats?.hasFeedPenalty
                        ? "bg-gradient-to-r from-[#C93831] to-[#B02F28] text-white hover:shadow-lg cursor-pointer"
                        : "bg-gray-300 text-gray-500 cursor-not-allowed"
                    }`}
                  >
                    <Plus className="w-5 h-5" />
                    만들기
                  </button>
                </TooltipTrigger>
                <TooltipContent side="top" sideOffset={8}>
                  {/* [수정] isBanned 또는 hasFeedPenalty일 때 안내 메시지 표시 */}
                  <p>
                    {stats?.isBanned || stats?.hasFeedPenalty
                      ? "패널티로 인해 피드 작성이 제한되었습니다."
                      : canPost
                      ? "피드 작성"
                      : "하루에 한 번만 피드를 작성할 수 있습니다."}
                  </p>
                </TooltipContent>
              </Tooltip>
            </TooltipProvider>
          </div>

          {/* Posts Grid - content-visibility로 화면 밖 요소 렌더링 생략 */}
          <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5">
            {myFeeds.map((feed, index) => (
              <FeedItem
                key={feed.id}
                feed={feed}
                index={index}
                onFeedClick={handleFeedClick}
                // [수정] 뷰포트에 따른 적응형 우선순위 주입 (모바일: 2, PC: 5)
                isPriority={index < (isMobile ? 2 : 5)}
              />
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
