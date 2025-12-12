/**
 * UserHoverCard.tsx
 *
 * 사용자 프로필 호버 카드 컴포넌트
 * - 아바타에 마우스를 올리면 사용자 정보 표시
 * - 피드, 댓글 등에서 재사용
 */

import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import {
  HoverCard,
  HoverCardContent,
  HoverCardTrigger,
} from "@/components/ui/hover-card";
import { User } from "lucide-react";
import { getProfileThumbnailUrl } from "@/api";

export interface UserHoverCardProps {
  /** 사용자 이름 */
  name: string;
  /** 부서/설명 */
  department?: string;
  /** 이번 달 활동일 */
  activeDays?: number;
  /** 아바타 크기 (기본: md) */
  size?: "sm" | "md" | "lg";
  /** 아바타 이미지 URL */
  avatarUrl?: string;
  /** 추가 클래스 */
  className?: string;
}

const sizeClasses = {
  sm: "w-8 h-8",
  md: "w-10 h-10",
  lg: "w-14 h-14",
};

const iconSizes = {
  sm: "w-4 h-4",
  md: "w-5 h-5",
  lg: "w-7 h-7",
};

export function UserHoverCard({
  name,
  department,
  activeDays,
  size = "md",
  avatarUrl,
  className,
}: UserHoverCardProps) {
  return (
    <HoverCard openDelay={200} closeDelay={100}>
      <HoverCardTrigger asChild>
        <div className={className}>
          <Avatar className={`${sizeClasses[size]} border-2 border-white shadow-lg cursor-pointer`}>
            {avatarUrl ? (
              <img src={getProfileThumbnailUrl(avatarUrl)} alt={name} className="w-full h-full object-cover" loading="lazy" />
            ) : (
              <AvatarFallback className="bg-white">
                <User className={`${iconSizes[size]} text-gray-400`} />
              </AvatarFallback>
            )}
          </Avatar>
        </div>
      </HoverCardTrigger>
      <HoverCardContent className="w-80 bg-white/95 backdrop-blur-xl border border-gray-200">
        <div className="flex gap-4">
          <Avatar className="w-14 h-14 border-2 border-white shadow-lg bg-white">
            {avatarUrl ? (
              <img src={getProfileThumbnailUrl(avatarUrl)} alt={name} className="w-full h-full object-cover" loading="lazy" />
            ) : (
              <AvatarFallback className="bg-white">
                <User className="w-7 h-7 text-gray-400" />
              </AvatarFallback>
            )}
          </Avatar>
          <div className="space-y-2 flex-1">
            <h4 className="text-base font-black text-gray-900">{name}</h4>
            <p className="text-sm text-gray-700 font-medium">{department || "부서 미정"}</p>
            {activeDays !== undefined && (
              <div className="pt-1">
                <div className="flex justify-between text-xs">
                  <span className="text-gray-600 font-medium">이번 달 활동</span>
                  <span className="font-black text-gray-900">{activeDays}일</span>
                </div>
              </div>
            )}
          </div>
        </div>
      </HoverCardContent>
    </HoverCard>
  );
}

export default UserHoverCard;
