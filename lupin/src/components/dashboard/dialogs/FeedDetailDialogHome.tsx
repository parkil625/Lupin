/**
 * FeedDetailDialogHome.tsx
 *
 * 피드 상세보기 다이얼로그 컴포넌트
 * - 홈 화면에서 피드 클릭 시 표시
 * - 이미지 캐러셀 기능
 * - 피드 상세 정보 표시
 * - 댓글 표시 및 작성 기능
 */
import { useState, useEffect, useMemo } from "react";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
} from "@/components/ui/dialog";
import { ScrollArea } from "@/components/ui/scroll-area";
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
  Flame,
  Zap,
  Clock,
  Siren,
} from "lucide-react";
import { Feed, Comment } from "@/types/dashboard.types";
import { commentApi, reportApi, getCdnUrl } from "@/api";
import { toast } from "sonner";

// 백엔드 댓글 응답 타입
interface BackendComment {
  id: number;
  writerName?: string;
  writerAvatar?: string;
  writerDepartment?: string;
  writerActiveDays?: number;
  content: string;
  createdAt: string;
  likeCount?: number;
  isLiked?: boolean;
  [key: string]: unknown;
}
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { FeedContentDisplay } from "@/components/shared/FeedContent";
import { getRelativeTime } from "@/lib/utils";
import { useImageBrightness } from "@/hooks";
import { UserHoverCard } from "@/components/dashboard/shared/UserHoverCard";

interface FeedDetailDialogHomeProps {
  feed: Feed | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  currentImageIndex: number;
  onPrevImage: () => void;
  onNextImage: () => void;
  onEdit?: (feed: Feed) => void;
  onDelete?: (feedId: number) => void;
  targetCommentId?: number | null;
}

