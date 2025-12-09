/**
 * RankingSkeleton.tsx
 *
 * 랭킹 페이지 로딩 스켈레톤 UI
 * - 실제 레이아웃과 동일한 크기로 CLS 방지
 * - 메인 테마색 활용
 */

import { Card } from "@/components/ui/card";
import { ScrollArea } from "@/components/ui/scroll-area";
import { RANKING_CONSTANTS, THEME_COLORS } from "@/constants/rankingConstants";

interface RankingSkeletonProps {
  currentMonth: number;
}

export function RankingSkeleton({ currentMonth }: RankingSkeletonProps) {
  const skeletonBgColor = THEME_COLORS.PRIMARY_LIGHT_ALPHA_15;

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
            {/* 랭킹 리스트 스켈레톤 */}
            <div className="lg:col-span-2 flex flex-col gap-2">
              {Array.from({ length: RANKING_CONSTANTS.TOP_RANKERS_COUNT }).map(
                (_, i) => (
                  <div
                    key={i}
                    className="w-full rounded-xl animate-pulse"
                    style={{
                      backgroundColor: skeletonBgColor,
                      height: "58px", // RankerItem과 동일한 높이
                    }}
                  />
                )
              )}
            </div>

            {/* 사이드바 스켈레톤 */}
            <div className="space-y-6">
              {/* 내 통계 스켈레톤 */}
              <Card className="backdrop-blur-2xl bg-white/60 border border-gray-200 shadow-xl">
                <div className="p-6 space-y-4">
                  <div className="flex items-center gap-2">
                    <div
                      className="w-6 h-6 rounded animate-pulse"
                      style={{ backgroundColor: skeletonBgColor }}
                    />
                    <div
                      className="h-6 w-20 rounded animate-pulse"
                      style={{ backgroundColor: skeletonBgColor }}
                    />
                  </div>
                  <div className="space-y-3">
                    {Array.from({ length: 3 }).map((_, i) => (
                      <div
                        key={i}
                        className="flex justify-between items-center"
                      >
                        <div
                          className="h-5 w-24 rounded animate-pulse"
                          style={{ backgroundColor: skeletonBgColor }}
                        />
                        <div
                          className="h-7 w-12 rounded animate-pulse"
                          style={{ backgroundColor: skeletonBgColor }}
                        />
                      </div>
                    ))}
                  </div>
                </div>
              </Card>

              {/* 전체 현황 스켈레톤 */}
              <Card className="backdrop-blur-2xl bg-white/60 border border-gray-200 shadow-xl">
                <div className="p-6 space-y-4">
                  <div className="flex items-center gap-2">
                    <div
                      className="w-6 h-6 rounded animate-pulse"
                      style={{ backgroundColor: skeletonBgColor }}
                    />
                    <div
                      className="h-6 w-24 rounded animate-pulse"
                      style={{ backgroundColor: skeletonBgColor }}
                    />
                  </div>
                  <div className="space-y-3">
                    {Array.from({ length: 3 }).map((_, i) => (
                      <div
                        key={i}
                        className="flex justify-between items-center"
                      >
                        <div
                          className="h-5 w-24 rounded animate-pulse"
                          style={{ backgroundColor: skeletonBgColor }}
                        />
                        <div
                          className="h-7 w-12 rounded animate-pulse"
                          style={{ backgroundColor: skeletonBgColor }}
                        />
                      </div>
                    ))}
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
