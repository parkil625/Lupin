/**
 * FeedCommentSection.tsx
 *
 * 피드 댓글 섹션 컴포넌트
 * - 댓글 목록 표시
 * - 댓글/답글 작성
 * - 정렬 기능
 */

import { useState, useEffect, useMemo } from "react";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Heart, Send, ArrowUpDown, X, Flag } from "lucide-react";
import { Comment } from "@/types/dashboard.types";
import { UserHoverCard } from "@/components/dashboard/shared/UserHoverCard";
import { commentApi, reportApi } from "@/api";
import { getRelativeTime } from "@/lib/utils";
import { toast } from "sonner";

export interface FeedCommentSectionProps {
  feedId: number;
}

export function FeedCommentSection({ feedId }: FeedCommentSectionProps) {
  const [comments, setComments] = useState<Comment[]>([]);
  const [commentText, setCommentText] = useState("");
  const [replyText, setReplyText] = useState("");
  const [replyingTo, setReplyingTo] = useState<number | null>(null);
  const [collapsedComments, setCollapsedComments] = useState<Set<number>>(new Set());
  const [commentLikes, setCommentLikes] = useState<Record<number, { liked: boolean; count: number }>>({});
  const [commentReported, setCommentReported] = useState<Record<number, boolean>>({});
  const [sortOrder, setSortOrder] = useState<"latest" | "popular">("latest");
  const [showSortMenu, setShowSortMenu] = useState(false);

  const currentUserName = localStorage.getItem("userName") || "알 수 없음";
  const currentUserId = parseInt(localStorage.getItem("userId") || "1");

  // 댓글 로드
  useEffect(() => {
    const fetchComments = async () => {
      try {
        const response = await commentApi.getCommentsByFeedId(feedId, 0, 100);
        const commentList = response.content || response;

        const commentsWithReplies = await Promise.all(
          commentList.map(async (comment: any) => {
            try {
              const replies = await commentApi.getRepliesByCommentId(comment.id);
              const formattedReplies = (replies || []).map((reply: any) => ({
                ...reply,
                time: getRelativeTime(reply.createdAt),
              }));
              return {
                ...comment,
                time: getRelativeTime(comment.createdAt),
                replies: formattedReplies,
              };
            } catch {
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
        console.error("댓글 로드 실패:", error);
        setComments([]);
      }
    };

    fetchComments();
  }, [feedId]);

  // 댓글 전송
  const handleSendComment = async () => {
    if (!commentText.trim()) return;

    try {
      const response = await commentApi.createComment({
        content: commentText,
        feedId,
        writerId: currentUserId,
      });

      const newComment: Comment = {
        id: response.id,
        author: response.writerName || currentUserName,
        avatar: (response.writerName || currentUserName).charAt(0),
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
  };

  // 답글 전송
  const handleSendReply = async () => {
    if (!replyText.trim() || replyingTo === null) return;

    try {
      const response = await commentApi.createComment({
        content: replyText,
        feedId,
        writerId: currentUserId,
        parentId: replyingTo,
      });

      const newReply: Comment = {
        id: response.id,
        author: response.writerName || currentUserName,
        avatar: (response.writerName || currentUserName).charAt(0),
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
      setReplyText("");
      setReplyingTo(null);
    } catch (error) {
      console.error("답글 작성 실패:", error);
      alert("답글 작성에 실패했습니다.");
    }
  };

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

  // 총 댓글 수 계산
  const totalCount = useMemo(() => {
    let count = 0;
    for (const comment of comments) {
      count += 1;
      if (comment.replies) count += comment.replies.length;
    }
    return count;
  }, [comments]);

  const toggleCommentLike = (commentId: number) => {
    setCommentLikes((prev) => {
      const current = prev[commentId] || { liked: false, count: 0 };
      return {
        ...prev,
        [commentId]: {
          liked: !current.liked,
          count: current.liked ? Math.max(0, current.count - 1) : current.count + 1,
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

  // 댓글 신고
  const handleReportComment = async (commentId: number) => {
    try {
      await reportApi.reportComment(commentId);
      setCommentReported((prev) => ({ ...prev, [commentId]: !prev[commentId] }));
      toast.success(commentReported[commentId] ? "신고가 취소되었습니다." : "신고가 접수되었습니다.");
    } catch {
      toast.error("신고에 실패했습니다.");
    }
  };

  return (
    <div className="flex-1 bg-transparent border-l border-gray-200/30 flex flex-col overflow-hidden">
      {/* 헤더 */}
      <div className="px-6 py-4 border-b border-gray-200/30 flex items-center justify-between bg-transparent">
        <h3 className="text-lg font-bold text-gray-900">댓글 {totalCount}개</h3>
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
                  sortOrder === "latest" ? "bg-white/15 font-semibold text-[#C93831]" : "text-gray-900"
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
                  sortOrder === "popular" ? "bg-white/15 font-semibold text-[#C93831]" : "text-gray-900"
                }`}
              >
                인기순
              </button>
            </div>
          )}
        </div>
      </div>

      {/* 댓글 목록 */}
      <div className="flex-1 overflow-hidden">
        <ScrollArea className="h-full">
          <div className="space-y-4 px-6 pt-4 pb-4">
            {comments.length === 0 ? (
              <div className="text-center text-gray-500 text-sm py-8">
                첫 댓글을 남겨보세요!
              </div>
            ) : (
              sortedComments.map((comment) => (
                <CommentItem
                  key={comment.id}
                  comment={comment}
                  depth={0}
                  replyingTo={replyingTo}
                  setReplyingTo={setReplyingTo}
                  replyText={replyText}
                  setReplyText={setReplyText}
                  onSendReply={handleSendReply}
                  commentLikes={commentLikes}
                  toggleCommentLike={toggleCommentLike}
                  collapsedComments={collapsedComments}
                  toggleCollapse={toggleCollapse}
                  commentReported={commentReported}
                  onReportComment={handleReportComment}
                  currentUserName={currentUserName}
                />
              ))
            )}
          </div>
        </ScrollArea>
      </div>

      {/* 댓글 입력 */}
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
  );
}

/** 댓글 아이템 컴포넌트 */
interface CommentItemProps {
  comment: Comment;
  depth: number;
  replyingTo: number | null;
  setReplyingTo: (id: number | null) => void;
  replyText: string;
  setReplyText: (text: string) => void;
  onSendReply: () => void;
  commentLikes: Record<number, { liked: boolean; count: number }>;
  toggleCommentLike: (id: number) => void;
  collapsedComments: Set<number>;
  toggleCollapse: (id: number) => void;
  commentReported: Record<number, boolean>;
  onReportComment: (id: number) => void;
  currentUserName: string;
}

function CommentItem({
  comment,
  depth,
  replyingTo,
  setReplyingTo,
  replyText,
  setReplyText,
  onSendReply,
  commentLikes,
  toggleCommentLike,
  collapsedComments,
  toggleCollapse,
  commentReported,
  onReportComment,
  currentUserName,
}: CommentItemProps) {
  const isReply = depth > 0;
  const hasReplies = comment.replies && comment.replies.length > 0;
  const isCollapsed = collapsedComments.has(comment.id);
  const isReplying = replyingTo === comment.id;
  const likeInfo = commentLikes[comment.id] || { liked: false, count: 0 };

  return (
    <div className={isReply ? "ml-8 mt-3" : ""}>
      <div className="flex gap-3">
        <UserHoverCard
          name={comment.author}
          department={comment.department}
          activeDays={comment.activeDays}
          avgScore={comment.avgScore}
          points={comment.points}
          size="sm"
        />
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2 mb-1">
            <span className="font-bold text-sm text-gray-900">{comment.author}</span>
            <span className="text-xs text-gray-900">{comment.time}</span>
          </div>
          <p className="text-sm text-gray-900 break-words mb-2">{comment.content}</p>

          <div className="flex items-center gap-4 mb-2">
            <button
              onClick={() => toggleCommentLike(comment.id)}
              className="flex items-center gap-1 hover:opacity-70 transition-opacity"
            >
              <Heart
                className={`w-4 h-4 ${likeInfo.liked ? "fill-red-500 text-red-500" : "text-gray-600"}`}
              />
              {likeInfo.count > 0 && (
                <span className="text-xs text-gray-600 font-semibold">{likeInfo.count}</span>
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
            {comment.author !== currentUserName && (
              <button
                onClick={() => onReportComment(comment.id)}
                className="flex items-center gap-1 hover:opacity-70 transition-opacity"
              >
                <Flag
                  className={`w-4 h-4 ${commentReported[comment.id] ? "fill-[#C93831] text-[#C93831]" : "text-gray-600"}`}
                />
              </button>
            )}
          </div>

          {isReplying && (
            <div className="mb-3">
              <input
                type="text"
                placeholder="답글을 입력하세요..."
                value={replyText}
                onChange={(e) => setReplyText(e.target.value)}
                onKeyPress={(e) => e.key === "Enter" && onSendReply()}
                className="w-full py-2 text-sm bg-transparent border-0 border-b-2 border-gray-300 outline-none focus:border-[#C93831] transition-colors"
                autoFocus
              />
              <div className="flex gap-2 mt-2">
                <button
                  onClick={() => {
                    setReplyingTo(null);
                    setReplyText("");
                  }}
                  className="px-3 py-1 text-xs font-semibold text-gray-600 hover:text-gray-900"
                >
                  취소
                </button>
                <button
                  onClick={onSendReply}
                  disabled={!replyText.trim()}
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
          {comment.replies!.map((reply) => (
            <CommentItem
              key={reply.id}
              comment={reply}
              depth={depth + 1}
              replyingTo={replyingTo}
              setReplyingTo={setReplyingTo}
              replyText={replyText}
              setReplyText={setReplyText}
              onSendReply={onSendReply}
              commentLikes={commentLikes}
              toggleCommentLike={toggleCommentLike}
              collapsedComments={collapsedComments}
              toggleCollapse={toggleCollapse}
              commentReported={commentReported}
              onReportComment={onReportComment}
              currentUserName={currentUserName}
            />
          ))}
        </div>
      )}
    </div>
  );
}

export default FeedCommentSection;