export default function FeedDetailDialogHome({
  feed,
  open,
  onOpenChange,
  currentImageIndex,
  onPrevImage,
  onNextImage,
  onEdit,
  onDelete,
  targetCommentId,
}: FeedDetailDialogHomeProps) {
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
  const [feedReported, setFeedReported] = useState(false);
  const [commentReported, setCommentReported] = useState<{ [key: number]: boolean }>({});

  // 현재 사용자 정보
  const currentUserName = localStorage.getItem("userName") || "알 수 없음";
  const currentUserId = parseInt(localStorage.getItem("userId") || "1");

  // 이미지 밝기에 따른 아이콘 색상 결정 (CDN URL 사용)
  const hasImages = feed?.images && feed.images.length > 0;
  const currentImageUrl = hasImages ? getCdnUrl(feed.images[currentImageIndex]) : undefined;
  const iconColor = useImageBrightness(currentImageUrl);
  const iconColorClass = iconColor === "white" ? "text-white" : "text-gray-900";

  // Feed가 변경되면 해당 피드의 댓글 로드
  useEffect(() => {
    const fetchComments = async () => {
      if (!feed) {
        setComments([]);
        return;
      }

      try {
        const response = await commentApi.getCommentsByFeedId(feed.id, 0, 100);
        const allComments = response.content || response;
        // parentId가 있는 댓글은 답글이므로 최상위 목록에서 제외
        const commentList = allComments.filter((c: BackendComment & { parentId?: number }) => !c.parentId);

        // 아바타 URL 변환 헬퍼 (CDN 사용)
        const getAvatarUrl = (avatarUrl?: string): string => {
          if (!avatarUrl) return "";
          return getCdnUrl(avatarUrl);
        };

        // 답글 정보도 함께 로드하고 필드 매핑
        const commentsWithReplies = await Promise.all(
          commentList.map(async (comment: BackendComment) => {
            try {
              const repliesData = await commentApi.getRepliesByCommentId(comment.id);
              const replies = (repliesData || []).map((reply: BackendComment) => ({
                ...reply,
                author: reply.writerName || "알 수 없음",
                avatar: getAvatarUrl(reply.writerAvatar),
                department: reply.writerDepartment,
                activeDays: reply.writerActiveDays,
                time: getRelativeTime(reply.createdAt),
                likeCount: reply.likeCount || 0,
                isLiked: reply.isLiked || false,
              }));
              return {
                ...comment,
                author: comment.writerName || "알 수 없음",
                avatar: getAvatarUrl(comment.writerAvatar),
                department: comment.writerDepartment,
                activeDays: comment.writerActiveDays,
                time: getRelativeTime(comment.createdAt),
                likeCount: comment.likeCount || 0,
                isLiked: comment.isLiked || false,
                replies,
              };
            } catch {
              return {
                ...comment,
                author: comment.writerName || "알 수 없음",
                avatar: getAvatarUrl(comment.writerAvatar),
                department: comment.writerDepartment,
                activeDays: comment.writerActiveDays,
                time: getRelativeTime(comment.createdAt),
                likeCount: comment.likeCount || 0,
                isLiked: comment.isLiked || false,
                replies: [],
              };
            }
          })
        );

        setComments(commentsWithReplies);

        // commentLikes 상태 초기화
        const likesState: { [key: number]: { liked: boolean; count: number } } = {};
        commentsWithReplies.forEach((comment) => {
          likesState[comment.id] = {
            liked: comment.isLiked || false,
            count: comment.likeCount || 0,
          };
          // 답글도 초기화
          if (comment.replies) {
            comment.replies.forEach((reply: Comment) => {
              likesState[reply.id] = {
                liked: reply.isLiked || false,
                count: reply.likeCount || 0,
              };
            });
          }
        });
        setCommentLikes(likesState);

        // targetCommentId가 있으면 댓글창 열기 (-1은 "댓글 창만 열기" 신호)
        if (targetCommentId !== null && targetCommentId !== undefined) {
          setShowComments(true);
        }
      } catch (error) {
        console.error("댓글 데이터 로드 실패:", error);
        setComments([]);
      }

      // targetCommentId가 없을 때만 댓글창 닫기
      if (!targetCommentId) {
        setShowComments(false);
      }
    };

    fetchComments();
  }, [feed, targetCommentId]);

  // 피드 신고 핸들러
  const handleReportFeed = async () => {
    if (!feed || feedReported) return;
    try {
      await reportApi.reportFeed(feed.id);
      setFeedReported(true);
      toast.success("신고가 접수되었습니다.");
    } catch {
      toast.error("신고에 실패했습니다.");
    }
  };

  // 댓글 신고 핸들러
  const handleReportComment = async (commentId: number) => {
    if (commentReported[commentId]) return;
    try {
      await reportApi.reportComment(commentId);
      setCommentReported(prev => ({ ...prev, [commentId]: true }));
      toast.success("신고가 접수되었습니다.");
    } catch {
      toast.error("신고에 실패했습니다.");
    }
  };

  // targetCommentId가 양수이면 해당 댓글로 스크롤 및 하이라이트 (-1은 무시)
  useEffect(() => {
    if (targetCommentId && targetCommentId > 0 && open && comments.length > 0 && showComments) {
      const numTargetId = Number(targetCommentId);
      console.log('[Highlight] 조건 충족, targetCommentId:', numTargetId);

      // targetCommentId가 답글인지 확인하고, 답글이면 부모 댓글 펼치기
      let parentIdToExpand: number | null = null;
      for (const comment of comments) {
        // 최상위 댓글인지 확인
        if (Number(comment.id) === numTargetId) {
          console.log('[Highlight] 최상위 댓글 찾음');
          break; // 최상위 댓글이면 펼칠 필요 없음
        }
        // 답글인지 확인
        if (comment.replies) {
          const reply = comment.replies.find(r => Number(r.id) === numTargetId);
          if (reply) {
            parentIdToExpand = comment.id;
            console.log('[Highlight] 답글 찾음, 부모:', parentIdToExpand);
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

      // DOM 업데이트 후 스크롤 (500ms로 증가)
      // 모바일/데스크톱 두 패널에 같은 ID가 있으므로 querySelectorAll 사용
      setTimeout(() => {
        const commentElements = document.querySelectorAll(`[id="comment-${targetCommentId}"]`);
        console.log('[Highlight] 요소 검색:', `comment-${targetCommentId}`, commentElements.length + '개 찾음');

        commentElements.forEach((commentElement) => {
          const el = commentElement as HTMLElement;
          // 보이는 요소만 스크롤
          if (el.offsetParent !== null) {
            el.scrollIntoView({ behavior: 'smooth', block: 'center' });
          }

          // 하이라이트 효과 - 더 명확한 스타일 (배경색 + 테두리)
          el.style.backgroundColor = '#fef3c7'; // amber-100
          el.style.borderRadius = '8px';
          el.style.boxShadow = '0 0 0 2px #f59e0b'; // amber-500 ring

          console.log('[Highlight] 스타일 적용:', el.id);

          setTimeout(() => {
            el.style.backgroundColor = '';
            el.style.borderRadius = '';
            el.style.boxShadow = '';
          }, 3000);
        });
      }, 500);
    }
  }, [targetCommentId, open, comments, showComments, collapsedComments]);

  const handleSendComment = async () => {
    if (commentText.trim() && feed) {
      try {
        // API 호출하여 댓글 생성
        const response = await commentApi.createComment({
          content: commentText,
          feedId: feed.id,
          writerId: currentUserId,
        });

        // 새 댓글을 리스트에 추가 (서버에서 받은 아바타 URL 사용)
        const authorName = response.writerName || currentUserName;
        const avatarUrl = response.writerAvatar ? getCdnUrl(response.writerAvatar) : "";
        const newComment: Comment = {
          id: response.id,
          author: authorName,
          avatar: avatarUrl || authorName.charAt(0),
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
    if (replyCommentText.trim() && feed && replyingTo !== null) {
      try {
        // API 호출하여 답글 생성
        const response = await commentApi.createComment({
          content: replyCommentText,
          feedId: feed.id,
          writerId: currentUserId,
          parentId: replyingTo,
        });

        // 새 답글을 해당 댓글에 추가 (서버에서 받은 아바타 URL 사용)
        const replyAuthorName = response.writerName || currentUserName;
        const replyAvatarUrl = response.writerAvatar ? getCdnUrl(response.writerAvatar) : "";
        const newReply: Comment = {
          id: response.id,
          author: replyAuthorName,
          avatar: replyAvatarUrl || replyAuthorName.charAt(0),
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

  // 모든 댓글 수 계산 (답글 포함)
  const countAllComments = (commentList: Comment[]): number => {
    let count = 0;
    for (const comment of commentList) {
      count += 1; // 현재 댓글
      if (comment.replies && comment.replies.length > 0) {
        count += countAllComments(comment.replies); // 답글 재귀 계산
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
    } else {
      return sorted.sort((a, b) => b.id - a.id); // 최신순 (id가 클수록 최신)
    }
  }, [comments, sortOrder, commentLikes]);

  if (!feed) return null;

  // 댓글 좋아요 토글
  const toggleCommentLike = async (commentId: number) => {
    const current = commentLikes[commentId] || { liked: false, count: 0 };
    const newLiked = !current.liked;

    // 낙관적 업데이트
    setCommentLikes((prev) => ({
      ...prev,
      [commentId]: {
        liked: newLiked,
        count: newLiked
          ? current.count + 1
          : Math.max(0, current.count - 1),
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

  // 댓글 접기/펼치기 토글
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

  // 댓글 삭제
  const handleDeleteComment = async (commentId: number) => {
    // 루트 댓글인지 확인
    const isRootComment = comments.some(c => c.id === commentId);
    const confirmMessage = isRootComment && comments.find(c => c.id === commentId)?.replies?.length
      ? "이 댓글과 모든 답글이 삭제됩니다. 삭제하시겠습니까?"
      : "댓글을 삭제하시겠습니까?";

    if (!confirm(confirmMessage)) return;

    try {
      await commentApi.deleteComment(commentId);

      // 로컬 상태에서 댓글 처리
      setComments(prevComments => {
        return prevComments
          .filter(c => c.id !== commentId) // 루트 댓글 완전 삭제 (답글도 함께 삭제)
          .map(c => ({
            ...c,
            replies: c.replies?.filter(r => r.id !== commentId) || [] // 다른 루트 댓글의 답글에서 삭제
          }));
      });
    } catch (error) {
      console.error("댓글 삭제 실패:", error);
      alert("댓글 삭제에 실패했습니다.");
    }
  };

  // 댓글 렌더링 함수 (재귀적으로 대댓글 표시)
  const renderComment = (comment: Comment, depth: number = 0) => {
    const isReply = depth > 0;
    const hasReplies = comment.replies && comment.replies.length > 0;
    const isCollapsed = collapsedComments.has(comment.id);
    const isReplying = replyingTo === comment.id;
    const likeInfo = commentLikes[comment.id] || { liked: false, count: 0 };
    const isDeleted = comment.isDeleted;

    return (
      <div key={comment.id} className={isReply ? "ml-8 mt-3" : ""}>
        {/* 하이라이트 가능한 영역 - 이 댓글만 포함, 답글은 제외 */}
        <div id={`comment-${comment.id}`} className="transition-colors duration-500">
          <div className="flex gap-3">
            <UserHoverCard
              name={comment.author}
              department={comment.department}
              activeDays={comment.activeDays}
              avatarUrl={comment.avatar || undefined}
              size="sm"
            />
            <div className="flex-1 min-w-0">
              {isDeleted ? (
                <>
                  <p className="text-sm text-gray-400 italic mb-2">
                    삭제된 댓글입니다.
                  </p>
                </>
              ) : (
                <>
                  <div className="flex items-center gap-2 mb-1">
                    <span className="font-bold text-sm text-gray-900">
                      {comment.author}
                    </span>
                    <span className="text-xs text-gray-500">{comment.time}</span>
                  </div>
                  <p className="text-sm text-gray-900 break-words mb-2">
                    {comment.content}
                  </p>

                  {/* 좋아요 + 답글 + 삭제 버튼 */}
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
                    {/* 답글 버튼 - 루트 댓글에만 표시 */}
                    {depth === 0 && (
                      <button
                        onClick={() => setReplyingTo(replyingTo === comment.id ? null : comment.id)}
                        className="text-xs text-gray-600 hover:text-[#C93831] font-semibold cursor-pointer"
                      >
                        답글
                      </button>
                    )}
                    {/* 삭제 버튼 - 내가 쓴 댓글만 */}
                    {comment.author === currentUserName && (
                      <button
                        onClick={() => handleDeleteComment(comment.id)}
                        className="text-xs text-gray-600 hover:text-red-500 font-semibold cursor-pointer"
                      >
                        삭제
                      </button>
                    )}
                    {/* 신고 버튼 - 다른 사람 댓글만 */}
                    {comment.author !== currentUserName && (
                      <button
                        onClick={() => handleReportComment(comment.id)}
                        disabled={commentReported[comment.id]}
                        className={`text-xs font-semibold cursor-pointer ${commentReported[comment.id] ? 'text-red-500' : 'text-gray-600 hover:text-red-500'}`}
                      >
                        {commentReported[comment.id] ? '신고됨' : '신고'}
                      </button>
                    )}
                  </div>
                </>
              )}

              {/* 답글 입력 칸 (답글 버튼 클릭 시) */}
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

              {/* 접기/펼치기 버튼 */}
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
        </div>

        {/* 대댓글 표시 - id 밖에 위치하여 부모 하이라이트 시 영향 받지 않음 */}
        {hasReplies && !isCollapsed && (
          <div className="relative mt-2 pl-2 border-l-2 border-gray-300">
            {comment.replies!.map((reply) => renderComment(reply, depth + 1))}
          </div>
        )}
      </div>
    );
  };

  return (
    <>
      {/* 모바일용 전체화면 댓글 오버레이 - Dialog 밖에 위치하여 fixed positioning 작동 */}
      {open && showComments && (
        <div className="md:hidden fixed inset-x-0 top-0 bottom-[60px] z-[60] bg-white flex flex-col">
          <div className="flex items-center justify-between p-4 border-b">
            <h3 className="font-bold text-lg">댓글 {totalCommentCount}개</h3>
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
              <button
                onClick={() => setShowComments(false)}
                className="p-2 hover:bg-gray-100 rounded-full cursor-pointer"
              >
                <X className="w-5 h-5" />
              </button>
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
          <div className="p-4 border-t border-gray-200/50">
            <div className="flex gap-2 items-center">
              <div className="relative flex-1">
                <input
                  type="text"
                  placeholder="댓글을 입력하세요..."
                  value={commentText}
                  onChange={(e) => setCommentText(e.target.value)}
                  onKeyPress={(e) => e.key === "Enter" && handleSendComment()}
                  className="w-full py-2 text-sm bg-transparent border-b-2 border-gray-300 focus:border-[#C93831] outline-none pr-8"
                />
                {commentText && (
                  <button
                    onClick={() => setCommentText("")}
                    className="absolute right-0 top-1/2 -translate-y-1/2 w-5 h-5 rounded-full bg-gray-200 hover:bg-gray-300 flex items-center justify-center cursor-pointer"
                  >
                    <X className="w-3 h-3 text-gray-600" />
                  </button>
                )}
              </div>
              <button
                onClick={handleSendComment}
                disabled={!commentText.trim()}
                className="w-10 h-10 rounded-full bg-gradient-to-r from-[#C93831] to-[#B02F28] text-white flex items-center justify-center hover:shadow-lg transition-shadow disabled:opacity-50 disabled:cursor-not-allowed flex-shrink-0 cursor-pointer"
              >
                <Send className="w-4 h-4" />
              </button>
            </div>
          </div>
        </div>
      )}

      <Dialog open={open} onOpenChange={onOpenChange}>
        <DialogContent
          className={`block p-0 gap-0 h-[calc(100vh-130px)] max-h-[calc(100vh-130px)] w-fit max-w-none md:max-w-[calc(100vw-32px)] md:h-[95vh] md:max-h-[95vh] overflow-hidden backdrop-blur-2xl bg-white/60 border-0 shadow-2xl transition-all duration-300 ${
            showComments
              ? "md:!w-[825px] md:!max-w-[825px]"
              : "md:!w-[475px] md:!max-w-[475px]"
          }`}
          onPointerDownOutside={(e) => {
            // 모바일에서만 댓글 오버레이 클릭 시 다이얼로그 닫힘 방지
            const isMobile = window.innerWidth < 768;
            if (showComments && isMobile) {
              e.preventDefault();
            }
          }}
          onInteractOutside={(e) => {
            // 모바일에서만 댓글 오버레이와 상호작용 시 다이얼로그 닫힘 방지
            const isMobile = window.innerWidth < 768;
            if (showComments && isMobile) {
              e.preventDefault();
            }
          }}
        >
        <DialogHeader className="sr-only">
          <DialogTitle>피드 상세보기</DialogTitle>
          <DialogDescription>
            피드의 상세 내용을 확인할 수 있습니다.
          </DialogDescription>
        </DialogHeader>
        <div className="relative h-full flex flex-col md:flex-row overflow-hidden">
          {/* Main Feed Content (Left) - 모바일에서 h-full aspect-[9/16] (Feed.tsx와 동일) */}
          <div className="h-full aspect-[9/16] max-w-[calc(100vw-32px)] md:aspect-auto md:w-[475px] md:max-w-[475px] flex-shrink-0 flex flex-col overflow-hidden">
            {feed.images && feed.images.length > 0 ? (
              <>
                {/* Image Carousel - 모바일에서 57% 높이 (Feed.tsx와 동일) */}
                <div className="relative h-[57%] md:h-[545px] w-full md:max-w-[475px] overflow-hidden">
                  <img
                    src={getCdnUrl(feed.images[currentImageIndex] || feed.images[0])}
                    alt={feed.activity}
                    className="w-full h-full object-cover"
                  />

                  {feed.images.length > 1 && (
                    <>
                      {currentImageIndex > 0 && (
                        <button
                          onClick={onPrevImage}
                          className="absolute left-2 top-1/2 -translate-y-1/2 cursor-pointer hover:scale-110 transition-transform"
                        >
                          <ChevronLeft className={`w-8 h-8 ${iconColorClass}`} />
                        </button>
                      )}
                      {currentImageIndex < feed.images.length - 1 && (
                        <button
                          onClick={onNextImage}
                          className="absolute right-2 top-1/2 -translate-y-1/2 cursor-pointer hover:scale-110 transition-transform"
                        >
                          <ChevronRight className={`w-8 h-8 ${iconColorClass}`} />
                        </button>
                      )}
                      <div className="absolute bottom-2 left-1/2 -translate-x-1/2 flex gap-1">
                        {feed.images.map((_, idx) => (
                          <div
                            key={idx}
                            className={`w-1.5 h-1.5 rounded-full ${
                              idx === currentImageIndex
                                ? (iconColor === "white" ? "bg-white" : "bg-gray-900")
                                : (iconColor === "white" ? "bg-white/50" : "bg-gray-900/50")
                            }`}
                          ></div>
                        ))}
                      </div>
                    </>
                  )}

                  {/* Author Avatar with HoverCard */}
                  <div className="absolute top-4 left-4">
                    <UserHoverCard
                      name={feed.author || feed.writerName}
                      department={feed.writerDepartment}
                      activeDays={feed.writerActiveDays}
                      avatarUrl={feed.writerAvatar}
                    />
                  </div>

                  {/* Menu Button */}
                  <div className="absolute top-4 right-4">
                    <DropdownMenu>
                      <DropdownMenuTrigger asChild>
                        <button className="cursor-pointer hover:scale-110 transition-transform">
                          <MoreVertical className={`w-6 h-6 ${iconColorClass}`} />
                        </button>
                      </DropdownMenuTrigger>
                      <DropdownMenuContent
                        align="end"
                        className="w-48 bg-white/95 backdrop-blur-xl border border-gray-200"
                      >
                        <DropdownMenuItem
                          onClick={() => {
                            onOpenChange(false);
                            onEdit?.(feed);
                          }}
                          className="flex items-center gap-2 cursor-pointer text-gray-900 hover:bg-gray-100"
                        >
                          <Edit className="w-4 h-4" />
                          <span className="font-medium">수정</span>
                        </DropdownMenuItem>
                        <DropdownMenuItem
                          onClick={() => {
                            if (window.confirm("정말 삭제하시겠습니까?")) {
                              onDelete?.(feed.id);
                              onOpenChange(false);
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

                  {/* Right Actions */}
                  <div className="absolute right-4 bottom-4 flex flex-col gap-4 z-10">
                    <div className="flex flex-col items-center gap-1">
                      <Heart className="w-6 h-6 text-[#C93831] fill-[#C93831]" />
                      <span className={`text-xs font-bold ${iconColorClass}`}>{feed.likes}</span>
                    </div>

                    <button
                      className="flex flex-col items-center gap-1 cursor-pointer hover:scale-110 transition-transform"
                      onClick={() => setShowComments(!showComments)}
                    >
                      <MessageCircle className={`w-6 h-6 ${iconColorClass} ${showComments ? (iconColor === "white" ? "fill-white" : "fill-gray-900") : ""}`} />
                      <span className={`text-xs font-bold ${iconColorClass}`}>{totalCommentCount}</span>
                    </button>
                  </div>
                </div>
              </>
            ) : (
              <>
                {/* No Image Layout */}
                <div className="relative p-6 bg-transparent h-[545px]">
                  <div className="flex items-center justify-between">
                    <UserHoverCard
                      name={feed.author || feed.writerName}
                      department={feed.writerDepartment}
                      activeDays={feed.writerActiveDays}
                      avatarUrl={feed.writerAvatar}
                    />

                    {/* Menu Button */}
                    <DropdownMenu>
                      <DropdownMenuTrigger asChild>
                        <button className="cursor-pointer hover:scale-110 transition-transform">
                          <MoreVertical className={`w-6 h-6 ${iconColorClass}`} />
                        </button>
                      </DropdownMenuTrigger>
                      <DropdownMenuContent
                        align="end"
                        className="w-48 bg-white/95 backdrop-blur-xl border border-gray-200"
                      >
                        <DropdownMenuItem
                          onClick={() => {
                            onOpenChange(false);
                            onEdit?.(feed);
                          }}
                          className="flex items-center gap-2 cursor-pointer text-gray-900 hover:bg-gray-100"
                        >
                          <Edit className="w-4 h-4" />
                          <span className="font-medium">수정</span>
                        </DropdownMenuItem>
                        <DropdownMenuItem
                          onClick={() => {
                            if (window.confirm("정말 삭제하시겠습니까?")) {
                              onDelete?.(feed.id);
                              onOpenChange(false);
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

                  {/* Right Actions for No-Image Posts */}
                  <div className="absolute right-4 bottom-4 flex flex-col gap-4 z-10">
                    <div className="flex flex-col items-center gap-1">
                      <Heart className="w-6 h-6 text-[#C93831] fill-[#C93831]" />
                      <span className={`text-xs font-bold ${iconColorClass}`}>{feed.likes}</span>
                    </div>

                    <button
                      className="flex flex-col items-center gap-1 cursor-pointer hover:scale-110 transition-transform"
                      onClick={() => setShowComments(!showComments)}
                    >
                      <MessageCircle className={`w-6 h-6 ${iconColorClass} ${showComments ? (iconColor === "white" ? "fill-white" : "fill-gray-900") : ""}`} />
                      <span className={`text-xs font-bold ${iconColorClass}`}>{totalCommentCount}</span>
                    </button>

                    <button
                      className="flex flex-col items-center gap-1 cursor-pointer hover:scale-110 transition-transform"
                      onClick={handleReportFeed}
                    >
                      <Siren className={`w-6 h-6 ${feedReported ? "text-[#C93831] fill-[#C93831]" : iconColorClass}`} />
                    </button>
                  </div>
                </div>
              </>
            )}

            {/* Feed Content (Always visible) - 모바일에서 43% 높이 (Feed.tsx와 동일) */}
            <ScrollArea
              className="h-[43%] md:flex-1 bg-transparent w-full md:w-[475px] md:max-w-[475px]"
            >
              <div className="p-4 md:p-6 space-y-3">
                <div className="space-y-3 w-full md:max-w-[427px]">
                  {/* Badges */}
                  <div className="flex items-center justify-between">
                    <div className="flex gap-2 flex-wrap">
                      <Badge className="bg-amber-50 text-amber-600 font-medium border border-amber-200">
                        <Sparkles className="w-3 h-3 mr-1" />+{feed.points}
                      </Badge>
                      <Badge className="bg-blue-50 text-blue-600 font-medium border border-blue-200">
                        <Zap className="w-3 h-3 mr-1" />{feed.activity}
                      </Badge>
                      {(feed.calories ?? 0) > 0 && (
                        <Badge className="bg-orange-50 text-orange-600 font-medium border border-orange-200">
                          <Flame className="w-3 h-3 mr-1" />{feed.calories}kcal
                        </Badge>
                      )}
                    </div>
                    <Badge className="bg-slate-50 text-slate-500 font-medium border border-slate-200">
                      <Clock className="w-3 h-3 mr-1" />{feed.time}
                    </Badge>
                  </div>

                  {/* Content */}
                  <FeedContentDisplay content={feed?.content || ""} />
                </div>
              </div>
            </ScrollArea>
          </div>

          {/* Comments Panel - 데스크톱용 사이드 패널 (모바일은 Dialog 밖에 위치) */}
          {showComments && (
              <div className="hidden md:flex flex-1 bg-transparent border-l border-gray-200/30 flex-col overflow-hidden">
                <div className="px-6 py-4 border-b border-gray-200/50 flex items-center justify-between">
                  <h3 className="text-lg font-bold text-gray-900">댓글 {totalCommentCount}개</h3>
                  <div className="relative">
                    <button
                      onClick={() => setShowSortMenu(!showSortMenu)}
                      className="flex items-center gap-1 px-3 py-1.5 rounded-lg hover:bg-gray-100 transition-colors cursor-pointer"
                    >
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
                <div className="p-4 border-t border-gray-200/50">
                  <div className="flex gap-2 items-center">
                    <div className="relative flex-1">
                      <input
                        type="text"
                        placeholder="댓글을 입력하세요..."
                        value={commentText}
                        onChange={(e) => setCommentText(e.target.value)}
                        onKeyPress={(e) => e.key === "Enter" && handleSendComment()}
                        className="w-full py-2 text-sm bg-transparent border-b-2 border-gray-300 focus:border-[#C93831] outline-none pr-8"
                      />
                      {commentText && (
                        <button
                          onClick={() => setCommentText("")}
                          className="absolute right-0 top-1/2 -translate-y-1/2 w-5 h-5 rounded-full bg-gray-200 hover:bg-gray-300 flex items-center justify-center cursor-pointer"
                        >
                          <X className="w-3 h-3 text-gray-600" />
                        </button>
                      )}
                    </div>
                    <button
                      onClick={handleSendComment}
                      disabled={!commentText.trim()}
                      className="w-10 h-10 rounded-full bg-gradient-to-r from-[#C93831] to-[#B02F28] text-white flex items-center justify-center hover:shadow-lg transition-shadow disabled:opacity-50 disabled:cursor-not-allowed flex-shrink-0 cursor-pointer"
                    >
                      <Send className="w-4 h-4" />
                    </button>
                  </div>
                </div>
              </div>
          )}
        </div>
        </DialogContent>
      </Dialog>
    </>
  );
}
