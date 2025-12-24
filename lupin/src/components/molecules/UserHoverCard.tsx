import React from "react";
import { getCdnUrl } from "@/lib/utils"; // utils 경로 확인 필요

interface UserHoverCardProps {
  name?: string;
  department?: string;
  activeDays?: number;
  avatarUrl?: string;
  size?: "sm" | "md";
}

export default function UserHoverCard({
  name = "알 수 없음",
  department = "소속 없음",
  activeDays = 0,
  avatarUrl,
  size = "md",
}: UserHoverCardProps) {
  // 아바타 이미지가 있으면 CDN URL로 변환, 없으면 기본 이미지
  const imageUrl = avatarUrl
    ? getCdnUrl(avatarUrl)
    : "https://ui-avatars.com/api/?background=random&name=" + name;

  return (
    <div className="flex items-center gap-3 p-2 bg-white rounded-lg shadow-sm border border-gray-100 w-fit">
      <img
        src={imageUrl}
        alt={name}
        className={`${
          size === "sm" ? "w-8 h-8" : "w-10 h-10"
        } rounded-full object-cover border border-gray-200`}
      />
      <div className="flex flex-col">
        <span className="text-sm font-bold text-gray-900">{name}</span>
        <div className="flex items-center gap-2 text-xs text-gray-500">
          <span>{department}</span>
          {activeDays > 0 && (
            <>
              <span className="w-0.5 h-3 bg-gray-300"></span>
              <span className="text-orange-500 font-medium">
                🔥 {activeDays}일째 활동 중
              </span>
            </>
          )}
        </div>
      </div>
    </div>
  );
}