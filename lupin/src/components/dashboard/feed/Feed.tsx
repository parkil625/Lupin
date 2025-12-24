/**
 * Feed.tsx
 *
 * 피드 페이지 컴포넌트
 * - 세로 스크롤 + 한 개씩 표시 (쇼츠 스타일)
 * - FeedV2 디자인 적용
 */

import React, { useState, useEffect, useRef, useMemo } from "react";
import SearchInput from "@/components/molecules/SearchInput";
import { Feed, Comment } from "@/types/dashboard.types";
import { getRelativeTime, parseBlockNoteContent } from "@/lib/utils";
import { ScrollArea } from "@/components/ui/scroll-area";
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
  Siren,
  Flame,
  Clock,
  Zap,
  Loader2,
} from "lucide-react";
import { commentApi, reportApi, getCdnUrl } from "@/api";
import { toast } from "sonner";
import { useImageBrightness } from "@/hooks";
import { useFeedStore } from "@/store/useFeedStore";
import AutoSizer from "react-virtualized-auto-sizer";
import { ComponentType } from "react";

// [해결 1] TS 에러 무시: react-window의 타입 정의가 꼬여있어서 발생하는 "FixedSizeList 없음" 에러를 잡습니다.
// @ts-expect-error: 타입 정의 불일치 문제 무시
import { FixedSizeList } from "react-window";

import UserHoverCard from "@/components/molecules/UserHoverCard";

// [해결 2] 린트 에러 무시: any 사용 금지 규칙을 이 줄에서만 끕니다.
// eslint-disable-next-line @typescript-eslint/no-explicit-any
const List = FixedSizeList as unknown as ComponentType<any>;

// [수정] any 대신 명확한 인터페이스 사용
interface FeedData {
  feeds: Feed[];
  getFeedImageIndex: (feedId: number) => number;
  setFeedImageIndex: (
    feedId: number,
    updater: number | ((prev: number) => number)
  ) => void;
  hasLiked: (feedId: number) => boolean;
  handleLike: (feedId: number) => void;
  pivotFeedId?: number | null;
  targetCommentIdForFeed?: number | null;
}

interface ListChildComponentProps {
  index: number;
  style: React.CSSProperties;
  data: FeedData;
}

interface FeedViewProps {
  allFeeds: Feed[];
  searchQuery: string;
  setSearchQuery: React.Dispatch<React.SetStateAction<string>>;
  showSearch: boolean;
  setShowSearch: React.Dispatch<React.SetStateAction<boolean>>;
  getFeedImageIndex: (feedId: number) => number;
  setFeedImageIndex: (
    feedId: number,
    updater: number | ((prev: number) => number)
  ) => void;
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
function CommentPanel({
  feedId,
  onClose,
  targetCommentId,
}: {
  feedId: number;
  onClose?: () => void;
  targetCommentId?: number | null;
}) {
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
  const [commentReported, setCommentReported] = useState<
    Record<number, boolean>
  >({});
  const [highlightedCommentId, setHighlightedCommentId] = useState<
    number | null
  >(null);
  const commentRefs = useRef<Record<number, HTMLDivElement | null>>({});
  const [editingCommentId, setEditingCommentId] = useState<number | null>(null);
  const [editCommentText, setEditCommentText] = useState("");

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
          commentList.map(
            async (comment: {
              id: number;
              writerName?: string;
              writerAvatar?: string;
              writerDepartment?: string;
              writerActiveDays?: number;
              createdAt?: string;
            }) => {
              try {
                const repliesData = await commentApi.getRepliesByCommentId(
                  comment.id
                );
                const replies = (repliesData || []).map(
                  (reply: {
                    writerName?: string;
                    writerAvatar?: string;
                    writerDepartment?: string;
                    writerActiveDays?: number;
                    createdAt?: string;
                  }) => ({
                    ...reply,
                    author: reply.writerName || "알 수 없음",
                    avatar: getAvatarUrl(reply.writerAvatar),
                    department: reply.writerDepartment,
                    activeDays: reply.writerActiveDays,
                    time: getRelativeTime(
                      reply.createdAt || new Date().toISOString()
                    ),
                  })
                );
                return {
                  ...comment,
                  author: comment.writerName || "알 수 없음",
                  avatar: getAvatarUrl(comment.writerAvatar),
                  department: comment.writerDepartment,
                  activeDays: comment.writerActiveDays,
                  time: getRelativeTime(
                    comment.createdAt || new Date().toISOString()
                  ),
                  replies,
                };
              } catch {
                return {
                  ...comment,
                  author: comment.writerName || "알 수 없음",
                  avatar: getAvatarUrl(comment.writerAvatar),
                  department: comment.writerDepartment,
                  activeDays: comment.writerActiveDays,
                  time: getRelativeTime(
                    comment.createdAt || new Date().toISOString()
                  ),
                  replies: [],
                };
              }
            }
          )
        );
        setComments(commentsWithReplies);

