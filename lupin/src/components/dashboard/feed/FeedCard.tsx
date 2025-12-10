/**
 * FeedCard.tsx
 *
 * 피드 카드 컴포넌트
 * - 이미지 캐러셀
 * - 좋아요/댓글 버튼
 * - 댓글 섹션
 */

import React, { useState } from "react";
import { Badge } from "@/components/ui/badge";
import {
  Heart,
  MessageCircle,
  ChevronLeft,
  ChevronRight,
  Pencil,
} from "lucide-react";
import { Feed } from "@/types/dashboard.types";
import { FeedContentDisplay } from "@/components/shared/FeedContent";
import { useImageBrightness } from "@/hooks";
import { getCdnUrl } from "@/api";
import { UserHoverCard } from "@/components/dashboard/shared/UserHoverCard";
import { FeedCommentSection } from "./FeedCommentSection";

export interface FeedCardProps {
  /** 피드 데이터 */
  feed: Feed;
  /** 현재 이미지 인덱스 */
  currentImageIndex: number;
  /** 좋아요 여부 */
  liked: boolean;
  /** 이미지 인덱스 변경 핸들러 */
  onImageIndexChange: (feedId: number, index: number) => void;
  /** 좋아요 핸들러 */
  onLike: (feedId: number) => void;
}

export function FeedCard({
  feed,
  currentImageIndex,
  liked,
  onImageIndexChange,
  onLike,
}: FeedCardProps) {
  const [showComments, setShowComments] = useState(false);

  // 이미지 밝기에 따른 아이콘 색상 (CDN URL 사용)
  const currentImage = feed.images?.[currentImageIndex] || feed.images?.[0];
  const currentImageUrl = currentImage ? getCdnUrl(currentImage) : undefined;
  const iconColor = useImageBrightness(currentImageUrl);

  const hasImages = feed.images && feed.images.length > 0;

  const handlePrevImage = (e: React.MouseEvent) => {
    e.stopPropagation();
    onImageIndexChange(feed.id, Math.max(0, currentImageIndex - 1));
  };

  const handleNextImage = (e: React.MouseEvent) => {
    e.stopPropagation();
    onImageIndexChange(feed.id, Math.min(feed.images.length - 1, currentImageIndex + 1));
  };

  return (
    <div className="flex items-center justify-center w-full">
      <div
        className={`h-full max-h-[95vh] overflow-hidden backdrop-blur-2xl bg-white/60 border border-gray-200/30 shadow-2xl flex transition-all duration-300 w-full ${
          showComments ? "max-w-[825px]" : "max-w-[475px]"
        }`}
      >
        {/* 메인 피드 콘텐츠 */}
        <div className="w-full max-w-[475px] flex-shrink-0 flex flex-col overflow-hidden">
          {/* 이미지 영역 */}
          <div className="relative aspect-[9/10] w-full overflow-hidden">
            {hasImages ? (
              <>
                <img
                  src={getCdnUrl(feed.images[currentImageIndex] || feed.images[0])}
                  alt={feed.activity}
                  className="w-full h-full object-cover"
                />

                {/* 이미지 네비게이션 */}
                {feed.images.length > 1 && (
                  <>
                    {currentImageIndex > 0 && (
                      <button
                        onClick={handlePrevImage}
                        className="absolute left-2 top-1/2 -translate-y-1/2 hover:opacity-70 transition-opacity"
                      >
                        <ChevronLeft className={`w-8 h-8 ${iconColor === "white" ? "text-white" : "text-black"}`} />
                      </button>
                    )}
                    {currentImageIndex < feed.images.length - 1 && (
                      <button
                        onClick={handleNextImage}
                        className="absolute right-2 top-1/2 -translate-y-1/2 hover:opacity-70 transition-opacity"
                      >
                        <ChevronRight className={`w-8 h-8 ${iconColor === "white" ? "text-white" : "text-black"}`} />
                      </button>
                    )}
                    {/* 인디케이터 */}
                    <div className="absolute bottom-2 left-1/2 -translate-x-1/2 flex gap-1">
                      {feed.images.map((_, idx) => (
                        <div
                          key={idx}
                          className={`w-1.5 h-1.5 rounded-full ${
                            idx === currentImageIndex ? "bg-white" : "bg-white/50"
                          }`}
                        />
                      ))}
                    </div>
                  </>
                )}
              </>
            ) : (
              <div className="w-full h-full bg-gradient-to-br from-gray-100 to-gray-200 flex items-center justify-center">
                <p className="text-gray-500 font-medium">{feed.activity}</p>
              </div>
            )}

            {/* 작성자 아바타 */}
            <div className="absolute top-4 left-4">
              <UserHoverCard
                name={feed.author || feed.writerName}
                points={feed.points}
                avatarUrl={feed.writerAvatar}
              />
            </div>

            {/* 액션 버튼 */}
            <div className="absolute right-4 bottom-4 flex flex-col gap-4 z-10">
              <button
                onClick={() => onLike(feed.id)}
                className="flex flex-col items-center gap-1 group"
              >
                <div className="w-12 h-12 rounded-full flex items-center justify-center hover:scale-110 transition-transform">
                  <Heart
                    className={`w-6 h-6 ${
                      liked
                        ? "fill-[#C93831] text-[#C93831]"
                        : iconColor === "white"
                        ? "text-white"
                        : "text-black"
                    }`}
                  />
                </div>
                <span className={`text-xs font-bold ${iconColor === "white" ? "text-white" : "text-black"}`}>
                  {feed.likes}
                </span>
              </button>

              <button
                onClick={() => setShowComments(!showComments)}
                className="flex flex-col items-center gap-1 group"
              >
                <div className="w-12 h-12 rounded-full flex items-center justify-center hover:scale-110 transition-transform">
                  <MessageCircle className={`w-6 h-6 ${iconColor === "white" ? "text-white" : "text-black"}`} />
                </div>
                <span className={`text-xs font-bold ${iconColor === "white" ? "text-white" : "text-black"}`}>
                  {feed.comments}
                </span>
              </button>
            </div>
          </div>

          {/* 피드 내용 */}
          <div className="p-6 space-y-3 flex-1 overflow-auto bg-transparent">
            <FeedBadges feed={feed} />
            <FeedContentDisplay content={feed.content} />
          </div>
        </div>

        {/* 댓글 패널 */}
        {showComments && (
          <FeedCommentSection feedId={feed.id} />
        )}
      </div>
    </div>
  );
}

/** 피드 뱃지 컴포넌트 */
function FeedBadges({ feed }: { feed: Feed }) {
  return (
    <div className="flex items-start justify-between gap-3">
      <div className="flex items-center gap-2 flex-wrap">
        <Badge className="bg-gradient-to-r from-yellow-400 to-orange-500 text-white px-3 py-1 font-bold border-0">
          +{feed.points}
        </Badge>
        <Badge className="bg-white text-blue-700 px-3 py-1 font-bold text-xs border-0">
          {feed.activity}
        </Badge>
        {feed.calories && (
          <Badge className="bg-white text-orange-700 px-3 py-1 font-bold text-xs border-0">
            {feed.calories}kcal
          </Badge>
        )}
      </div>
      <div className="flex items-center gap-1.5 flex-shrink-0">
        <Badge className="bg-white text-gray-700 px-3 py-1 font-bold text-xs flex items-center gap-1 border-0">
          {feed.updatedAt && <Pencil className="w-3 h-3" />}
          {feed.time}
        </Badge>
      </div>
    </div>
  );
}

export default FeedCard;
