/**
 * Feed.tsx
 *
 * 피드 페이지 컴포넌트
 * - 모든 사용자의 운동 인증 피드 표시
 * - 좋아요, 댓글 기능
 * - 검색 및 필터링 기능
 */

import React, { useMemo, useState, useEffect, useRef, useCallback } from "react";
import { Badge } from "@/components/ui/badge";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import {
  HoverCard,
  HoverCardTrigger,
  HoverCardContent,
} from "@/components/ui/hover-card";
import { ScrollArea } from "@/components/ui/scroll-area";
import {
  Heart,
  MessageCircle,
  X,
  Sparkles,
  ChevronLeft,
  ChevronRight,
  Pencil,
  Send,
  ArrowUpDown,
  User,
} from "lucide-react";
import { SearchInput } from "@/components/molecules";
import { Feed, Comment } from "@/types/dashboard.types";
import { useCreateBlockNote } from "@blocknote/react";
import { BlockNoteView } from "@blocknote/mantine";
import "@blocknote/mantine/style.css";
import { commentApi } from "@/api";

// 상대적 시간 표시 함수
function getRelativeTime(date: Date | string): string {
  const now = new Date();
  const targetDate = typeof date === 'string' ? new Date(date) : date;
  const diffMs = now.getTime() - targetDate.getTime();
  const diffSeconds = Math.floor(diffMs / 1000);
  const diffMinutes = Math.floor(diffSeconds / 60);
  const diffHours = Math.floor(diffMinutes / 60);
  const diffDays = Math.floor(diffHours / 24);
  const diffWeeks = Math.floor(diffDays / 7);
  const diffMonths = Math.floor(diffDays / 30);
  const diffYears = Math.floor(diffDays / 365);

  if (diffSeconds < 60) return "방금 전";
  if (diffMinutes < 60) return `${diffMinutes}분 전`;
  if (diffHours < 24) return `${diffHours}시간 전`;
  if (diffDays < 7) return `${diffDays}일 전`;
  if (diffWeeks < 4) return `${diffWeeks}주 전`;
  if (diffMonths < 12) return `${diffMonths}개월 전`;
  return `${diffYears}년 전`;
}

interface FeedViewProps {
  allFeeds: Feed[];
  searchQuery: string;
  setSearchQuery: (query: string) => void;
  getFeedImageIndex: (feedId: number) => number;
  setFeedImageIndex: (feedId: number, index: number) => void;
  hasLiked: (feedId: number) => boolean;
  handleLike: (feedId: number) => void;
  feedContainerRef: React.RefObject<HTMLDivElement | null>;
  scrollToFeedId: number | null;
  setScrollToFeedId: (id: number | null) => void;
  loadMoreFeeds: () => void;
  hasMoreFeeds: boolean;
  isLoadingFeeds: boolean;
}

