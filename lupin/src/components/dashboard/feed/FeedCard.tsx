/**
 * FeedCard.tsx
 *
 * 피드 카드 컴포넌트
 * - 이미지 캐러셀
 * - 좋아요/댓글 버튼
 * - 댓글 섹션
 */

import React, { useState } from "react";
// 추가할 코드
import { Badge } from "@/components/ui/badge";
import {
  Heart,
  MessageCircle,
  ChevronLeft,
  ChevronRight,
  MoreHorizontal,
  Siren,
} from "lucide-react";
import { Feed } from "@/types/dashboard.types";
import { FeedContentDisplay } from "@/components/shared/FeedContent";
import { useImageBrightness } from "@/hooks";
import { getCdnUrl, reportApi } from "@/api"; // reportApi 추가 확인 필요
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { toast } from "sonner";
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
  // 추가할 코드
  const [showComments, setShowComments] = useState(false);
  const [isReported, setIsReported] = useState(feed.isReported || false); // DB 상태로 초기화

  // [신고 핸들러] 신고 상태 토글
  const handleReport = async () => {
    const action = isReported ? "신고를 취소" : "이 게시글을 신고";
    if (!confirm(`정말 ${action}하시겠습니까?`)) return;

    try {
      // 1. 서버에 신고 요청
      await reportApi.reportFeed(feed.id);

      // 2. 상태 토글 (UI 업데이트)
      setIsReported(!isReported);

      toast.success(
        isReported ? "신고가 취소되었습니다." : "신고가 접수되었습니다."
      );
    } catch (error) {
      console.error(error);
      toast.error("신고 처리에 실패했습니다.");
    }
  };

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
    onImageIndexChange(
      feed.id,
      Math.min(feed.images.length - 1, currentImageIndex + 1)
    );
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
                  src={getCdnUrl(
                    feed.images[currentImageIndex] || feed.images[0]
                  )}
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
                        <ChevronLeft
                          className={`w-8 h-8 ${
                            iconColor === "white" ? "text-white" : "text-black"
                          }`}
                        />
                      </button>
                    )}
                    {currentImageIndex < feed.images.length - 1 && (
                      <button
                        onClick={handleNextImage}
                        className="absolute right-2 top-1/2 -translate-y-1/2 hover:opacity-70 transition-opacity"
                      >
                        <ChevronRight
                          className={`w-8 h-8 ${
                            iconColor === "white" ? "text-white" : "text-black"
                          }`}
                        />
                      </button>
                    )}
                    {/* 인디케이터 */}
                    <div className="absolute bottom-2 left-1/2 -translate-x-1/2 flex gap-1">
                      {feed.images.map((_, idx) => (
                        <div
                          key={idx}
                          className={`w-1.5 h-1.5 rounded-full ${
                            idx === currentImageIndex
                              ? "bg-white"
                              : "bg-white/50"
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
            <div className="absolute top-4 left-4 z-10">
              <UserHoverCard
                name={feed.author || feed.writerName}
                department={feed.writerDepartment}
                activeDays={feed.writerActiveDays}
                avatarUrl={feed.writerAvatar}
              />
            </div>

            {/* [추가] 우측 상단 더보기 메뉴 (신고) */}
            <div className="absolute top-4 right-4 z-10">
              <DropdownMenu>
                <DropdownMenuTrigger asChild>
                  <button className="p-2 rounded-full bg-black/20 hover:bg-black/40 text-white backdrop-blur-md transition-colors outline-none">
                    <MoreHorizontal className="w-5 h-5" />
                  </button>
                </DropdownMenuTrigger>
                <DropdownMenuContent align="end" className="w-40">
                  <DropdownMenuItem
                    onClick={handleReport}
                    className={`${
                      isReported
                        ? "text-gray-500 bg-gray-100"
                        : "text-red-600 focus:text-red-600 focus:bg-red-50"
                    } cursor-pointer gap-2`}
                  >
                    <Siren
                      className={`w-4 h-4 ${isReported ? "fill-gray-500" : ""}`}
                    />
                    <span>{isReported ? "신고 취소" : "신고하기"}</span>
                  </DropdownMenuItem>
                </DropdownMenuContent>
              </DropdownMenu>
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
                <span
                  className={`text-xs font-bold ${
                    iconColor === "white" ? "text-white" : "text-black"
                  }`}
                >
                  {feed.likes}
                </span>
              </button>

              <button
                onClick={() => setShowComments(!showComments)}
                className="flex flex-col items-center gap-1 group"
              >
                <div className="w-12 h-12 rounded-full flex items-center justify-center hover:scale-110 transition-transform">
                  <MessageCircle
                    className={`w-6 h-6 ${
                      iconColor === "white" ? "text-white" : "text-black"
                    }`}
                  />
                </div>
                <span
                  className={`text-xs font-bold ${
                    iconColor === "white" ? "text-white" : "text-black"
                  }`}
                >
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
        {showComments && <FeedCommentSection feedId={feed.id} />}
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
          {feed.time}
          {feed.updatedAt && (
            <span className="text-gray-400 font-normal ml-1">(수정됨)</span>
          )}
        </Badge>
      </div>
    </div>
  );
}

export default FeedCard;
