/**
 * Feed.tsx
 *
 * 피드 페이지 컴포넌트
 * - 세로 스크롤 + 한 개씩 표시 (쇼츠 스타일)
 * - FeedV2 디자인 적용
 */

import React, { useState, useEffect, useRef, useCallback, useMemo } from "react";
import { SearchInput } from "@/components/molecules";
import { Feed, Comment } from "@/types/dashboard.types";
import { getRelativeTime, parseBlockNoteContent } from "@/lib/utils";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Badge } from "@/components/ui/badge";
import {
  Heart,
  MessageCircle,
  Sparkles,
  ChevronLeft,
  ChevronRight,
  Send,
  X,
  ArrowUpDown,
  User,
  Siren,
  Flame,
  Clock,
  Zap,
} from "lucide-react";
import { commentApi, reportApi, getCdnUrl } from "@/api";
import { toast } from "sonner";
import { useImageBrightness } from "@/hooks";

interface FeedViewProps {
  allFeeds: Feed[];
  searchQuery: string;
  setSearchQuery: React.Dispatch<React.SetStateAction<string>>;
  showSearch: boolean;
  setShowSearch: React.Dispatch<React.SetStateAction<boolean>>;
  getFeedImageIndex: (feedId: number) => number;
  setFeedImageIndex: (feedId: number, updater: number | ((prev: number) => number)) => void;
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
 * 댓글 패널 컴포넌트
 */
function CommentPanel({ feedId, onClose }: { feedId: number; onClose?: () => void }) {
  const [commentText, setCommentText] = useState("");
  const [replyCommentText, setReplyCommentText] = useState("");
  const [comments, setComments] = useState<Comment[]>([]);
  const [replyingTo, setReplyingTo] = useState<number | null>(null);
  const [collapsedComments, setCollapsedComments] = useState<Set<number>>(new Set());
  const [commentLikes, setCommentLikes] = useState<{ [key: number]: { liked: boolean; count: number } }>({});
  const [sortOrder, setSortOrder] = useState<"latest" | "popular">("latest");
  const [showSortMenu, setShowSortMenu] = useState(false);
  const [commentReported, setCommentReported] = useState<Record<number, boolean>>({});

  const currentUserName = localStorage.getItem("userName") || "알 수 없음";
  const currentUserId = parseInt(localStorage.getItem("userId") || "1");

  // 아바타 URL 생성 헬퍼 (CDN 사용)
  const getAvatarUrl = (avatarUrl?: string): string => {
    if (!avatarUrl) return "";
    return getCdnUrl(avatarUrl);
  };

  // 댓글 로드
  useEffect(() => {
    const fetchComments = async () => {
      try {
        const response = await commentApi.getCommentsByFeedId(feedId, 0, 100);
        const commentList = response.content || response;

        const commentsWithReplies = await Promise.all(
          commentList.map(async (comment: { id: number; writerName?: string; writerAvatar?: string; createdAt?: string }) => {
            try {
              const repliesData = await commentApi.getRepliesByCommentId(comment.id);
              const replies = (repliesData || []).map((reply: { writerName?: string; writerAvatar?: string; createdAt?: string }) => ({
                ...reply,
                author: reply.writerName || "알 수 없음",
                avatar: getAvatarUrl(reply.writerAvatar),
                time: getRelativeTime(reply.createdAt || new Date().toISOString()),
              }));
              return {
                ...comment,
                author: comment.writerName || "알 수 없음",
                avatar: getAvatarUrl(comment.writerAvatar),
                time: getRelativeTime(comment.createdAt || new Date().toISOString()),
                replies,
              };
            } catch {
              return {
                ...comment,
                author: comment.writerName || "알 수 없음",
                avatar: getAvatarUrl(comment.writerAvatar),
                time: getRelativeTime(comment.createdAt || new Date().toISOString()),
                replies: [],
              };
            }
          })
        );
        setComments(commentsWithReplies);

        // commentLikes 상태 초기화 (likeCount, isLiked 반영)
        const likesState: { [key: number]: { liked: boolean; count: number } } = {};
        commentsWithReplies.forEach((comment: { id: number; isLiked?: boolean; likeCount?: number; replies?: { id: number; isLiked?: boolean; likeCount?: number }[] }) => {
          likesState[comment.id] = {
            liked: comment.isLiked || false,
            count: comment.likeCount || 0,
          };
          // 답글의 좋아요 상태도 초기화
          if (comment.replies) {
            comment.replies.forEach((reply) => {
              likesState[reply.id] = {
                liked: reply.isLiked || false,
                count: reply.likeCount || 0,
              };
            });
          }
        });
        setCommentLikes(likesState);
      } catch (error) {
        console.error("댓글 로드 실패:", error);
        setComments([]);
      }
    };
    fetchComments();
  }, [feedId]);

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

  const totalCommentCount = countAllComments(comments);

  const sortedComments = useMemo(() => {
    if (comments.length === 0) return [];
    const sorted = [...comments];
    if (sortOrder === "popular") {
      return sorted.sort((a, b) => {
        const aLikes = commentLikes[a.id]?.count || 0;
        const bLikes = commentLikes[b.id]?.count || 0;
        return bLikes - aLikes;
      });
    }
    return sorted.sort((a, b) => b.id - a.id);
  }, [comments, sortOrder, commentLikes]);

  const toggleCommentLike = async (commentId: number) => {
    const current = commentLikes[commentId] || { liked: false, count: 0 };
    const newLiked = !current.liked;

    // 낙관적 업데이트
    setCommentLikes((prev) => ({
      ...prev,
      [commentId]: {
        liked: newLiked,
        count: newLiked ? current.count + 1 : Math.max(0, current.count - 1),
      },
    }));

    try {
      if (newLiked) {
        await commentApi.likeComment(commentId);
      } else {
        await commentApi.unlikeComment(commentId);
      }
    } catch {
      // 에러 시 롤백
      setCommentLikes((prev) => ({
        ...prev,
        [commentId]: current,
      }));
    }
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

  const handleSendComment = async () => {
    if (commentText.trim()) {
      try {
        const response = await commentApi.createComment({
          content: commentText,
          feedId: feedId,
          writerId: currentUserId,
        });
        const authorName = response.writerName || currentUserName;
        const newComment: Comment = {
          id: response.id,
          author: authorName,
          avatar: getAvatarUrl(response.writerAvatar),
          content: response.content,
          time: "방금 전",
          replies: [],
        };
        setComments([...comments, newComment]);
        setCommentText("");
      } catch (error) {
        console.error("댓글 작성 실패:", error);
        toast.error("댓글 작성에 실패했습니다.");
      }
    }
  };

  const handleSendReply = async () => {
    if (replyCommentText.trim() && replyingTo !== null) {
      try {
        const response = await commentApi.createComment({
          content: replyCommentText,
          feedId: feedId,
          writerId: currentUserId,
          parentId: replyingTo,
        });
        const replyAuthorName = response.writerName || currentUserName;
        const newReply: Comment = {
          id: response.id,
          author: replyAuthorName,
          avatar: getAvatarUrl(response.writerAvatar),
          content: response.content,
          time: "방금 전",
          parentId: replyingTo,
          replies: [],
        };
        setComments(
          comments.map((comment) => {
            if (comment.id === replyingTo) {
              return { ...comment, replies: [...(comment.replies || []), newReply] };
            }
            return comment;
          })
        );
        setReplyCommentText("");
        setReplyingTo(null);
      } catch (error) {
        console.error("답글 작성 실패:", error);
        toast.error("답글 작성에 실패했습니다.");
      }
    }
  };

  const handleDeleteComment = async (commentId: number) => {
    if (!confirm("댓글을 삭제하시겠습니까?")) return;
    try {
      await commentApi.deleteComment(commentId);
      setComments((prevComments) => {
        return prevComments
          .map((c) => {
            if (c.id === commentId) {
              if (c.replies && c.replies.length > 0) {
                return { ...c, author: "", content: "삭제된 댓글입니다.", isDeleted: true };
              }
              return null;
            }
            return { ...c, replies: c.replies?.filter((r) => r.id !== commentId) || [] };
          })
          .filter(Boolean) as Comment[];
      });
    } catch (error) {
      console.error("댓글 삭제 실패:", error);
      toast.error("댓글 삭제에 실패했습니다.");
    }
  };

  const handleReportComment = async (commentId: number) => {
    if (commentReported[commentId]) return;
    try {
      await reportApi.reportComment(commentId);
      setCommentReported((prev) => ({ ...prev, [commentId]: true }));
      toast.success("신고가 접수되었습니다.");
    } catch {
      toast.error("신고에 실패했습니다.");
    }
  };

  const renderComment = (comment: Comment, depth: number = 0) => {
    const hasReplies = comment.replies && comment.replies.length > 0;
    const isCollapsed = collapsedComments.has(comment.id);
    const isReplying = replyingTo === comment.id;
    const likeInfo = commentLikes[comment.id] || { liked: false, count: 0 };
    const isDeleted = comment.isDeleted;

    return (
      <div key={comment.id} className={`transition-colors duration-500 ${depth > 0 ? "ml-8 mt-3" : ""}`}>
        <div className="flex gap-3">
          <Avatar className="w-8 h-8 flex-shrink-0">
            {comment.avatar && comment.avatar.startsWith("http") ? (
              <img src={comment.avatar} alt={comment.author} className="w-full h-full object-cover" />
            ) : (
              <AvatarFallback className="bg-white">
                <User className="w-4 h-4 text-gray-400" />
              </AvatarFallback>
            )}
          </Avatar>
          <div className="flex-1 min-w-0">
            {isDeleted ? (
              <p className="text-sm text-gray-400 italic mb-2">삭제된 댓글입니다.</p>
            ) : (
              <>
                <div className="flex items-center gap-2 mb-1">
                  <span className="font-bold text-sm text-gray-900">{comment.author}</span>
                  <span className="text-xs text-gray-500">{comment.time}</span>
                </div>
                <p className="text-sm text-gray-900 break-words mb-2">{comment.content}</p>
                <div className="flex items-center gap-4 mb-2">
                  <button onClick={() => toggleCommentLike(comment.id)} className="flex items-center gap-1 hover:opacity-70 transition-opacity cursor-pointer">
                    <Heart className={`w-4 h-4 ${likeInfo.liked ? "fill-[#C93831] text-[#C93831]" : "text-gray-600"}`} />
                    {likeInfo.count > 0 && <span className="text-xs text-gray-600 font-semibold">{likeInfo.count}</span>}
                  </button>
                  {depth === 0 && (
                    <button onClick={() => setReplyingTo(replyingTo === comment.id ? null : comment.id)} className="text-xs text-gray-600 hover:text-[#C93831] font-semibold cursor-pointer">
                      답글
                    </button>
                  )}
                  {comment.author === currentUserName && (
                    <button onClick={() => handleDeleteComment(comment.id)} className="text-xs text-gray-600 hover:text-red-500 font-semibold cursor-pointer">
                      삭제
                    </button>
                  )}
                  {comment.author !== currentUserName && (
                    <button onClick={() => handleReportComment(comment.id)} disabled={commentReported[comment.id]} className={`text-xs font-semibold cursor-pointer ${commentReported[comment.id] ? "text-red-500" : "text-gray-600 hover:text-red-500"}`}>
                      {commentReported[comment.id] ? "신고됨" : "신고"}
                    </button>
                  )}
                </div>
              </>
            )}

            {isReplying && (
              <div className="mb-3">
                <input
                  type="text"
                  placeholder="답글을 입력하세요..."
                  value={replyCommentText}
                  onChange={(e) => setReplyCommentText(e.target.value)}
                  onKeyDown={(e) => e.key === "Enter" && handleSendReply()}
                  className="w-full py-2 text-sm bg-transparent border-b-2 border-gray-300 focus:border-[#C93831] outline-none"
                  autoFocus
                />
                <div className="flex gap-2 mt-2">
                  <button onClick={() => { setReplyingTo(null); setReplyCommentText(""); }} className="px-3 py-1 text-xs font-semibold text-gray-600 hover:text-gray-900 cursor-pointer">
                    취소
                  </button>
                  <button onClick={handleSendReply} disabled={!replyCommentText.trim()} className="px-3 py-1 text-xs font-semibold text-[#C93831] hover:text-[#B02F28] disabled:opacity-50 cursor-pointer">
                    답글
                  </button>
                </div>
              </div>
            )}

            {hasReplies && (
              <button onClick={() => toggleCollapse(comment.id)} className="text-xs text-[#C93831] hover:text-[#B02F28] font-semibold flex items-center gap-1 mb-2 cursor-pointer">
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

  return (
    <div className="flex flex-col h-full bg-white/50 backdrop-blur-sm">
      {/* Header */}
      <div className="px-6 py-4 border-b border-gray-200/50 flex items-center justify-between">
        <h3 className="text-lg font-bold text-gray-900">댓글 {totalCommentCount}개</h3>
        <div className="flex items-center gap-2">
          <div className="relative">
            <button onClick={() => setShowSortMenu(!showSortMenu)} className="flex items-center gap-1 px-3 py-1.5 rounded-lg hover:bg-gray-100 transition-colors cursor-pointer">
              <ArrowUpDown className="w-4 h-4 text-gray-900" />
              <span className="text-sm font-semibold text-gray-900">{sortOrder === "latest" ? "최신순" : "인기순"}</span>
            </button>
            {showSortMenu && (
              <div className="absolute right-0 top-full mt-1 bg-white border border-gray-200 rounded-lg shadow-lg overflow-hidden z-50">
                <button
                  onClick={() => { setSortOrder("latest"); setShowSortMenu(false); }}
                  className={`w-full px-4 py-2 text-left text-sm hover:bg-gray-100 transition-colors cursor-pointer ${sortOrder === "latest" ? "bg-gray-50 font-semibold text-[#C93831]" : "text-gray-900"}`}
                >
                  최신순
                </button>
                <button
                  onClick={() => { setSortOrder("popular"); setShowSortMenu(false); }}
                  className={`w-full px-4 py-2 text-left text-sm hover:bg-gray-100 transition-colors cursor-pointer ${sortOrder === "popular" ? "bg-gray-50 font-semibold text-[#C93831]" : "text-gray-900"}`}
                >
                  인기순
                </button>
              </div>
            )}
          </div>
          {onClose && (
            <button
              onClick={onClose}
              className="p-2 hover:bg-gray-100 rounded-full cursor-pointer"
              aria-label="댓글 패널 닫기"
            >
              <X className="w-5 h-5" />
            </button>
          )}
        </div>
      </div>

      {/* Comments List */}
      <div className="flex-1 overflow-hidden">
        <ScrollArea className="h-full">
          <div className="space-y-4 px-6 pt-4 pb-4">
            {comments.length === 0 ? (
              <div className="text-center text-gray-500 text-sm py-8">첫 댓글을 남겨보세요!</div>
            ) : (
              sortedComments.map((comment) => renderComment(comment))
            )}
          </div>
        </ScrollArea>
      </div>

      {/* Comment Input */}
      <div className="p-4 border-t border-gray-200/50">
        <div className="flex gap-2 items-center">
          <div className="relative flex-1">
            <input
              type="text"
              placeholder="댓글을 입력하세요..."
              value={commentText}
              onChange={(e) => setCommentText(e.target.value)}
              onKeyDown={(e) => e.key === "Enter" && handleSendComment()}
              className="w-full py-2 text-sm bg-transparent border-b-2 border-gray-300 focus:border-[#C93831] outline-none pr-8"
            />
            {commentText && (
              <button onClick={() => setCommentText("")} className="absolute right-0 top-1/2 -translate-y-1/2 w-5 h-5 rounded-full bg-gray-200 hover:bg-gray-300 flex items-center justify-center cursor-pointer" aria-label="댓글 지우기">
                <X className="w-3 h-3 text-gray-600" />
              </button>
            )}
          </div>
          <button
            onClick={handleSendComment}
            disabled={!commentText.trim()}
            className="w-10 h-10 rounded-full bg-gradient-to-r from-[#C93831] to-[#B02F28] text-white flex items-center justify-center hover:shadow-lg transition-shadow disabled:opacity-50 disabled:cursor-not-allowed flex-shrink-0 cursor-pointer"
            aria-label="댓글 전송"
          >
            <Send className="w-4 h-4" />
          </button>
        </div>
      </div>
    </div>
  );
}

/**
 * 개별 피드 아이템 컴포넌트
 */
function FeedItem({
  feed,
  currentImageIndex,
  liked,
  onPrevImage,
  onNextImage,
  onLike,
  isPriority = false,
}: {
  feed: Feed;
  currentImageIndex: number;
  liked: boolean;
  onPrevImage: () => void;
  onNextImage: () => void;
  onLike: () => void;
  isPriority?: boolean;
}) {
  const [showComments, setShowComments] = useState(false);
  const [isReported, setIsReported] = useState(false);

  const hasImages = feed.images && feed.images.length > 0;
  const images = feed.images || [];
  const isFirstImage = currentImageIndex === 0;
  const isLastImage = currentImageIndex === images.length - 1;

  // 아바타 URL 생성 헬퍼 (CDN 사용)
  const getAvatarUrl = (avatarUrl?: string): string => {
    if (!avatarUrl) return "";
    return getCdnUrl(avatarUrl);
  };
  const writerAvatarUrl = getAvatarUrl(feed.writerAvatar);

  // 이미지 밝기에 따른 아이콘 색상 결정 (CDN URL 사용)
  const currentImageUrl = hasImages ? getCdnUrl(images[currentImageIndex]) : undefined;
  const iconColor = useImageBrightness(currentImageUrl);
  const iconColorClass = iconColor === "white" ? "text-white" : "text-gray-900";

  const handleReport = async () => {
    try {
      await reportApi.reportFeed(feed.id);
      setIsReported(!isReported);
      toast.success(isReported ? "신고가 취소되었습니다." : "신고가 접수되었습니다.");
    } catch {
      toast.error(isReported ? "신고 취소에 실패했습니다." : "신고에 실패했습니다.");
    }
  };

  return (
    <div className={`h-full max-h-[calc(100vh-130px)] md:max-h-full w-fit mx-auto flex shadow-xl rounded-2xl overflow-hidden transition-all duration-300 relative`}>
      {/* 피드 카드 (왼쪽) */}
      <div className="h-full aspect-[9/16] max-w-[calc(100vw-32px)] flex flex-col flex-shrink-0">
        {/* 이미지 영역 - 57% */}
        <div className="relative h-[57%]">
          {hasImages ? (
            <img
              src={getCdnUrl(images[currentImageIndex] || images[0])}
              alt={feed.activity}
              className="w-full h-full object-cover"
              loading={isPriority ? "eager" : "lazy"}
              fetchPriority={isPriority ? "high" : "auto"}
            />
          ) : (
            <div className="w-full h-full bg-gradient-to-br from-gray-100 to-gray-200 flex items-center justify-center">
              <p className="text-gray-500 font-medium text-lg">{feed.activity}</p>
            </div>
          )}

          {/* 이미지 네비게이션 */}
          {hasImages && images.length > 1 && (
            <>
              {!isFirstImage && (
                <button
                  className="absolute left-2 top-1/2 -translate-y-1/2 cursor-pointer hover:scale-110 transition-transform"
                  onClick={onPrevImage}
                  aria-label="이전 이미지"
                >
                  <ChevronLeft className={`w-8 h-8 ${iconColorClass}`} />
                </button>
              )}
              {!isLastImage && (
                <button
                  className="absolute right-2 top-1/2 -translate-y-1/2 cursor-pointer hover:scale-110 transition-transform"
                  onClick={onNextImage}
                  aria-label="다음 이미지"
                >
                  <ChevronRight className={`w-8 h-8 ${iconColorClass}`} />
                </button>
              )}

              {/* 인디케이터 */}
              <div className="absolute bottom-2 left-1/2 -translate-x-1/2 flex gap-1">
                {images.map((_, index) => (
                  <div
                    key={index}
                    className={`w-1.5 h-1.5 rounded-full ${
                      index === currentImageIndex
                        ? (iconColor === "white" ? "bg-white" : "bg-gray-900")
                        : (iconColor === "white" ? "bg-white/50" : "bg-gray-900/50")
                    }`}
                  />
                ))}
              </div>
            </>
          )}

          {/* 작성자 */}
          <Avatar className="absolute top-4 left-4 w-10 h-10 border-2 border-white shadow-lg">
            {writerAvatarUrl ? (
              <img src={writerAvatarUrl} alt={feed.writerName} className="w-full h-full object-cover" />
            ) : (
              <AvatarFallback className="bg-white">
                <User className="w-5 h-5 text-gray-400" />
              </AvatarFallback>
            )}
          </Avatar>

          {/* 액션 버튼 */}
          <div className="absolute right-4 bottom-4 flex flex-col gap-4">
            <button
              className="flex flex-col items-center gap-1 cursor-pointer hover:scale-110 transition-transform"
              onClick={onLike}
              aria-label={liked ? "좋아요 취소" : "좋아요"}
            >
              <Heart className={`w-6 h-6 ${liked ? "fill-[#C93831] text-[#C93831]" : iconColorClass}`} />
              <span className={`text-xs font-bold ${iconColorClass}`}>{feed.likes}</span>
            </button>
            <button
              className="flex flex-col items-center gap-1 cursor-pointer hover:scale-110 transition-transform"
              onClick={() => setShowComments(!showComments)}
              aria-label={showComments ? "댓글 닫기" : "댓글 보기"}
            >
              <MessageCircle className={`w-6 h-6 ${iconColorClass} ${showComments ? (iconColor === "white" ? "fill-white" : "fill-gray-900") : ""}`} />
              <span className={`text-xs font-bold ${iconColorClass}`}>{feed.comments || 0}</span>
            </button>
            <button
              className="flex flex-col items-center gap-1 cursor-pointer hover:scale-110 transition-transform"
              onClick={handleReport}
              aria-label={isReported ? "신고 취소" : "피드 신고"}
            >
              <Siren className={`w-6 h-6 ${isReported ? "text-[#C93831] fill-[#C93831]" : iconColorClass}`} />
            </button>
          </div>
        </div>

        {/* 콘텐츠 영역 - 43% */}
        <ScrollArea className="h-[43%] bg-white/50 backdrop-blur-sm">
          <div className="p-6 space-y-3">
            {/* 뱃지 */}
            <div className="flex items-center justify-between">
              <div className="flex gap-2 flex-wrap">
                <Badge className="bg-amber-50 text-amber-600 font-medium border border-amber-200">
                  <Sparkles className="w-3 h-3 mr-1" />+{feed.points}
                </Badge>
                <Badge className="bg-blue-50 text-blue-600 font-medium border border-blue-200">
                  <Zap className="w-3 h-3 mr-1" />{feed.activity}
                </Badge>
                {feed.calories && (
                  <Badge className="bg-orange-50 text-orange-600 font-medium border border-orange-200">
                    <Flame className="w-3 h-3 mr-1" />{feed.calories}kcal
                  </Badge>
                )}
              </div>
              <Badge className="bg-slate-50 text-slate-500 font-medium border border-slate-200">
                <Clock className="w-3 h-3 mr-1" />{feed.time}
              </Badge>
            </div>

            {/* 본문 */}
            <p className="text-gray-900 text-sm">{parseBlockNoteContent(feed.content)}</p>
          </div>
        </ScrollArea>
      </div>

      {/* 댓글 패널 - 모바일: 전체화면 오버레이, 데스크톱: 옆에 표시 */}
      {showComments && (
        <>
          {/* 모바일용 전체화면 오버레이 (하단 네비 제외) */}
          <div className="md:hidden fixed inset-x-0 top-0 bottom-[60px] z-50 bg-white flex flex-col">
            <CommentPanel feedId={feed.id} onClose={() => setShowComments(false)} />
          </div>
          {/* 데스크톱용 사이드 패널 */}
          <div className="hidden md:block h-full aspect-[7/16] border-l border-gray-200/50 flex-shrink-0">
            <CommentPanel feedId={feed.id} />
          </div>
        </>
      )}
    </div>
  );
}

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
  loadMoreFeeds,
  hasMoreFeeds,
  isLoadingFeeds,
}: FeedViewProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const scrollRef = useRef<HTMLDivElement>(null);

  // [최적화] LCP 이미지 Preload - 첫 번째 피드 이미지
  useEffect(() => {
    if (allFeeds.length > 0 && allFeeds[0].images?.[0]) {
      const link = document.createElement('link');
      link.rel = 'preload';
      link.as = 'image';
      link.href = getCdnUrl(allFeeds[0].images[0]);
      document.head.appendChild(link);
      return () => link.remove();
    }
  }, [allFeeds]);

  // 필터링된 피드
  const filteredFeeds = useMemo(() => {
    if (!searchQuery.trim()) return allFeeds;
    return allFeeds.filter((feed) => {
      const authorName = feed.author || feed.writerName;
      return authorName.toLowerCase().includes(searchQuery.toLowerCase());
    });
  }, [allFeeds, searchQuery]);

  // 특정 피드로 스크롤
  useEffect(() => {
    if (scrollToFeedId && scrollRef.current) {
      const feedElement = scrollRef.current.querySelector(`[data-feed-id="${scrollToFeedId}"]`);
      if (feedElement) {
        feedElement.scrollIntoView({ behavior: "smooth", block: "start" });
        setScrollToFeedId(null);
      }
    }
  }, [scrollToFeedId, setScrollToFeedId]);

  // 무한 스크롤
  const handleScroll = useCallback(() => {
    if (!scrollRef.current) return;
    const { scrollTop, scrollHeight, clientHeight } = scrollRef.current;
    if (scrollHeight - scrollTop - clientHeight < 200 && hasMoreFeeds && !isLoadingFeeds) {
      loadMoreFeeds();
    }
  }, [hasMoreFeeds, isLoadingFeeds, loadMoreFeeds]);

  return (
    <div
      ref={(el) => {
        containerRef.current = el;
        if (feedContainerRef) {
          // eslint-disable-next-line react-hooks/immutability -- 부모 컴포넌트에 ref 전달 필요
          (feedContainerRef as React.MutableRefObject<HTMLDivElement | null>).current = el;
        }
      }}
      className="h-full flex flex-col p-4 gap-4"
    >
      {/* 검색바 */}
      <div className="mx-auto max-w-2xl w-full flex-shrink-0">
        <SearchInput
          value={searchQuery}
          onChange={setSearchQuery}
          placeholder="작성자 이름으로 검색..."
          suggestions={[...new Set(allFeeds.map((feed) => feed.author || feed.writerName))]}
        />
      </div>

      {/* 피드 리스트 - 쇼츠 스타일 스크롤 */}
      <div
        ref={scrollRef}
        className="flex-1 overflow-y-auto snap-y snap-mandatory scrollbar-hide flex flex-col gap-4 pb-4"
        onScroll={handleScroll}
      >
        {filteredFeeds.map((feed, index) => (
          <div
            key={feed.id}
            data-feed-id={feed.id}
            className="h-full flex-shrink-0 snap-start snap-always"
          >
            <FeedItem
              feed={feed}
              currentImageIndex={getFeedImageIndex(feed.id)}
              liked={hasLiked(feed.id)}
              onPrevImage={() => setFeedImageIndex(feed.id, (prev) => Math.max(0, prev - 1))}
              onNextImage={() => setFeedImageIndex(feed.id, (prev) => Math.min(feed.images.length - 1, prev + 1))}
              onLike={() => handleLike(feed.id)}
              isPriority={index === 0}
            />
          </div>
        ))}

        {/* 로딩 표시 */}
        {isLoadingFeeds && (
          <div className="flex items-center justify-center py-8 flex-shrink-0">
            <div className="w-8 h-8 border-4 border-gray-300 border-t-[#C93831] rounded-full animate-spin"></div>
          </div>
        )}
      </div>
    </div>
  );
}