// 개별 피드 카드 컴포넌트
function FeedCard({
  feed,
  currentIndex,
  liked,
  setFeedImageIndex,
  handleLike,
}: {
  feed: Feed;
  currentIndex: number;
  liked: boolean;
  setFeedImageIndex: (feedId: number, index: number) => void;
  handleLike: (feedId: number) => void;
}) {
  const [showComments, setShowComments] = useState(false);
  const [commentText, setCommentText] = useState("");
  const [replyCommentText, setReplyCommentText] = useState("");
  const [comments, setComments] = useState<Comment[]>([]);
  const [replyingTo, setReplyingTo] = useState<number | null>(null);
  const [collapsedComments, setCollapsedComments] = useState<Set<number>>(
    new Set()
  );
  const [commentLikes, setCommentLikes] = useState<{
    [key: number]: { liked: boolean; count: number };
  }>({});
  const [sortOrder, setSortOrder] = useState<"latest" | "popular">("latest");
  const [showSortMenu, setShowSortMenu] = useState(false);
  const [iconColor, setIconColor] = useState<"white" | "black">("white");
  const [prevFeedId, setPrevFeedId] = useState(feed.id);

  // Feed가 변경되면 상태 리셋 (렌더링 중 상태 업데이트 패턴)
  if (feed.id !== prevFeedId) {
    setComments([]);
    setShowComments(false);
    setPrevFeedId(feed.id);
  }

  // 댓글 데이터 로드
  useEffect(() => {
    const fetchComments = async () => {
      if (!showComments) return;

      try {
        const response = await commentApi.getCommentsByFeedId(feed.id, 0, 100);
        // API 응답이 Page 객체인 경우
        const commentList = response.content || response;

        // 답글 정보도 함께 로드
        const commentsWithReplies = await Promise.all(
          commentList.map(async (comment: any) => {
            try {
              const replies = await commentApi.getRepliesByCommentId(
                comment.id
              );
              // 답글도 시간 포맷팅
              const formattedReplies = (replies || []).map((reply: { createdAt: string; [key: string]: unknown }) => ({
                ...reply,
                time: getRelativeTime(reply.createdAt),
              }));
              return {
                ...comment,
                time: getRelativeTime(comment.createdAt),
                replies: formattedReplies,
              };
            } catch (error) {
              return {
                ...comment,
                time: getRelativeTime(comment.createdAt),
                replies: [],
              };
            }
          })
        );

        setComments(commentsWithReplies);
      } catch (error) {
        console.error("댓글 데이터 로드 실패:", error);
        setComments([]);
      }
    };

    fetchComments();
  }, [feed.id, showComments]);

  // 이미지 밝기 분석하여 아이콘 색상 결정
  useEffect(() => {
    if (!feed.images || feed.images.length === 0) {
      return;
    }

    const img = new Image();
    img.crossOrigin = "Anonymous";
    img.src = feed.images[currentIndex] || feed.images[0];

    img.onload = () => {
      const canvas = document.createElement("canvas");
      const ctx = canvas.getContext("2d");
      if (!ctx) return;

      canvas.width = img.width;
      canvas.height = img.height;
      ctx.drawImage(img, 0, 0);

      // 우측 하단 영역의 밝기 계산 (아이콘이 위치한 부분)
      const sampleWidth = Math.min(100, img.width);
      const sampleHeight = Math.min(150, img.height);
      const x = img.width - sampleWidth;
      const y = img.height - sampleHeight;

      const imageData = ctx.getImageData(x, y, sampleWidth, sampleHeight);
      const data = imageData.data;

      let totalBrightness = 0;
      let totalAlpha = 0;
      for (let i = 0; i < data.length; i += 4) {
        const r = data[i];
        const g = data[i + 1];
        const b = data[i + 2];
        const a = data[i + 3];
        // 밝기 계산 (perceived brightness)
        const brightness = 0.299 * r + 0.587 * g + 0.114 * b;
        totalBrightness += brightness;
        totalAlpha += a;
      }

      const avgBrightness = totalBrightness / (data.length / 4);
      const avgAlpha = totalAlpha / (data.length / 4);

      // 투명한 배경이면 검정색, 아니면 평균 밝기에 따라 결정
      if (avgAlpha < 200) {
        setIconColor("black");
      } else {
        // 평균 밝기가 128보다 크면 어두운 아이콘, 작으면 밝은 아이콘
        setIconColor(avgBrightness > 128 ? "black" : "white");
      }
    };
  }, [feed.images, currentIndex]);

  // BlockNote 에디터 생성
  const initialContent = useMemo(() => {
    if (!feed.content) return undefined;
    try {
      const parsed = JSON.parse(feed.content);
      return parsed;
    } catch {
      return [
        {
          type: "paragraph",
          content: feed.content,
        },
      ];
    }
  }, [feed.content]);

  const editor = useCreateBlockNote({
    initialContent,
  });

  // 현재 사용자 정보
  const currentUserName = localStorage.getItem("userName") || "알 수 없음";
  const currentUserId = parseInt(localStorage.getItem("userId") || "1");

  const handleSendComment = async () => {
    if (commentText.trim()) {
      try {
        const response = await commentApi.createComment({
          content: commentText,
          feedId: feed.id,
          writerId: currentUserId,
        });

        const authorName = response.writerName || currentUserName;
        const newComment: Comment = {
          id: response.id,
          author: authorName,
          avatar: authorName.charAt(0),
          content: response.content,
          time: "방금 전",
          replies: [],
        };
        setComments([...comments, newComment]);
        setCommentText("");
      } catch (error) {
        console.error("댓글 작성 실패:", error);
        alert("댓글 작성에 실패했습니다.");
      }
    }
  };

  const handleSendReply = async () => {
    if (replyCommentText.trim() && replyingTo !== null) {
      try {
        const response = await commentApi.createComment({
          content: replyCommentText,
          feedId: feed.id,
          writerId: currentUserId,
          parentId: replyingTo,
        });

        const replyAuthorName = response.writerName || currentUserName;
        const newReply: Comment = {
          id: response.id,
          author: replyAuthorName,
          avatar: replyAuthorName.charAt(0),
          content: response.content,
          time: "방금 전",
          parentId: replyingTo,
          replies: [],
        };

        setComments(
          comments.map((comment) => {
            if (comment.id === replyingTo) {
              return {
                ...comment,
                replies: [...(comment.replies || []), newReply],
              };
            }
            return comment;
          })
        );
        setReplyCommentText("");
        setReplyingTo(null);
      } catch (error) {
        console.error("답글 작성 실패:", error);
        alert("답글 작성에 실패했습니다.");
      }
    }
  };

  const countAllComments = (commentList: Comment[]): number => {
    let count = 0;
    for (const comment of commentList) {
      count += 1;
      if (comment.replies && comment.replies.length > 0) {
        count += countAllComments(comment.replies);
      }
    }
    return count;
  };

  // 댓글이 로드되지 않았으면 feed.comments 사용, 로드되었으면 실제 카운트 사용
  const totalCommentCount =
    comments.length > 0 ? countAllComments(comments) : feed.comments;

  const sortedComments = useMemo(() => {
    if (comments.length === 0) return [];
    const sorted = [...comments];
    if (sortOrder === "popular") {
      return sorted.sort((a, b) => {
        const aLikes = commentLikes[a.id]?.count || 0;
        const bLikes = commentLikes[b.id]?.count || 0;
        return bLikes - aLikes;
      });
    } else {
      return sorted.sort((a, b) => b.id - a.id);
    }
  }, [comments, sortOrder, commentLikes]);

  const toggleCommentLike = (commentId: number) => {
    setCommentLikes((prev) => {
      const current = prev[commentId] || { liked: false, count: 0 };
      return {
        ...prev,
        [commentId]: {
          liked: !current.liked,
          count: current.liked
            ? Math.max(0, current.count - 1)
            : current.count + 1,
        },
      };
    });
  };

  const toggleCollapse = (commentId: number) => {
    setCollapsedComments((prev) => {
      const newSet = new Set(prev);
      if (newSet.has(commentId)) {
        newSet.delete(commentId);
      } else {
        newSet.add(commentId);
      }
      return newSet;
    });
  };

  const renderComment = (comment: Comment, depth: number = 0) => {
    const isReply = depth > 0;
    const hasReplies = comment.replies && comment.replies.length > 0;
    const isCollapsed = collapsedComments.has(comment.id);
    const isReplying = replyingTo === comment.id;
    const likeInfo = commentLikes[comment.id] || { liked: false, count: 0 };

    return (
      <div key={comment.id} className={isReply ? "ml-8 mt-3" : ""}>
        <div className="flex gap-3">
          <HoverCard openDelay={200} closeDelay={100}>
            <HoverCardTrigger asChild>
              <div>
                <Avatar className="w-8 h-8 flex-shrink-0 cursor-pointer">
                  <AvatarFallback className="bg-white">
                    <User className="w-4 h-4 text-gray-400" />
                  </AvatarFallback>
                </Avatar>
              </div>
            </HoverCardTrigger>
            <HoverCardContent className="w-80 bg-white/95 backdrop-blur-xl border border-gray-200">
              <div className="flex gap-4">
                <Avatar className="w-14 h-14 border-2 border-white shadow-lg bg-white">
                  <AvatarFallback className="bg-white">
                    <User className="w-7 h-7 text-gray-400" />
                  </AvatarFallback>
                </Avatar>
                <div className="space-y-2 flex-1">
                  <h4 className="text-base font-black text-gray-900">
                    {comment.author}
                  </h4>
                  <p className="text-sm text-gray-700 font-medium">
                    {comment.department || "댓글 작성자"}
                  </p>
                  <div className="pt-1 space-y-1.5">
                    <div className="flex justify-between text-xs">
                      <span className="text-gray-600 font-medium">
                        이번 달 활동
                      </span>
                      <span className="text-gray-900 font-bold">
                        {comment.activeDays || 0}일
                      </span>
                    </div>
                    <div className="flex justify-between text-xs">
                      <span className="text-gray-600 font-medium">
                        평균 점수
                      </span>
                      <span className="text-gray-900 font-bold">
                        {comment.avgScore || 0}점
                      </span>
                    </div>
                    <div className="flex justify-between text-xs">
                      <span className="text-gray-600 font-medium">총 점수</span>
                      <span className="text-[#C93831] font-bold">
                        {comment.points || 0}점
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            </HoverCardContent>
          </HoverCard>
          <div className="flex-1 min-w-0">
            <div className="flex items-center gap-2 mb-1">
              <span className="font-bold text-sm text-gray-900">
                {comment.author}
              </span>
              <span className="text-xs text-gray-900">{comment.time}</span>
            </div>
            <p className="text-sm text-gray-900 break-words mb-2">
              {comment.content}
            </p>

            <div className="flex items-center gap-4 mb-2">
              <button
                onClick={() => toggleCommentLike(comment.id)}
                className="flex items-center gap-1 hover:opacity-70 transition-opacity"
              >
                <Heart
                  className={`w-4 h-4 ${
                    likeInfo.liked
                      ? "fill-red-500 text-red-500"
                      : "text-gray-600"
                  }`}
                />
                {likeInfo.count > 0 && (
                  <span className="text-xs text-gray-600 font-semibold">
                    {likeInfo.count}
                  </span>
                )}
              </button>
              {depth === 0 && (
                <button
                  onClick={() => setReplyingTo(isReplying ? null : comment.id)}
                  className="text-xs text-gray-600 hover:text-[#C93831] font-semibold"
                >
                  답글
                </button>
              )}
            </div>

            {isReplying && (
              <div className="mb-3">
                <input
                  type="text"
                  placeholder="답글을 입력하세요..."
                  value={replyCommentText}
                  onChange={(e) => setReplyCommentText(e.target.value)}
                  onKeyPress={(e) => e.key === "Enter" && handleSendReply()}
                  style={{
                    width: "100%",
                    padding: "0.5rem 0",
                    fontSize: "0.875rem",
                    background: "transparent",
                    border: "none",
                    borderBottom: "2px solid #d1d5db",
                    outline: "none",
                    transition: "border-color 0.2s",
                  }}
                  onFocus={(e) =>
                    (e.target.style.borderBottomColor = "#C93831")
                  }
                  onBlur={(e) => (e.target.style.borderBottomColor = "#d1d5db")}
                  autoFocus
                />
                <div className="flex gap-2 mt-2">
                  <button
                    onClick={() => {
                      setReplyingTo(null);
                      setReplyCommentText("");
                    }}
                    className="px-3 py-1 text-xs font-semibold text-gray-600 hover:text-gray-900"
                  >
                    취소
                  </button>
                  <button
                    onClick={handleSendReply}
                    disabled={!replyCommentText.trim()}
                    className="px-3 py-1 text-xs font-semibold text-[#C93831] hover:text-[#B02F28] disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    답글
                  </button>
                </div>
              </div>
            )}

            {hasReplies && (
              <button
                onClick={() => toggleCollapse(comment.id)}
                className="text-xs text-[#C93831] hover:text-[#B02F28] font-semibold flex items-center gap-1 mb-2"
              >
                {isCollapsed ? "▶" : "▼"} 답글 {comment.replies!.length}개
              </button>
            )}
          </div>
        </div>

        {hasReplies && !isCollapsed && (
          <div className="relative mt-2 pl-2 border-l-2 border-gray-300">
            {comment.replies!.map((reply) => renderComment(reply, depth + 1))}
          </div>
        )}
      </div>
    );
  };

  const hasImages = feed.images && feed.images.length > 0;

  return (
    <div className="flex items-center justify-center">
      <div
        className={`h-full max-h-[95vh] overflow-hidden backdrop-blur-2xl bg-white/60 border border-gray-200/30 shadow-2xl flex transition-all duration-300 ${
          showComments
            ? "!w-[825px] !max-w-[825px]"
            : "!w-[475px] !max-w-[475px]"
        }`}
        style={{
          borderRadius: 0,
          width: showComments ? "825px" : "475px",
          maxWidth: showComments ? "825px" : "475px",
        }}
      >
        {/* Main Feed Content (Left) */}
        <div className="w-[475px] max-w-[475px] flex-shrink-0 flex flex-col overflow-hidden">
          {hasImages ? (
            <>
              {/* Image Carousel */}
              <div className="relative h-[545px] w-full overflow-hidden">
                <img
                  src={feed.images[currentIndex] || feed.images[0]}
                  alt={feed.activity}
                  className="w-full h-full object-cover"
                  style={{ maxWidth: "475px", width: "475px", height: "545px" }}
                />

                {feed.images.length > 1 && (
                  <>
                    {currentIndex > 0 && (
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          setFeedImageIndex(
                            feed.id,
                            Math.max(0, currentIndex - 1)
                          );
                        }}
                        className="absolute left-2 top-1/2 -translate-y-1/2 hover:opacity-70 transition-opacity"
                      >
                        <ChevronLeft
                          className={`w-8 h-8 ${
                            iconColor === "white" ? "text-white" : "text-black"
                          }`}
                        />
                      </button>
                    )}
                    {currentIndex < feed.images.length - 1 && (
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          setFeedImageIndex(
                            feed.id,
                            Math.min(feed.images.length - 1, currentIndex + 1)
                          );
                        }}
                        className="absolute right-2 top-1/2 -translate-y-1/2 hover:opacity-70 transition-opacity"
                      >
                        <ChevronRight
                          className={`w-8 h-8 ${
                            iconColor === "white" ? "text-white" : "text-black"
                          }`}
                        />
                      </button>
                    )}
                    <div className="absolute bottom-2 left-1/2 -translate-x-1/2 flex gap-1">
                      {feed.images.map((_, idx) => (
                        <div
                          key={idx}
                          className={`w-1.5 h-1.5 rounded-full ${
                            idx === currentIndex ? "bg-white" : "bg-white/50"
                          }`}
                        ></div>
                      ))}
                    </div>
                  </>
                )}

                {/* Author Avatar Only */}
                <div className="absolute top-4 left-4">
                  <HoverCard openDelay={200} closeDelay={100}>
                    <HoverCardTrigger asChild>
                      <div>
                        <Avatar className="w-10 h-10 border-2 border-white shadow-lg cursor-pointer">
                          <AvatarFallback className="bg-white">
                            <User className="w-5 h-5 text-gray-400" />
                          </AvatarFallback>
                        </Avatar>
                      </div>
                    </HoverCardTrigger>
                    <HoverCardContent className="w-80 bg-white/95 backdrop-blur-xl border border-gray-200">
                      <div className="flex gap-4">
                        <Avatar className="w-14 h-14 border-2 border-white shadow-lg bg-white">
                          <AvatarFallback className="bg-white">
                            <User className="w-7 h-7 text-gray-400" />
                          </AvatarFallback>
                        </Avatar>
                        <div className="space-y-2 flex-1">
                          <h4 className="text-base font-black text-gray-900">
                            {feed.author}
                          </h4>
                          <p className="text-sm text-gray-700 font-medium">
                            {feed.department || "운동 활동"}
                          </p>
                          <div className="pt-1 space-y-1.5">
                            <div className="flex justify-between text-xs">
                              <span className="text-gray-600 font-medium">
                                이번 달 활동
                              </span>
                              <span className="text-gray-900 font-bold">
                                {feed.activeDays || 0}일
                              </span>
                            </div>
                            <div className="flex justify-between text-xs">
                              <span className="text-gray-600 font-medium">
                                평균 점수
                              </span>
                              <span className="text-gray-900 font-bold">
                                {feed.avgScore || 0}점
                              </span>
                            </div>
                            <div className="flex justify-between text-xs">
                              <span className="text-gray-600 font-medium">
                                총 점수
                              </span>
                              <span className="text-[#C93831] font-bold">
                                {feed.points}점
                              </span>
                            </div>
                          </div>
                        </div>
                      </div>
                    </HoverCardContent>
                  </HoverCard>
                </div>

                {/* Right Actions */}
                <div className="absolute right-4 bottom-4 flex flex-col gap-4 z-10">
                  <button
                    onClick={() => handleLike(feed.id)}
                    className="flex flex-col items-center gap-1 group"
                  >
                    <div className="w-12 h-12 rounded-full flex items-center justify-center hover:scale-110 transition-transform">
                      <Heart
                        className={`w-6 h-6 ${
                          liked
                            ? "fill-red-500 text-red-500"
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
                    className="flex flex-col items-center gap-1 group"
                    onClick={() => setShowComments(!showComments)}
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
                      {totalCommentCount}
                    </span>
                  </button>
                </div>
              </div>
            </>
          ) : (
            <>
              {/* No Image Layout - Avatar and Buttons */}
              <div className="relative p-6 bg-transparent h-[545px]">
                <HoverCard openDelay={200} closeDelay={100}>
                  <HoverCardTrigger asChild>
                    <div>
                      <Avatar className="w-10 h-10 border-2 border-gray-300 shadow-lg cursor-pointer">
                        <AvatarFallback className="bg-white">
                          <User className="w-5 h-5 text-gray-400" />
                        </AvatarFallback>
                      </Avatar>
                    </div>
                  </HoverCardTrigger>
                  <HoverCardContent className="w-80 bg-white/95 backdrop-blur-xl border border-gray-200">
                    <div className="flex gap-4">
                      <Avatar className="w-14 h-14 border-2 border-white shadow-lg bg-white">
                        <AvatarFallback className="bg-white">
                          <User className="w-7 h-7 text-gray-400" />
                        </AvatarFallback>
                      </Avatar>
                      <div className="space-y-2 flex-1">
                        <h4 className="text-base font-black text-gray-900">
                          {feed.author}
                        </h4>
                        <p className="text-sm text-gray-700 font-medium">
                          {feed.department || "운동 활동"}
                        </p>
                        <div className="pt-1 space-y-1.5">
                          <div className="flex justify-between text-xs">
                            <span className="text-gray-600 font-medium">
                              이번 달 활동
                            </span>
                            <span className="text-gray-900 font-bold">
                              {feed.activeDays || 0}일
                            </span>
                          </div>
                          <div className="flex justify-between text-xs">
                            <span className="text-gray-600 font-medium">
                              평균 점수
                            </span>
                            <span className="text-gray-900 font-bold">
                              {feed.avgScore || 0}점
                            </span>
                          </div>
                          <div className="flex justify-between text-xs">
                            <span className="text-gray-600 font-medium">
                              총 점수
                            </span>
                            <span className="text-[#C93831] font-bold">
                              {feed.points}점
                            </span>
                          </div>
                        </div>
                      </div>
                    </div>
                  </HoverCardContent>
                </HoverCard>

                {/* Right Actions for No-Image Posts */}
                <div className="absolute right-4 bottom-4 flex flex-col gap-4 z-10">
                  <button
                    onClick={() => handleLike(feed.id)}
                    className="flex flex-col items-center gap-1 group"
                  >
                    <div className="w-12 h-12 rounded-full flex items-center justify-center hover:scale-110 transition-transform">
                      <Heart
                        className={`w-6 h-6 ${
                          liked
                            ? "fill-red-500 text-red-500"
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
                    className="flex flex-col items-center gap-1 group"
                    onClick={() => setShowComments(!showComments)}
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
                      {totalCommentCount}
                    </span>
                  </button>
                </div>
              </div>
            </>
          )}

          {/* Feed Content */}
          <div
            className="p-6 space-y-3 flex-1 overflow-auto bg-transparent"
            style={{ width: "475px", maxWidth: "475px" }}
          >
            <style>{`
              .bn-container {
                max-width: 427px !important;
                width: 427px !important;
                background: transparent !important;
              }
              .bn-editor {
                max-width: 427px !important;
                width: 427px !important;
                padding: 0 !important;
                background: transparent !important;
              }
              .bn-block-content {
                max-width: 427px !important;
              }
              .ProseMirror {
                background: transparent !important;
                color: #111827 !important;
              }
              .ProseMirror p, .ProseMirror h1, .ProseMirror h2, .ProseMirror h3, .ProseMirror h4, .ProseMirror h5, .ProseMirror h6, .ProseMirror li, .ProseMirror span {
                color: #111827 !important;
              }
            `}</style>
            <div className="space-y-3" style={{ maxWidth: "427px" }}>
              <div className="flex items-start justify-between gap-3">
                {/* Left: Badges */}
                <div className="flex items-center gap-2 flex-wrap">
                  <Badge className="bg-gradient-to-r from-yellow-400 to-orange-500 text-white px-3 py-1 font-bold border-0">
                    <Sparkles className="w-3 h-3 mr-1" />+{feed.points}
                  </Badge>
                  <Badge className="bg-white text-blue-700 px-3 py-1 font-bold text-xs border-0">
                    {feed.activity}
                  </Badge>
                  {feed.stats?.calories && (
                    <Badge className="bg-white text-orange-700 px-3 py-1 font-bold text-xs border-0">
                      {feed.stats.calories}
                    </Badge>
                  )}
                  {feed.streak && (
                    <Badge className="bg-white text-red-700 px-3 py-1 font-bold text-xs border-0">
                      {feed.streak}일 연속
                    </Badge>
                  )}
                </div>

                {/* Right: Time & Edited */}
                <div className="flex items-center gap-1.5 flex-shrink-0">
                  <Badge className="bg-white text-gray-700 px-3 py-1 font-bold text-xs flex items-center gap-1 border-0">
                    {feed.edited && <Pencil className="w-3 h-3" />}
                    {feed.time}
                  </Badge>
                </div>
              </div>

              <div className="text-gray-900 font-medium text-sm leading-relaxed">
                <BlockNoteView editor={editor} editable={false} theme="light" />
              </div>
            </div>
          </div>
        </div>

        {/* Comments Panel (Right - slides in) */}
        {showComments && (
          <div className="flex-1 bg-transparent border-l border-gray-200/30 flex flex-col overflow-hidden">
            {/* Comments Header */}
            <div className="px-6 py-4 border-b border-gray-200/30 flex items-center justify-between bg-transparent">
              <h3 className="text-lg font-bold text-gray-900">
                댓글 {totalCommentCount}개
              </h3>
              <div className="relative">
                <button
                  onClick={() => setShowSortMenu(!showSortMenu)}
                  className="flex items-center gap-1 px-3 py-1.5 rounded-lg hover:bg-white/10 transition-colors"
                >
                  <ArrowUpDown className="w-4 h-4 text-gray-900" />
                  <span className="text-sm font-semibold text-gray-900">
                    {sortOrder === "latest" ? "최신순" : "인기순"}
                  </span>
                </button>
                {showSortMenu && (
                  <div className="absolute right-0 top-full mt-1 bg-white/70 backdrop-blur-md border border-gray-200/50 rounded-lg shadow-lg overflow-hidden z-50">
                    <button
                      onClick={() => {
                        setSortOrder("latest");
                        setShowSortMenu(false);
                      }}
                      className={`w-full px-4 py-2 text-left text-sm hover:bg-white/20 transition-colors ${
                        sortOrder === "latest"
                          ? "bg-white/15 font-semibold text-[#C93831]"
                          : "text-gray-900"
                      }`}
                    >
                      최신순
                    </button>
                    <button
                      onClick={() => {
                        setSortOrder("popular");
                        setShowSortMenu(false);
                      }}
                      className={`w-full px-4 py-2 text-left text-sm hover:bg-white/20 transition-colors ${
                        sortOrder === "popular"
                          ? "bg-white/15 font-semibold text-[#C93831]"
                          : "text-gray-900"
                      }`}
                    >
                      인기순
                    </button>
                  </div>
                )}
              </div>
            </div>

            {/* Comments List */}
            <div className="flex-1 overflow-hidden">
              <ScrollArea className="h-full">
                <div className="space-y-4 px-6 pt-4 pb-4">
                  {comments.length === 0 ? (
                    <div className="text-center text-gray-500 text-sm py-8">
                      첫 댓글을 남겨보세요!
                    </div>
                  ) : (
                    sortedComments.map((comment) => renderComment(comment))
                  )}
                </div>
              </ScrollArea>
            </div>

            {/* Comment Input */}
            <div className="p-4 border-t border-gray-200/30 bg-transparent">
              <div className="flex gap-2 items-center">
                <div className="relative flex-1">
                  <input
                    type="text"
                    placeholder="댓글을 입력하세요..."
                    value={commentText}
                    onChange={(e) => setCommentText(e.target.value)}
                    onKeyPress={(e) => {
                      if (e.key === "Enter") {
                        handleSendComment();
                      }
                    }}
                    style={{
                      width: "100%",
                      padding: "0.5rem 0",
                      paddingRight: commentText ? "2.5rem" : "0.5rem",
                      fontSize: "0.875rem",
                      background: "transparent",
                      border: "none",
                      borderBottom: "2px solid #d1d5db",
                      outline: "none",
                      transition: "border-color 0.2s, padding-right 0.2s",
                    }}
                    onFocus={(e) =>
                      (e.target.style.borderBottomColor = "#C93831")
                    }
                    onBlur={(e) =>
                      (e.target.style.borderBottomColor = "#d1d5db")
                    }
                  />
                  {commentText && (
                    <button
                      onClick={() => setCommentText("")}
                      className="absolute right-0 top-1/2 -translate-y-1/2 w-5 h-5 rounded-full bg-gray-200 hover:bg-gray-300 flex items-center justify-center transition-colors"
                    >
                      <X className="w-3 h-3 text-gray-600" />
                    </button>
                  )}
                </div>
                <button
                  onClick={handleSendComment}
                  disabled={!commentText.trim()}
                  className="w-10 h-10 rounded-full bg-gradient-to-r from-[#C93831] to-[#B02F28] text-white flex items-center justify-center hover:shadow-lg transition-shadow disabled:opacity-50 disabled:cursor-not-allowed flex-shrink-0"
                >
                  <Send className="w-4 h-4" />
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

export default function FeedView({
  allFeeds,
  searchQuery,
  setSearchQuery,
  getFeedImageIndex,
  setFeedImageIndex,
  hasLiked,
  handleLike,
  feedContainerRef,
  scrollToFeedId,
  setScrollToFeedId,
  loadMoreFeeds,
  hasMoreFeeds,
  isLoadingFeeds,
}: FeedViewProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const [containerSize, setContainerSize] = useState({ width: 0, height: 0 });
  const listRef = useRef<HTMLDivElement>(null);

  // 필터링된 피드 목록
  const filteredFeeds = useMemo(() =>
    allFeeds.filter((feed) =>
      feed.author.toLowerCase().includes(searchQuery.toLowerCase())
    ),
    [allFeeds, searchQuery]
  );

  // 컨테이너 크기 측정
  useEffect(() => {
    const updateSize = () => {
      if (containerRef.current) {
        setContainerSize({
          width: containerRef.current.clientWidth,
          height: containerRef.current.clientHeight
        });
      }
    };

    updateSize();
    window.addEventListener('resize', updateSize);
    return () => window.removeEventListener('resize', updateSize);
  }, []);

  // 그리드 설정: 한 행에 5개씩 표시
  const COLUMNS = 5;
  const rowCount = Math.ceil(filteredFeeds.length / COLUMNS);

  // 특정 피드로 스크롤
  useEffect(() => {
    if (scrollToFeedId && listRef.current) {
      const feedElement = listRef.current.querySelector(`[data-feed-id="${scrollToFeedId}"]`);
      if (feedElement) {
        feedElement.scrollIntoView({ behavior: "smooth", block: "center" });
        setScrollToFeedId(null);
      }
    }
  }, [scrollToFeedId, setScrollToFeedId]);

  // 무한 스크롤을 위한 스크롤 감지
  const handleScroll = useCallback(() => {
    if (!listRef.current) return;
    const { scrollTop, scrollHeight, clientHeight } = listRef.current;
    // 하단 200px 이내에 도달하면 더 로드
    if (scrollHeight - scrollTop - clientHeight < 200 && hasMoreFeeds && !isLoadingFeeds) {
      loadMoreFeeds();
    }
  }, [hasMoreFeeds, isLoadingFeeds, loadMoreFeeds]);

  // 각 행 렌더링 (5개의 카드)
  const renderRow = (index: number) => {
    const startIdx = index * COLUMNS;
    const rowFeeds = filteredFeeds.slice(startIdx, startIdx + COLUMNS);

    return (
      <div key={index} className="flex justify-center gap-0">
        {rowFeeds.map((feed) => {
          const currentIndex = getFeedImageIndex(feed.id);
          const liked = hasLiked(feed.id);

          return (
            <div key={feed.id} data-feed-id={feed.id} className="flex-shrink-0">
              <FeedCard
                feed={feed}
                currentIndex={currentIndex}
                liked={liked}
                setFeedImageIndex={setFeedImageIndex}
                handleLike={handleLike}
              />
            </div>
          );
        })}
        {/* 마지막 행이 5개 미만일 때 빈 공간 채우기 */}
        {rowFeeds.length < COLUMNS && Array(COLUMNS - rowFeeds.length).fill(0).map((_, i) => (
          <div key={`empty-${i}`} style={{ width: "475px", flexShrink: 0 }} />
        ))}
      </div>
    );
  };

  return (
    <div className="h-full relative">
      <div
        ref={(el) => {
          containerRef.current = el;
          if (feedContainerRef) {
            (feedContainerRef as React.MutableRefObject<HTMLDivElement | null>).current = el;
          }
        }}
        className="h-full w-full"
      >
        <style>{`
          .feed-grid-list {
            scrollbar-width: thin;
            scrollbar-color: rgba(201, 56, 49, 0.3) transparent;
          }
          .feed-grid-list::-webkit-scrollbar {
            width: 6px;
          }
          .feed-grid-list::-webkit-scrollbar-track {
            background: transparent;
          }
          .feed-grid-list::-webkit-scrollbar-thumb {
            background: rgba(201, 56, 49, 0.3);
            border-radius: 3px;
          }
          .feed-grid-list::-webkit-scrollbar-thumb:hover {
            background: rgba(201, 56, 49, 0.5);
          }
        `}</style>

        {/* Search Bar - Fixed at top */}
        <div className="sticky top-0 z-30 flex justify-center px-4 py-3 bg-gradient-to-b from-gray-50 to-transparent">
          <div className="w-full max-w-2xl">
            <SearchInput
              value={searchQuery}
              onChange={setSearchQuery}
              placeholder="작성자 이름으로 검색..."
              suggestions={[...new Set(allFeeds.map((feed) => feed.author))]}
            />
          </div>
        </div>

        {/* Grid Feed List */}
        <div
          ref={listRef}
          className="feed-grid-list overflow-auto"
          style={{ height: containerSize.height - 60 }}
          onScroll={handleScroll}
        >
          {Array.from({ length: rowCount }, (_, i) => renderRow(i))}
        </div>

        {/* Loading indicator */}
        {isLoadingFeeds && (
          <div className="absolute bottom-4 left-1/2 -translate-x-1/2 z-20">
            <div className="w-8 h-8 border-4 border-gray-300 border-t-[#C93831] rounded-full animate-spin"></div>
          </div>
        )}
      </div>
    </div>
  );
}
