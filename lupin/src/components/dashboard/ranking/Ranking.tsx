/**
 * Ranking.tsx
 *
 * 랭킹 페이지 컴포넌트 (최적화 버전)
 * - Promise.all로 API 병렬 호출
 * - RankerItem 컴포넌트 추출 및 React.memo 적용
 * - useRankingViewModel 커스텀 훅으로 로직 분리
 * - Semantic markup (ol/li) 적용
 * - CLS 방지 스켈레톤 UI
 */

import { useEffect } from "react";
import { Card } from "@/components/ui/card";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Users } from "lucide-react";
import { useRankingViewModel } from "./useRankingViewModel";
import { RankerItem } from "./RankerItem";
import { RankingSkeleton } from "./RankingSkeleton";
import { THEME_COLORS } from "@/constants/rankingConstants";
import { getProfileThumbnailUrl } from "@/api";

interface RankingProps {
  userId: number;
  profileImage: string | null;
}

export default function Ranking({ userId, profileImage }: RankingProps) {
  const {
    topRankers,
    belowRankers,
    statistics,
    loading,
    currentMonth,
  } = useRankingViewModel(userId, profileImage);

  // [최적화] 상위 3개 프로필 이미지 preload - LCP 개선
  useEffect(() => {
    const imagesToPreload = topRankers
      .slice(0, 3)
      .filter(ranker => ranker.profileImage)
      .map(ranker => getProfileThumbnailUrl(ranker.profileImage!));

    const links: HTMLLinkElement[] = [];
    imagesToPreload.forEach(url => {
      const link = document.createElement('link');
      link.rel = 'preload';
      link.as = 'image';
      link.href = url;
      document.head.appendChild(link);
      links.push(link);
    });

    return () => links.forEach(link => link.remove());
  }, [topRankers]);

  // 로딩 중일 때 스켈레톤 렌더링
  if (loading) {
    return <RankingSkeleton currentMonth={currentMonth} />;
  }

  return (
    <ScrollArea className="h-full">
      <div className="p-4 md:p-8">
        <div className="max-w-7xl mx-auto w-full">
          {/* 헤더 */}
          <div className="mb-4 md:mb-6">
            <h1 className="text-3xl md:text-5xl font-black text-gray-900 mb-2">
              {currentMonth}월 랭킹
            </h1>
            <p className="text-gray-700 font-medium text-base md:text-lg">
              이번 달 TOP 운동왕은 누구?
            </p>
          </div>

          <div className="grid lg:grid-cols-3 gap-4 md:gap-8">
            {/* 랭킹 리스트 */}
            <div className="lg:col-span-2 flex flex-col gap-2">
              {/* Top 10 Rankers - Semantic <ol> 마크업 */}
              <ol className="flex flex-col gap-2" style={{ listStyle: "none", padding: 0, margin: 0 }}>
                {topRankers.map((ranker) => (
                  <li key={ranker.rank}>
                    <RankerItem ranker={ranker} />
                  </li>
                ))}
              </ol>

              {/* Separator - 10등 밖일 때만 표시 */}
              {belowRankers.length > 0 && (
                <div className="flex items-center justify-center py-3">
                  <div className="flex flex-col gap-1">
                    <div className="w-1.5 h-1.5 rounded-full bg-gray-400" />
                    <div className="w-1.5 h-1.5 rounded-full bg-gray-400" />
                    <div className="w-1.5 h-1.5 rounded-full bg-gray-400" />
                  </div>
                </div>
              )}

              {/* 11~13등 영역 - Semantic <ol> 마크업 */}
              {belowRankers.length > 0 && (
                <ol className="flex flex-col gap-2" style={{ listStyle: "none", padding: 0, margin: 0 }}>
                  {belowRankers.map((ranker) => (
                    <li key={ranker.rank}>
                      <RankerItem ranker={ranker} />
                    </li>
                  ))}
                </ol>
              )}
            </div>

            {/* 사이드바 */}
            <div className="space-y-6">
              {/* 전체 현황 */}
              <Card className="backdrop-blur-2xl bg-white/60 border border-gray-200 shadow-xl">
                <div className="p-6 space-y-4">
                  <h2 className="text-xl font-black text-gray-900 flex items-center gap-2">
                    <Users className="w-6 h-6" style={{ color: THEME_COLORS.PRIMARY }} />
                    전체 현황
                  </h2>

                  <div className="space-y-3">
                    <div className="flex justify-between items-center">
                      <span className="text-gray-700 font-medium">총 참여자</span>
                      <span className="font-black text-xl text-gray-900">
                        {statistics.totalUsers}명
                      </span>
                    </div>
                    <div className="flex justify-between items-center">
                      <span className="text-gray-700 font-medium">
                        이번 달 활동
                      </span>
                      <span className="font-black text-xl text-gray-900">
                        {statistics.activeUsersThisMonth}명
                      </span>
                    </div>
                    <div className="flex justify-between items-center">
                      <span className="text-gray-700 font-medium">평균 점수</span>
                      <span className="font-black text-xl text-gray-900">
                        {statistics.averagePoints.toLocaleString()}점
                      </span>
                    </div>
                  </div>
                </div>
              </Card>
            </div>
          </div>
        </div>
      </div>
    </ScrollArea>
  );
}