        // commentLikes 상태 초기화 (likeCount, isLiked 반영)
        const likesState: { [key: number]: { liked: boolean; count: number } } =
          {};
        commentsWithReplies.forEach(
          (comment: {
            id: number;
            isLiked?: boolean;
            likeCount?: number;
            replies?: { id: number; isLiked?: boolean; likeCount?: number }[];
          }) => {
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
          }
        );
        setCommentLikes(likesState);
      } catch (error) {
        console.error("댓글 로드 실패:", error);
        setComments([]);
      }
    };
    fetchComments();
  }, [feedId]);

  // 타겟 댓글 하이라이트 및 스크롤
  useEffect(() => {
    if (targetCommentId && comments.length > 0) {
      const numTargetId = Number(targetCommentId); // [수정] 여기서 정의

      // [수정] 비동기 처리로 렌더링 충돌 방지
      setTimeout(() => {
        setHighlightedCommentId(numTargetId);

        // 답글인 경우 부모 댓글의 접힘 상태 해제
        for (const comment of comments) {
          const reply = comment.replies?.find(
            (r) => Number(r.id) === numTargetId
          );
          if (reply) {
            setCollapsedComments((prev) => {
              const newSet = new Set(prev);
              newSet.delete(comment.id);
              return newSet;
            });
            break;
          }
        }
      }, 0);

      // 잠시 후 스크롤 (DOM 업데이트 대기)
      setTimeout(() => {
        const element = commentRefs.current[numTargetId]; // 이제 numTargetId 접근 가능
        if (element) {
          element.scrollIntoView({ behavior: "smooth", block: "center" });
        }
      }, 100);

      // 3초 후 하이라이트 제거
      const timer = setTimeout(() => {
        setHighlightedCommentId(null);
      }, 3000);

      return () => clearTimeout(timer);
    }
  }, [targetCommentId, comments]);

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
          .filter((c) => c.id !== commentId) // 부모 댓글 삭제 (대댓글도 백엔드에서 삭제됨)
          .map((c) => ({
            ...c,
            replies: c.replies?.filter((r) => r.id !== commentId) || [], // 대댓글 삭제
          }));
      });
    } catch (error) {
      console.error("댓글 삭제 실패:", error);
      toast.error("댓글 삭제에 실패했습니다.");
    }
  };

  const startEdit = (id: number, content: string) => {
    setEditingCommentId(id);
    setEditCommentText(content);
  };

  const cancelEdit = () => {
    setEditingCommentId(null);
    setEditCommentText("");
  };

  const handleUpdateComment = async (commentId: number) => {
    if (!editCommentText.trim()) return;
    try {
      await commentApi.updateComment(commentId, editCommentText);
      setComments((prev) =>
        prev.map((c) => {
          if (c.id === commentId)
            return {
              ...c,
              content: editCommentText,
              updatedAt: new Date().toISOString(),
            };
          if (c.replies) {
            return {
              ...c,
              replies: c.replies.map((r) =>
                r.id === commentId
                  ? {
                      ...r,
                      content: editCommentText,
                      updatedAt: new Date().toISOString(),
                    }
                  : r
              ),
            };
          }
          return c;
        })
      );
      cancelEdit();
      toast.success("댓글이 수정되었습니다.");
    } catch {
      toast.error("댓글 수정에 실패했습니다.");
    }
  };

  const handleReportComment = async (commentId: number) => {
    try {
      await reportApi.reportComment(commentId);
      setCommentReported((prev) => {
        const isReported = !prev[commentId];
        toast.success(
          isReported ? "신고가 접수되었습니다." : "신고가 취소되었습니다."
        );
        return { ...prev, [commentId]: isReported };
      });
    } catch {
      toast.error("신고 처리에 실패했습니다.");
    }
  };

  const renderComment = (comment: Comment, depth: number = 0) => {
    const hasReplies = comment.replies && comment.replies.length > 0;
    const isCollapsed = collapsedComments.has(comment.id);
    const isReplying = replyingTo === comment.id;
    const likeInfo = commentLikes[comment.id] || { liked: false, count: 0 };
    const isDeleted = comment.isDeleted;
    const isHighlighted =
      highlightedCommentId !== null &&
      Number(highlightedCommentId) === Number(comment.id);

    return (
      <div key={comment.id} className={depth > 0 ? "ml-8 mt-3" : ""}>
        {/* 댓글 내용 영역 - 하이라이트 대상 */}
        <div
          ref={(el) => {
            commentRefs.current[comment.id] = el;
          }}
          className="flex gap-3 transition-colors duration-500 rounded-lg"
          style={{ backgroundColor: isHighlighted ? "#fef3c7" : undefined }}
        >
          <UserHoverCard
            name={comment.author}
            department={comment.department}
            activeDays={comment.activeDays}
            avatarUrl={comment.avatar || undefined}
            size="sm"
          />
          <div className="flex-1 min-w-0">
            {isDeleted ? (
              <p className="text-sm text-gray-400 italic mb-2">
                삭제된 댓글입니다.
              </p>
            ) : (
              <>
                <div className="flex items-center gap-2 mb-1">
                  <span className="font-bold text-sm text-gray-900">
                    {comment.author}
                  </span>
                  <span className="text-xs text-gray-500">
                    {comment.time}
                    {comment.updatedAt && (
                      <span className="ml-1 text-gray-400 font-normal">
                        (수정됨)
                      </span>
                    )}
                  </span>
                </div>
                {editingCommentId === comment.id ? (
                  <div className="mb-2">
                    <input
                      type="text"
                      value={editCommentText}
                      onChange={(e) => setEditCommentText(e.target.value)}
                      className="w-full py-1 text-sm border-b-2 border-[#C93831] outline-none bg-transparent"
                      autoFocus
                      onKeyDown={(e) => {
                        if (e.key === "Enter") handleUpdateComment(comment.id);
                        if (e.key === "Escape") cancelEdit();
                      }}
                    />
                    <div className="flex gap-2 mt-1 justify-end">
                      <button
                        onClick={cancelEdit}
                        // [수정] cursor-pointer 추가
                        className="text-xs text-gray-500 hover:text-gray-900 transition-colors font-medium cursor-pointer"
                      >
                        취소
                      </button>
                      <button
                        onClick={() => handleUpdateComment(comment.id)}
                        // [수정] cursor-pointer 추가
                        className="text-xs text-[#C93831] font-bold hover:text-[#a02b25] transition-colors cursor-pointer"
                      >
                        저장
                      </button>
                    </div>
                  </div>
                ) : (
                  <p className="text-sm text-gray-900 break-words mb-2">
                    {comment.content}
                  </p>
                )}
                {/* [수정] 수정 모드가 아닐 때만 하단 액션 버튼 표시 */}
                {editingCommentId !== comment.id && (
                  <div className="flex items-center gap-4 mb-2">
                    <button
                      onClick={() => toggleCommentLike(comment.id)}
                      className="flex items-center gap-1 hover:opacity-70 transition-opacity cursor-pointer"
                    >
                      <Heart
                        className={`w-4 h-4 ${
                          likeInfo.liked
                            ? "fill-[#C93831] text-[#C93831]"
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
                        onClick={() =>
                          setReplyingTo(
                            replyingTo === comment.id ? null : comment.id
                          )
                        }
                        className="text-xs text-gray-600 hover:text-[#C93831] font-semibold cursor-pointer"
                      >
                        답글
                      </button>
                    )}

                    {comment.author === currentUserName && (
                      <>
                        <button
                          onClick={() => startEdit(comment.id, comment.content)}
                          className="text-xs text-gray-600 hover:text-[#C93831] font-semibold cursor-pointer"
                        >
                          수정
                        </button>
                        <button
                          onClick={() => handleDeleteComment(comment.id)}
                          className="text-xs text-gray-600 hover:text-red-500 font-semibold cursor-pointer"
                        >
                          삭제
                        </button>
                      </>
                    )}
                    {comment.author !== currentUserName && (
                      <button
                        onClick={() => handleReportComment(comment.id)}
                        className="flex items-center gap-1 hover:opacity-70 transition-opacity cursor-pointer"
                      >
                        <Siren
                          className={`w-4 h-4 ${
                            commentReported[comment.id]
                              ? "fill-[#C93831] text-[#C93831]"
                              : "text-gray-600"
                          }`}
                        />
                      </button>
                    )}
                  </div>
                )}
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
                  <button
                    onClick={() => {
                      setReplyingTo(null);
                      setReplyCommentText("");
                    }}
                    className="px-3 py-1 text-xs font-semibold text-gray-600 hover:text-gray-900 cursor-pointer"
                  >
                    취소
                  </button>
                  <button
                    onClick={handleSendReply}
                    disabled={!replyCommentText.trim()}
                    className="px-3 py-1 text-xs font-semibold text-[#C93831] hover:text-[#B02F28] disabled:opacity-50 cursor-pointer"
                  >
                    답글
                  </button>
                </div>
              </div>
            )}

            {hasReplies && (
              <button
                onClick={() => toggleCollapse(comment.id)}
                className="text-xs text-[#C93831] hover:text-[#B02F28] font-semibold flex items-center gap-1 mb-2 cursor-pointer"
              >
                {isCollapsed ? "▶" : "▼"} 답글 {comment.replies!.length}개
              </button>
            )}
          </div>
        </div>

        {/* 답글 영역 - 하이라이트 대상에서 분리 */}
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
        <h3 className="text-lg font-bold text-gray-900">
          댓글 {totalCommentCount}개
        </h3>
        <div className="flex items-center gap-2">
          <div className="relative">
            <button
              onClick={() => setShowSortMenu(!showSortMenu)}
              className="flex items-center gap-1 px-3 py-1.5 rounded-lg hover:bg-gray-100 transition-colors cursor-pointer"
            >
              <ArrowUpDown className="w-4 h-4 text-gray-900" />
              <span className="text-sm font-semibold text-gray-900">
                {sortOrder === "latest" ? "최신순" : "인기순"}
              </span>
            </button>
            {showSortMenu && (
              <div className="absolute right-0 top-full mt-1 bg-white border border-gray-200 rounded-lg shadow-lg overflow-hidden z-50">
                <button
                  onClick={() => {
                    setSortOrder("latest");
                    setShowSortMenu(false);
                  }}
                  className={`w-full px-4 py-2 text-left text-sm hover:bg-gray-100 transition-colors cursor-pointer ${
                    sortOrder === "latest"
                      ? "bg-gray-50 font-semibold text-[#C93831]"
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
                  className={`w-full px-4 py-2 text-left text-sm hover:bg-gray-100 transition-colors cursor-pointer ${
                    sortOrder === "popular"
                      ? "bg-gray-50 font-semibold text-[#C93831]"
                      : "text-gray-900"
                  }`}
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
              <button
                onClick={() => setCommentText("")}
                className="absolute right-0 top-1/2 -translate-y-1/2 w-5 h-5 rounded-full bg-gray-200 hover:bg-gray-300 flex items-center justify-center cursor-pointer"
                aria-label="댓글 지우기"
              >
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
 * React.memo로 불필요한 리렌더링 방지
 */
const FeedItem = React.memo(function FeedItem({
  feed,
  currentImageIndex,
  liked,
  onPrevImage,
  onNextImage,
  onLike,
  isPriority = false,
  targetCommentId,
}: {
  feed: Feed;
  currentImageIndex: number;
  liked: boolean;
  onPrevImage: () => void;
  onNextImage: () => void;
  onLike: () => void;
  isPriority?: boolean;
  targetCommentId?: number | null;
}) {
  const [showComments, setShowComments] = useState(false);
  const [isReported, setIsReported] = useState(false);

  // targetCommentId가 있으면 댓글창 자동 열기
  useEffect(() => {
    if (targetCommentId) {
      setTimeout(() => setShowComments(true), 0);
    }
  }, [targetCommentId]);

  const hasImages = feed.images && feed.images.length > 0;
  const images = feed.images || [];
  const isFirstImage = currentImageIndex === 0;
  const isLastImage = currentImageIndex === images.length - 1;

  // 이미지 밝기에 따른 아이콘 색상 결정 (CDN URL 사용)
  const currentImageUrl = hasImages
    ? getCdnUrl(images[currentImageIndex])
    : undefined;
  const iconColor = useImageBrightness(currentImageUrl);
  const iconColorClass = iconColor === "white" ? "text-white" : "text-gray-900";

  const handleReport = async () => {
    try {
      await reportApi.reportFeed(feed.id);
      setIsReported(!isReported);
      toast.success(
        isReported ? "신고가 취소되었습니다." : "신고가 접수되었습니다."
      );
    } catch {
      toast.error(
        isReported ? "신고 취소에 실패했습니다." : "신고에 실패했습니다."
      );
    }
  };

  return (
    <div
      className={`h-full max-h-[calc(100vh-130px)] md:max-h-full w-fit mx-auto flex shadow-[0_2px_12px_rgba(0,0,0,0.12)] rounded-2xl overflow-hidden transition-all duration-300 relative`}
    >
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
              <p className="text-gray-500 font-medium text-lg">
                {feed.activity}
              </p>
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
                        ? iconColor === "white"
                          ? "bg-white"
                          : "bg-gray-900"
                        : iconColor === "white"
                        ? "bg-white/50"
                        : "bg-gray-900/50"
                    }`}
                  />
                ))}
              </div>
            </>
          )}

          {/* 작성자 호버카드 */}
          <div className="absolute top-4 left-4">
            <UserHoverCard
              name={feed.author || feed.writerName}
              department={feed.writerDepartment}
              activeDays={feed.writerActiveDays}
              avatarUrl={feed.writerAvatar}
            />
          </div>

          {/* 액션 버튼 */}
          <div className="absolute right-4 bottom-4 flex flex-col gap-4">
            <button
              className="flex flex-col items-center gap-1 cursor-pointer hover:scale-110 transition-transform"
              onClick={onLike}
              aria-label={liked ? "좋아요 취소" : "좋아요"}
            >
              <Heart
                className={`w-6 h-6 ${
                  liked ? "fill-[#C93831] text-[#C93831]" : iconColorClass
                }`}
              />
              <span className={`text-xs font-bold ${iconColorClass}`}>
                {feed.likes}
              </span>
            </button>
            <button
              className="flex flex-col items-center gap-1 cursor-pointer hover:scale-110 transition-transform"
              onClick={() => setShowComments(!showComments)}
              aria-label={showComments ? "댓글 닫기" : "댓글 보기"}
            >
              <MessageCircle
                className={`w-6 h-6 ${iconColorClass} ${
                  showComments
                    ? iconColor === "white"
                      ? "fill-white"
                      : "fill-gray-900"
                    : ""
                }`}
              />
              <span className={`text-xs font-bold ${iconColorClass}`}>
                {feed.comments || 0}
              </span>
            </button>
            <button
              className="flex flex-col items-center gap-1 cursor-pointer hover:scale-110 transition-transform"
              onClick={handleReport}
              aria-label={isReported ? "신고 취소" : "피드 신고"}
            >
              <Siren
                className={`w-6 h-6 ${
                  isReported ? "text-[#C93831] fill-[#C93831]" : iconColorClass
                }`}
              />
            </button>
          </div>
        </div>

        {/* 콘텐츠 영역 - 43% */}
        <ScrollArea className="h-[43%] bg-white/50 backdrop-blur-sm">
          <div className="p-6 space-y-3">
            {/* 뱃지 */}
            <div className="flex items-center justify-between">
              <div className="flex gap-2 flex-wrap">
                {(feed.points ?? 0) > 0 && (
                  <Badge className="bg-amber-50 text-amber-600 font-medium border border-amber-200">
                    <Sparkles className="w-3 h-3 mr-1" />+{feed.points}
                  </Badge>
                )}
                <Badge className="bg-blue-50 text-blue-600 font-medium border border-blue-200">
                  <Zap className="w-3 h-3 mr-1" />
                  {feed.activity}
                </Badge>
                {(feed.calories ?? 0) > 0 && (
                  <Badge className="bg-orange-50 text-orange-600 font-medium border border-orange-200">
                    <Flame className="w-3 h-3 mr-1" />
                    {feed.calories}kcal
                  </Badge>
                )}
              </div>
              <Badge className="bg-slate-50 text-slate-500 font-medium border border-slate-200">
                <Clock className="w-3 h-3 mr-1" />
                {feed.time}
                {feed.updatedAt && (
                  <span className="ml-1 text-[10px] text-slate-400">
                    (수정됨)
                  </span>
                )}
              </Badge>
            </div>

            {/* 본문 */}
            <p className="text-gray-900 text-sm">
              {parseBlockNoteContent(feed.content)}
            </p>
          </div>
        </ScrollArea>
      </div>

      {/* 댓글 패널 - 모바일: 전체화면 오버레이, 데스크톱: 옆에 표시 */}
      {showComments && (
        <>
          {/* 모바일용 전체화면 오버레이 (하단 네비 제외) */}
          <div className="md:hidden fixed inset-x-0 top-0 bottom-[60px] z-50 bg-white flex flex-col">
            <CommentPanel
              feedId={feed.id}
              onClose={() => setShowComments(false)}
              targetCommentId={targetCommentId}
            />
          </div>
          {/* 데스크톱용 사이드 패널 */}
          <div className="hidden md:block h-full aspect-[7/16] border-l border-gray-200/50 flex-shrink-0">
            <CommentPanel feedId={feed.id} targetCommentId={targetCommentId} />
          </div>
        </>
      )}
    </div>
  );
});

/**
 * 윈도윙을 위한 개별 행 컴포넌트 (Memoization 적용)
 */
const FeedRow = React.memo(
  ({ index, style, data }: ListChildComponentProps) => {
    const {
      feeds,
      getFeedImageIndex,
      setFeedImageIndex,
      hasLiked,
      handleLike,
      pivotFeedId,
      targetCommentIdForFeed,
    } = data;
    const feed = feeds[index];

    return (
      <div style={style} className="snap-start snap-always">
        {/* gap-4(16px)를 표현하기 위해 높이에서 16px를 뺀 만큼만 채움 */}
        <div className="w-full h-[calc(100%-16px)]">
          <FeedItem
            feed={feed}
            currentImageIndex={getFeedImageIndex(feed.id)}
            liked={hasLiked(feed.id)}
            onPrevImage={() =>
              setFeedImageIndex(feed.id, (prev: number) =>
                Math.max(0, prev - 1)
              )
            }
            onNextImage={() =>
              setFeedImageIndex(feed.id, (prev: number) =>
                Math.min(feed.images.length - 1, prev + 1)
              )
            }
            onLike={() => handleLike(feed.id)}
            isPriority={index === 0}
            targetCommentId={
              index === 0 && pivotFeedId && feed.id === pivotFeedId
                ? targetCommentIdForFeed
                : null
            }
          />
        </div>
      </div>
    );
  }
);
FeedRow.displayName = "FeedRow"; // [수정] 이 줄을 추가하면 에러가 사라집니다!

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

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const listRef = useRef<any>(null);

  // 스토어에서 targetCommentIdForFeed와 pivotFeedId 읽기
  const { targetCommentIdForFeed, pivotFeedId, setTargetCommentIdForFeed } =
    useFeedStore();

  // targetCommentIdForFeed 사용 후 정리 (컴포넌트 언마운트 또는 다른 페이지로 이동 시)
  useEffect(() => {
    return () => {
      if (targetCommentIdForFeed) {
        setTargetCommentIdForFeed(null);
      }
    };
  }, [targetCommentIdForFeed, setTargetCommentIdForFeed]);

  // [최적화] LCP 이미지 Preload - 첫 번째 피드 이미지
  useEffect(() => {
    if (allFeeds.length > 0 && allFeeds[0].images?.[0]) {
      const link = document.createElement("link");
      link.rel = "preload";
      link.as = "image";
      link.href = getCdnUrl(allFeeds[0].images[0]);
      document.head.appendChild(link);
      return () => link.remove();
    }
  }, [allFeeds]);

  // 검색 자동완성 목록 (메모이제이션)
  const authorSuggestions = useMemo(
    () => [...new Set(allFeeds.map((feed) => feed.author || feed.writerName))],
    [allFeeds]
  );

  // 필터링된 피드
  const filteredFeeds = useMemo(() => {
    if (!searchQuery.trim()) return allFeeds;
    return allFeeds.filter((feed) => {
      const authorName = feed.author || feed.writerName;
      return authorName.toLowerCase().includes(searchQuery.toLowerCase());
    });
  }, [allFeeds, searchQuery]);

  // 특정 피드로 스크롤 (윈도윙 적용)
  useEffect(() => {
    if (scrollToFeedId && listRef.current) {
      const index = filteredFeeds.findIndex((f) => f.id === scrollToFeedId);
      if (index !== -1) {
        listRef.current.scrollToItem(index, "start");
        setScrollToFeedId(null);
      }
    }
  }, [scrollToFeedId, setScrollToFeedId, filteredFeeds]);

  useEffect(() => {
    if (feedContainerRef && containerRef.current) {
      const ref =
        feedContainerRef as React.MutableRefObject<HTMLDivElement | null>;
      ref.current = containerRef.current;
    }
  }, [feedContainerRef]);

  return (
    <div ref={containerRef} className="h-full flex flex-col p-4 gap-4">
      {/* 검색바 */}
      <div className="mx-auto max-w-2xl w-full flex-shrink-0">
        <SearchInput
          value={searchQuery}
          onChange={setSearchQuery}
          placeholder="작성자 이름으로 검색..."
          suggestions={authorSuggestions}
        />
      </div>

      {/* 피드 리스트 - 윈도윙 적용 */}
      <div className="flex-1 w-full h-full relative">
        <AutoSizer>
          {({ height, width }) => {
            // FeedRow에 전달할 데이터 객체
            const itemData: FeedData = {
              feeds: filteredFeeds,
              getFeedImageIndex,
              setFeedImageIndex,
              hasLiked,
              handleLike,
              pivotFeedId,
              targetCommentIdForFeed,
            };

            return (
              <List
                ref={listRef}
                height={height}
                width={width}
                itemCount={filteredFeeds.length}
                // [중요] gap-4(16px)를 위해 itemSize를 높이+16으로 설정
                itemSize={height + 16}
                itemData={itemData}
                className="snap-y snap-mandatory scrollbar-hide"
                onItemsRendered={({
                  visibleStopIndex,
                }: {
                  visibleStopIndex: number;
                }) => {
                  // 마지막 아이템이 보이면 추가 로드
                  if (
                    visibleStopIndex >= filteredFeeds.length - 1 &&
                    hasMoreFeeds &&
                    !isLoadingFeeds
                  ) {
                    loadMoreFeeds();
                  }
                }}
              >
                {FeedRow}
              </List>
            );
          }}
        </AutoSizer>

        {/* 로딩 표시 (리스트 위에 오버레이) */}
        {isLoadingFeeds && (
          <div className="absolute bottom-4 left-1/2 -translate-x-1/2 z-10 pointer-events-none">
            {/* [수정] Loader2 컴포넌트 사용 */}
            <Loader2 className="w-8 h-8 text-[#C93831] animate-spin shadow-lg" />
          </div>
        )}
      </div>
    </div>
  );
}
