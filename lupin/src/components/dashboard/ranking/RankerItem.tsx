/**
 * RankerItem.tsx
 *
 * 개별 랭커 정보를 표시하는 컴포넌트
 * - React.memo로 불필요한 리렌더링 방지
 * - Atomic Design 패턴 적용
 */

import { memo } from "react";
import { Card } from "@/components/ui/card";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import {
  HoverCard,
  HoverCardTrigger,
  HoverCardContent,
} from "@/components/ui/hover-card";
import { User, Crown, Dumbbell } from "lucide-react";
import { RankerData } from "./useRankingViewModel";
import { RANK_STYLES, RANKING_CONSTANTS } from "@/constants/rankingConstants";
import { getProfileThumbnailUrl } from "@/api";

interface RankerItemProps {
  ranker: RankerData;
}

export const RankerItem = memo(function RankerItem({ ranker }: RankerItemProps) {
  const rankStyle = RANK_STYLES[ranker.rank as keyof typeof RANK_STYLES];

  return (
    <Card
      className={`backdrop-blur-2xl border shadow-lg overflow-hidden transition-all ${
        ranker.isMe
          ? "bg-gradient-to-r from-red-50 to-white border-red-300"
          : "bg-white/60 border-gray-200"
      }`}
    >
      <div className="px-7 py-1 flex items-center w-full">
        <div className="flex items-center gap-4 w-full">
          {/* Rank Indicator */}
          <div className="w-10 flex items-center justify-center">
            {rankStyle ? (
              <Crown
                className={rankStyle.crown.size}
                style={{
                  color: rankStyle.crown.color,
                  fill: rankStyle.crown.fill,
                }}
              />
            ) : (
              <span className="text-2xl font-black text-gray-900">
                {ranker.rank}
              </span>
            )}
          </div>

          {/* Avatar with HoverCard */}
          <HoverCard
            openDelay={RANKING_CONSTANTS.HOVER_CARD_DELAY.OPEN}
            closeDelay={RANKING_CONSTANTS.HOVER_CARD_DELAY.CLOSE}
          >
            <HoverCardTrigger asChild>
              <div>
                <Avatar className="w-10 h-10 border-2 border-white shadow-lg bg-white cursor-pointer">
                  {ranker.profileImage ? (
                    <img
                      src={getProfileThumbnailUrl(ranker.profileImage)}
                      alt={ranker.name}
                      className="w-full h-full object-cover rounded-full"
                      loading="lazy"
                    />
                  ) : (
                    <AvatarFallback className="bg-white">
                      <User className="w-5 h-5 text-gray-400" />
                    </AvatarFallback>
                  )}
                </Avatar>
              </div>
            </HoverCardTrigger>
            <HoverCardContent className="w-80 bg-white/95 backdrop-blur-xl border border-gray-200">
              <div className="flex gap-4">
                <Avatar className="w-14 h-14 border-2 border-white shadow-lg bg-white">
                  {ranker.profileImage ? (
                    <img
                      src={getProfileThumbnailUrl(ranker.profileImage)}
                      alt={ranker.name}
                      className="w-full h-full object-cover rounded-full"
                      loading="lazy"
                    />
                  ) : (
                    <AvatarFallback className="bg-white">
                      <User className="w-7 h-7 text-gray-400" />
                    </AvatarFallback>
                  )}
                </Avatar>
                <div className="space-y-2 flex-1">
                  <h4 className="text-base font-black text-gray-900">
                    {ranker.name}
                  </h4>
                  <p className="text-sm text-gray-700 font-medium">
                    {ranker.department}
                  </p>
                  <div className="pt-1">
                    <div className="flex justify-between text-xs">
                      <span className="text-gray-600 font-medium">
                        이번 달 활동
                      </span>
                      <span className="font-black text-gray-900">
                        {ranker.activeDays}일
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            </HoverCardContent>
          </HoverCard>

          {/* Name and Points */}
          <div className="flex-1 min-w-0">
            <p
              className={`font-black truncate ${
                ranker.rank <= 3 ? "text-xl" : "text-lg"
              } ${ranker.isMe ? "text-red-700" : "text-gray-900"}`}
            >
              {ranker.name}
              {ranker.isMe && (
                <span className="ml-2 text-xs font-medium text-red-600">
                  (나)
                </span>
              )}
            </p>
          </div>

          {/* Stats */}
          <div className="flex items-center gap-2">
            <Dumbbell
              className="w-5 h-5"
              style={{ color: "#C93831" }}
            />
            <span className="text-xl font-black text-gray-900">
              {ranker.points.toLocaleString()}
            </span>
            <span className="text-sm font-medium text-gray-600">pt</span>
          </div>
        </div>
      </div>
    </Card>
  );
});
