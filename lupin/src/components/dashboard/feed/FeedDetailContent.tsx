/**
 * FeedDetailContent.tsx
 *
 * 피드 상세 콘텐츠 컴포넌트 (Dialog 없이 사용 가능)
 * - FeedDetailDialogHome의 내부 디자인을 재사용
 * - Feed 메뉴에서도 동일한 디자인 사용
 */

import { useState, useEffect, useMemo } from "react";
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
  MoreVertical,
  Edit,
  Trash2,
  X,
  ArrowUpDown,
  Pencil,
  User,
} from "lucide-react";
import { Feed, Comment } from "@/types/dashboard.types";
import { commentApi, reportApi, getCdnUrl } from "@/api";
import { toast } from "sonner";

// 백엔드 댓글 응답 타입
interface BackendComment {
  id: number;
  writerName?: string;
  writerAvatar?: string;
  content: string;
  createdAt: string;
  [key: string]: unknown;
}

// 아바타 URL 변환 헬퍼 함수 (CDN 사용)
const getAvatarUrl = (avatarUrl?: string): string => {
  if (!avatarUrl) return "";
  return getCdnUrl(avatarUrl);
};
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { FeedContentDisplay } from "@/components/shared/FeedContent";
import { useImageBrightness } from "@/hooks";
import { getRelativeTime } from "@/lib/utils";

export interface FeedDetailContentProps {
  feed: Feed;
  currentImageIndex: number;
  onPrevImage: () => void;
  onNextImage: () => void;
  /** 내 피드인 경우에만 표시 */
  onEdit?: (feed: Feed) => void;
  onDelete?: (feedId: number) => void;
  /** 좋아요 상태 (다른 사람 피드인 경우) */
  liked?: boolean;
  onLike?: (feedId: number) => void;
  /** 특정 댓글로 스크롤 */
  targetCommentId?: number | null;
  /** 내 피드 여부 */
  isMine?: boolean;
}

export function FeedDetailContent({
  feed,
  currentImageIndex,
  onPrevImage,
  onNextImage,
  onEdit,
  onDelete,
  liked = false,
  onLike,
  targetCommentId,
  isMine = false,
}: FeedDetailContentProps) {
  const [showComments, setShowComments] = useState(false);
  const [commentText, setCommentText] = useState("");
  const [replyCommentText, setReplyCommentText] = useState("");
  const [comments, setComments] = useState<Comment[]>([]);
  const [replyingTo, setReplyingTo] = useState<number | null>(null);
  const [collapsedComments, setCollapsedComments] = useState<Set<number>>(new Set());
  const [commentLikes, setCommentLikes] = useState<Record<number, { liked: boolean; count: number }>>({});
  const [sortOrder, setSortOrder] = useState<"latest" | "popular">("latest");
  const [showSortMenu, setShowSortMenu] = useState(false);
  const [commentReported, setCommentReported] = useState<Record<number, boolean>>({});

  const currentUserName = localStorage.getItem("userName") || "알 수 없음";
  const currentUserId = parseInt(localStorage.getItem("userId") || "1");

  // 이미지 밝기에 따른 아이콘 색상 (CDN URL 사용)
  const currentImage = feed.images?.[currentImageIndex] || feed.images?.[0];
  const currentImageUrl = currentImage ? getCdnUrl(currentImage) : undefined;
  const iconColor = useImageBrightness(currentImageUrl);

  
  // 댓글 로드
  useEffect(() => {
    const fetchComments = async () => {
      if (!feed) return;
      try {
        const response = await commentApi.getCommentsByFeedId(feed.id, 0, 100);
        const commentList = response.content || response;

        const commentsWithReplies = await Promise.all(
          commentList.map(async (comment: BackendComment) => {
            try {
              const repliesData = await commentApi.getRepliesByCommentId(comment.id);
              const replies = (repliesData || []).map((reply: BackendComment) => ({
                ...reply,
                author: reply.writerName || "알 수 없음",
                avatar: getAvatarUrl(reply.writerAvatar),
                time: getRelativeTime(reply.createdAt),
              }));
              return {
                ...comment,
                author: comment.writerName || "알 수 없음",
                avatar: getAvatarUrl(comment.writerAvatar),
                time: getRelativeTime(comment.createdAt),
                replies,
              };
            } catch {
              return {
                ...comment,
                author: comment.writerName || "알 수 없음",
                avatar: getAvatarUrl(comment.writerAvatar),
                time: getRelativeTime(comment.createdAt),
                replies: [],
              };
            }
          })
        );

        setComments(commentsWithReplies);

        // commentLikes 상태 초기화 (likeCount, isLiked 반영)
        const likesState: Record<number, { liked: boolean; count: number }> = {};
        commentsWithReplies.forEach((comment) => {
          likesState[comment.id] = {
            liked: comment.isLiked || false,
            count: comment.likeCount || 0,
          };
          // 답글의 좋아요 상태도 초기화
          if (comment.replies) {
            comment.replies.forEach((reply: { id: number; isLiked?: boolean; likeCount?: number }) => {
              likesState[reply.id] = {
                liked: reply.isLiked || false,
                count: reply.likeCount || 0,
              };
            });
          }
        });
        setCommentLikes(likesState);

        if (targetCommentId) setShowComments(true);
      } catch (error) {
        console.error("댓글 로드 실패:", error);
        setComments([]);
      }
    };

    fetchComments();
  }, [feed, targetCommentId]);

  // targetCommentId가 있으면 해당 댓글로 스크롤 및 하이라이트
  useEffect(() => {
    if (targetCommentId && comments.length > 0 && showComments) {
      // targetCommentId가 답글인지 확인하고, 답글이면 부모 댓글 펼치기
      let parentIdToExpand: number | null = null;
      for (const comment of comments) {
        // 최상위 댓글인지 확인
        if (comment.id === targetCommentId) {
          break; // 최상위 댓글이면 펼칠 필요 없음
        }
        // 답글인지 확인
        if (comment.replies) {
          const reply = comment.replies.find(r => r.id === targetCommentId);
          if (reply) {
            parentIdToExpand = comment.id;
            break;
          }
        }
      }

      // 부모 댓글이 접혀있으면 펼치기
      if (parentIdToExpand && collapsedComments.has(parentIdToExpand)) {
        setCollapsedComments(prev => {
          const newSet = new Set(prev);
          newSet.delete(parentIdToExpand);
          return newSet;
        });
      }

      // DOM 업데이트 후 스크롤
      setTimeout(() => {
        const commentElement = document.getElementById(`comment-${targetCommentId}`);
        if (commentElement) {
          commentElement.scrollIntoView({ behavior: 'smooth', block: 'center' });
          // 하이라이트 효과 (3초)
          commentElement.style.backgroundColor = '#fef3c7';
          commentElement.style.borderRadius = '8px';
          commentElement.style.padding = '8px';
          setTimeout(() => {
            commentElement.style.backgroundColor = '';
            commentElement.style.borderRadius = '';
            commentElement.style.padding = '';
          }, 3000);
        }
      }, 300);
    }
  }, [targetCommentId, comments, showComments, collapsedComments]);

  // 댓글 전송
  const handleSendComment = async () => {
    if (!commentText.trim() || !feed) return;
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
  };

  // 답글 전송
  const handleSendReply = async () => {
    if (!replyCommentText.trim() || !feed || replyingTo === null) return;
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
        avatar: getAvatarUrl(response.writerAvatar),
        content: response.content,
        time: "방금 전",
        parentId: replyingTo,
        replies: [],
      };

      setComments(
        comments.map((comment) =>
          comment.id === replyingTo
            ? { ...comment, replies: [...(comment.replies || []), newReply] }
            : comment
        )
      );
      setReplyCommentText("");
      setReplyingTo(null);
    } catch (error) {
      console.error("답글 작성 실패:", error);
      toast.error("답글 작성에 실패했습니다.");
    }
  };

  // 총 댓글 수 계산
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

  // 정렬된 댓글
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

  const handleDeleteComment = async (commentId: number) => {
    if (!confirm("댓글을 삭제하시겠습니까?")) return;
    try {
      await commentApi.deleteComment(commentId);
      setComments((prevComments) =>
        prevComments
          .filter((c) => c.id !== commentId) // 부모 댓글 삭제 (대댓글도 백엔드에서 삭제됨)
          .map((c) => ({
            ...c,
            replies: c.replies?.filter((r) => r.id !== commentId) || [], // 대댓글 삭제
          }))
      );
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

  const hasImages = feed.images && feed.images.length > 0;

  // 댓글 렌더링
  const renderComment = (comment: Comment, depth: number = 0) => {
    const isReply = depth > 0;
    const hasReplies = comment.replies && comment.replies.length > 0;
    const isCollapsed = collapsedComments.has(comment.id);
    const isReplying = replyingTo === comment.id;
    const likeInfo = commentLikes[comment.id] || { liked: false, count: 0 };
    const isDeleted = comment.isDeleted;

    return (
      <div key={comment.id} id={`comment-${comment.id}`} className={`transition-colors duration-500 ${isReply ? "ml-8 mt-3" : ""}`}>
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
                  <span className="text-xs text-gray-900">{comment.time}</span>
                </div>
                <p className="text-sm text-gray-900 break-words mb-2">{comment.content}</p>

                <div className="flex items-center gap-4 mb-2">
                  <button onClick={() => toggleCommentLike(comment.id)} className="flex items-center gap-1 hover:opacity-70 transition-opacity">
                    <Heart className={`w-4 h-4 ${likeInfo.liked ? "fill-[#C93831] text-[#C93831]" : "text-gray-600"}`} />
                    {likeInfo.count > 0 && <span className="text-xs text-gray-600 font-semibold">{likeInfo.count}</span>}
                  </button>
                  {depth === 0 && (
                    <button onClick={() => setReplyingTo(replyingTo === comment.id ? null : comment.id)} className="text-xs text-gray-600 hover:text-[#C93831] font-semibold">
                      답글
                    </button>
                  )}
                  {comment.author === currentUserName && (
                    <button onClick={() => handleDeleteComment(comment.id)} className="text-xs text-gray-600 hover:text-red-500 font-semibold">
                      삭제
                    </button>
                  )}
                  {comment.author !== currentUserName && (
                    <button onClick={() => handleReportComment(comment.id)} disabled={commentReported[comment.id]} className={`text-xs font-semibold ${commentReported[comment.id] ? "text-red-500" : "text-gray-600 hover:text-red-500"}`}>
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
                  onKeyPress={(e) => e.key === "Enter" && handleSendReply()}
                  className="w-full py-2 text-sm bg-transparent border-0 border-b-2 border-gray-300 outline-none focus:border-[#C93831] transition-colors"
                  autoFocus
                />
                <div className="flex gap-2 mt-2">
                  <button onClick={() => { setReplyingTo(null); setReplyCommentText(""); }} className="px-3 py-1 text-xs font-semibold text-gray-600 hover:text-gray-900">
                    취소
                  </button>
                  <button onClick={handleSendReply} disabled={!replyCommentText.trim()} className="px-3 py-1 text-xs font-semibold text-[#C93831] hover:text-[#B02F28] disabled:opacity-50 disabled:cursor-not-allowed">
                    답글
                  </button>
                </div>
              </div>
            )}

            {hasReplies && (
              <button onClick={() => toggleCollapse(comment.id)} className="text-xs text-[#C93831] hover:text-[#B02F28] font-semibold flex items-center gap-1 mb-2">
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
    <div className="flex items-center justify-center">
      <div
        className={`h-full max-h-[95vh] overflow-hidden backdrop-blur-2xl bg-white/60 border border-gray-200/30 shadow-2xl flex transition-all duration-300 ${
          showComments ? "w-[825px]" : "w-[475px]"
        }`}
      >
        {/* 메인 피드 콘텐츠 */}
        <div className="w-[475px] flex-shrink-0 flex flex-col overflow-hidden">
          {/* 이미지 영역 */}
          <div className="relative h-[545px] w-full overflow-hidden">
            {hasImages ? (
              <>
                <img
                  src={getCdnUrl(feed.images[currentImageIndex] || feed.images[0])}
                  alt={feed.activity}
                  className="w-full h-full object-cover"
                />

                {feed.images.length > 1 && (
                  <>
                    {currentImageIndex > 0 && (
                      <button onClick={onPrevImage} className="absolute left-2 top-1/2 -translate-y-1/2 hover:opacity-70 transition-opacity">
                        <ChevronLeft className={`w-8 h-8 ${iconColor === "white" ? "text-white" : "text-black"}`} />
                      </button>
                    )}
                    {currentImageIndex < feed.images.length - 1 && (
                      <button onClick={onNextImage} className="absolute right-2 top-1/2 -translate-y-1/2 hover:opacity-70 transition-opacity">
                        <ChevronRight className={`w-8 h-8 ${iconColor === "white" ? "text-white" : "text-black"}`} />
                      </button>
                    )}
                    <div className="absolute bottom-2 left-1/2 -translate-x-1/2 flex gap-1">
                      {feed.images.map((_, idx) => (
                        <div key={idx} className={`w-1.5 h-1.5 rounded-full ${idx === currentImageIndex ? "bg-white" : "bg-white/50"}`} />
                      ))}
                    </div>
                  </>
                )}
              </>
            ) : (
              <div className="w-full h-full bg-gradient-to-br from-gray-100 to-gray-200 flex items-center justify-center">
                <p className="text-gray-500 font-medium text-lg">{feed.activity}</p>
              </div>
            )}

            {/* 작성자 아바타 */}
            <div className="absolute top-4 left-4">
              <Avatar className="w-10 h-10 border-2 border-white shadow-lg">
                {feed.writerAvatar ? (
                  <img src={feed.writerAvatar} alt={feed.writerName} className="w-full h-full object-cover" />
                ) : (
                  <AvatarFallback className="bg-white">
                    <User className="w-5 h-5 text-gray-400" />
                  </AvatarFallback>
                )}
              </Avatar>
            </div>

            {/* 메뉴 버튼 (내 피드인 경우만) */}
            {isMine && (
              <div className="absolute top-4 right-4">
                <DropdownMenu>
                  <DropdownMenuTrigger asChild>
                    <button className="hover:opacity-70 transition-opacity">
                      <MoreVertical className={`w-6 h-6 ${iconColor === "white" ? "text-white" : "text-black"}`} />
                    </button>
                  </DropdownMenuTrigger>
                  <DropdownMenuContent align="end" className="w-48 bg-white/95 backdrop-blur-xl border border-gray-200">
                    <DropdownMenuItem onClick={() => onEdit?.(feed)} className="flex items-center gap-2 cursor-pointer text-gray-900 hover:bg-gray-100">
                      <Edit className="w-4 h-4" />
                      <span className="font-medium">수정</span>
                    </DropdownMenuItem>
                    <DropdownMenuItem
                      onClick={() => {
                        if (window.confirm("정말 삭제하시겠습니까?")) {
                          onDelete?.(feed.id);
                        }
                      }}
                      className="flex items-center gap-2 cursor-pointer text-red-600 hover:bg-red-50"
                    >
                      <Trash2 className="w-4 h-4" />
                      <span className="font-medium">삭제</span>
                    </DropdownMenuItem>
                  </DropdownMenuContent>
                </DropdownMenu>
              </div>
            )}

            {/* 액션 버튼 */}
            <div className="absolute right-4 bottom-4 flex flex-col gap-4 z-10">
              <button onClick={() => onLike?.(feed.id)} className="flex flex-col items-center gap-1 group">
                <div className="w-12 h-12 rounded-full flex items-center justify-center hover:scale-110 transition-transform">
                  <Heart className={`w-6 h-6 ${liked || isMine ? "fill-[#C93831] text-[#C93831]" : iconColor === "white" ? "text-white" : "text-black"}`} />
                </div>
                <span className={`text-xs font-bold ${iconColor === "white" ? "text-white" : "text-black"}`}>{feed.likes}</span>
              </button>

              <button onClick={() => setShowComments(!showComments)} className="flex flex-col items-center gap-1 group">
                <div className="w-12 h-12 rounded-full flex items-center justify-center hover:scale-110 transition-transform">
                  <MessageCircle className={`w-6 h-6 ${iconColor === "white" ? "text-white" : "text-black"}`} />
                </div>
                <span className={`text-xs font-bold ${iconColor === "white" ? "text-white" : "text-black"}`}>{totalCommentCount}</span>
              </button>
            </div>
          </div>

          {/* 피드 내용 */}
          <ScrollArea className="flex-1 bg-transparent">
            <div className="p-6 space-y-3">
              <div className="flex items-start justify-between gap-3">
                <div className="flex items-center gap-2 flex-wrap">
                  <Badge className="bg-gradient-to-r from-yellow-400 to-orange-500 text-white px-3 py-1 font-bold border-0">
                    <Sparkles className="w-3 h-3 mr-1" />+{feed.points}
                  </Badge>
                  <Badge className="bg-white text-blue-700 px-3 py-1 font-bold text-xs border-0">{feed.activity}</Badge>
                  {(feed.calories ?? 0) > 0 && <Badge className="bg-white text-orange-700 px-3 py-1 font-bold text-xs border-0">{feed.calories}kcal</Badge>}
                </div>
                <Badge className="bg-white text-gray-700 px-3 py-1 font-bold text-xs flex items-center gap-1 border-0 flex-shrink-0">
                  {feed.updatedAt && <Pencil className="w-3 h-3" />}
                  {feed.time}
                </Badge>
              </div>

              <FeedContentDisplay content={feed.content} />
            </div>
          </ScrollArea>
        </div>

        {/* 댓글 패널 */}
        {showComments && (
          <div className="flex-1 bg-transparent border-l border-gray-200/30 flex flex-col overflow-hidden">
            <div className="px-6 py-4 border-b border-gray-200/30 flex items-center justify-between bg-transparent">
              <h3 className="text-lg font-bold text-gray-900">댓글 {totalCommentCount}개</h3>
              <div className="relative">
                <button onClick={() => setShowSortMenu(!showSortMenu)} className="flex items-center gap-1 px-3 py-1.5 rounded-lg hover:bg-white/10 transition-colors">
                  <ArrowUpDown className="w-4 h-4 text-gray-900" />
                  <span className="text-sm font-semibold text-gray-900">{sortOrder === "latest" ? "최신순" : "인기순"}</span>
                </button>
                {showSortMenu && (
                  <div className="absolute right-0 top-full mt-1 bg-white/70 backdrop-blur-md border border-gray-200/50 rounded-lg shadow-lg overflow-hidden z-50">
                    <button onClick={() => { setSortOrder("latest"); setShowSortMenu(false); }} className={`w-full px-4 py-2 text-left text-sm hover:bg-white/20 transition-colors ${sortOrder === "latest" ? "bg-white/15 font-semibold text-[#C93831]" : "text-gray-900"}`}>
                      최신순
                    </button>
                    <button onClick={() => { setSortOrder("popular"); setShowSortMenu(false); }} className={`w-full px-4 py-2 text-left text-sm hover:bg-white/20 transition-colors ${sortOrder === "popular" ? "bg-white/15 font-semibold text-[#C93831]" : "text-gray-900"}`}>
                      인기순
                    </button>
                  </div>
                )}
              </div>
            </div>

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

            <div className="p-4 border-t border-gray-200/30 bg-transparent">
              <div className="flex gap-2 items-center">
                <div className="relative flex-1">
                  <input
                    type="text"
                    placeholder="댓글을 입력하세요..."
                    value={commentText}
                    onChange={(e) => setCommentText(e.target.value)}
                    onKeyPress={(e) => e.key === "Enter" && handleSendComment()}
                    className="w-full py-2 pr-8 text-sm bg-transparent border-0 border-b-2 border-gray-300 outline-none focus:border-[#C93831] transition-colors"
                  />
                  {commentText && (
                    <button onClick={() => setCommentText("")} className="absolute right-0 top-1/2 -translate-y-1/2 w-5 h-5 rounded-full bg-gray-200 hover:bg-gray-300 flex items-center justify-center transition-colors">
                      <X className="w-3 h-3 text-gray-600" />
                    </button>
                  )}
                </div>
                <button onClick={handleSendComment} disabled={!commentText.trim()} className="w-10 h-10 rounded-full bg-gradient-to-r from-[#C93831] to-[#B02F28] text-white flex items-center justify-center hover:shadow-lg transition-shadow disabled:opacity-50 disabled:cursor-not-allowed flex-shrink-0">
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

export default FeedDetailContent;
