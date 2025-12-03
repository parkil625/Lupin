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
  Flame,
  Zap,
  Clock,
  User,
  Siren,
} from "lucide-react";
import { Feed, Comment } from "@/types/dashboard.types";
import { commentApi, reportApi } from "@/api";
import { toast } from "sonner";

// 백엔드 댓글 응답 타입
interface BackendComment {
  id: number;
  writerName?: string;
  content: string;
  createdAt: string;
  [key: string]: unknown;
}
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { useCreateBlockNote } from "@blocknote/react";
import { BlockNoteView } from "@blocknote/mantine";
import "@blocknote/mantine/style.css";
import { getRelativeTime } from "@/lib/utils";
import { useImageBrightness } from "@/hooks";

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

  // 이미지 밝기에 따른 아이콘 색상 결정
  const hasImages = feed?.images && feed.images.length > 0;
  const currentImageUrl = hasImages ? feed.images[currentImageIndex] : undefined;
  const iconColor = useImageBrightness(currentImageUrl);
  const iconColorClass = iconColor === "white" ? "text-white" : "text-gray-900";

  // BlockNote 에디터 생성 (읽기 전용)
  const feedContent = feed?.content;
  const initialContent = useMemo(() => {
    if (!feedContent) return undefined;
    try {
      // JSON인지 확인
      const parsed = JSON.parse(feedContent);
      return parsed;
    } catch {
      // 일반 텍스트인 경우 BlockNote 기본 형식으로 변환
      return [
        {
          type: "paragraph" as const,
          content: [{ type: "text" as const, text: feedContent, styles: {} }],
        },
      ];
    }
  }, [feedContent]);

  const editor = useCreateBlockNote({
    initialContent,
  });

  // Feed가 변경되면 에디터 콘텐츠 업데이트
  useEffect(() => {
    if (feed?.content && editor) {
      try {
        // JSON인지 확인
        const blocks = JSON.parse(feed.content);
        editor.replaceBlocks(editor.document, blocks);
      } catch {
        // 일반 텍스트인 경우 BlockNote 기본 형식으로 변환
        const textBlocks = [
          {
            type: "paragraph" as const,
            content: [{ type: "text" as const, text: feed.content, styles: {} }],
          },
        ];
        editor.replaceBlocks(editor.document, textBlocks);
      }
    }
  }, [feed?.content, editor]);

  // Feed가 변경되면 해당 피드의 댓글 로드
  useEffect(() => {
    const fetchComments = async () => {
      if (!feed) {
        setComments([]);
        return;
      }

      try {
        const response = await commentApi.getCommentsByFeedId(feed.id, 0, 100);
        const commentList = response.content || response;

        // 답글 정보도 함께 로드하고 필드 매핑
        const commentsWithReplies = await Promise.all(
          commentList.map(async (comment: BackendComment) => {
            try {
              const repliesData = await commentApi.getRepliesByCommentId(comment.id);
              const replies = (repliesData || []).map((reply: BackendComment) => ({
                ...reply,
                author: reply.writerName || "알 수 없음",
                avatar: (reply.writerName || "?").charAt(0),
                time: getRelativeTime(reply.createdAt),
              }));
              return {
                ...comment,
                author: comment.writerName || "알 수 없음",
                avatar: (comment.writerName || "?").charAt(0),
                time: getRelativeTime(comment.createdAt),
                replies,
              };
            } catch {
              return {
                ...comment,
                author: comment.writerName || "알 수 없음",
                avatar: (comment.writerName || "?").charAt(0),
                time: getRelativeTime(comment.createdAt),
                replies: [],
              };
            }
          })
        );

        setComments(commentsWithReplies);

        // targetCommentId가 있으면 댓글창 열기
        if (targetCommentId) {
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

  // targetCommentId가 있으면 해당 댓글로 스크롤
  useEffect(() => {
    if (targetCommentId && open && comments.length > 0 && showComments) {
      // DOM 업데이트 후 스크롤
      setTimeout(() => {
        const commentElement = document.getElementById(`comment-${targetCommentId}`);
        if (commentElement) {
          commentElement.scrollIntoView({ behavior: 'smooth', block: 'center' });
          // 하이라이트 효과 (인라인 스타일 사용, 3초)
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
  }, [targetCommentId, open, comments, showComments]);

  const handleSendComment = async () => {
    if (commentText.trim() && feed) {
      try {
        // API 호출하여 댓글 생성
        const response = await commentApi.createComment({
          content: commentText,
          feedId: feed.id,
          writerId: currentUserId,
        });

        // 새 댓글을 리스트에 추가
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
    if (replyCommentText.trim() && feed && replyingTo !== null) {
      try {
        // API 호출하여 답글 생성
        const response = await commentApi.createComment({
          content: replyCommentText,
          feedId: feed.id,
          writerId: currentUserId,
          parentId: replyingTo,
        });

        // 새 답글을 해당 댓글에 추가
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
    if (!confirm("댓글을 삭제하시겠습니까?")) return;

    try {
      await commentApi.deleteComment(commentId);

      // 로컬 상태에서 댓글 처리
      setComments(prevComments => {
        return prevComments.map(c => {
          // 최상위 댓글인 경우
          if (c.id === commentId) {
            // 답글이 있으면 "삭제된 댓글"로 표시
            if (c.replies && c.replies.length > 0) {
              return {
                ...c,
                author: "",
                content: "삭제된 댓글입니다.",
                isDeleted: true,
              };
            }
            // 답글이 없으면 완전히 제거
            return null;
          }

          // 답글에서 삭제
          return {
            ...c,
            replies: c.replies?.filter(r => r.id !== commentId) || []
          };
        }).filter(Boolean) as Comment[];
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

        {/* 대댓글 표시 (접혀있지 않을 때만) */}
        {hasReplies && !isCollapsed && (
          <div className="relative mt-2 pl-2 border-l-2 border-gray-300">
            {comment.replies!.map((reply) => renderComment(reply, depth + 1))}
          </div>
        )}
      </div>
    );
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent
        className={`p-0 w-full h-[calc(100%-70px)] max-h-[calc(100vh-70px)] md:h-[95vh] md:max-h-[95vh] overflow-hidden backdrop-blur-2xl bg-white/60 border border-gray-200/30 shadow-2xl transition-all duration-300 ${
          showComments
            ? "md:!w-[825px] md:!max-w-[825px]"
            : "md:!w-[475px] md:!max-w-[475px]"
        }`}
      >
        <DialogHeader className="sr-only">
          <DialogTitle>피드 상세보기</DialogTitle>
          <DialogDescription>
            피드의 상세 내용을 확인할 수 있습니다.
          </DialogDescription>
        </DialogHeader>
        <div className="relative h-full flex flex-col md:flex-row overflow-hidden">
          {/* Main Feed Content (Left) */}
          <div className="w-full md:w-[475px] md:max-w-[475px] flex-shrink-0 flex flex-col overflow-hidden">
            {feed.images && feed.images.length > 0 ? (
              <>
                {/* Image Carousel */}
                <div className="relative h-[50vh] md:h-[545px] w-full md:max-w-[475px] overflow-hidden flex-shrink-0">
                  <img
                    src={feed.images[currentImageIndex] || feed.images[0]}
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

                  {/* Author Avatar Only */}
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
                          onClick={() => onEdit?.(feed)}
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
                    <button className="flex flex-col items-center gap-1 cursor-pointer hover:scale-110 transition-transform">
                      <Heart className={`w-6 h-6 ${iconColorClass} ${iconColor === "white" ? "fill-white" : "fill-gray-900"}`} />
                      <span className={`text-xs font-bold ${iconColorClass}`}>{feed.likes}</span>
                    </button>

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
                    <Avatar className="w-10 h-10 border-2 border-gray-300 shadow-lg">
                      {feed.writerAvatar ? (
                        <img src={feed.writerAvatar} alt={feed.writerName} className="w-full h-full object-cover" />
                      ) : (
                        <AvatarFallback className="bg-white">
                          <User className="w-5 h-5 text-gray-400" />
                        </AvatarFallback>
                      )}
                    </Avatar>

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
                          onClick={() => onEdit?.(feed)}
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
                    <button className="flex flex-col items-center gap-1 cursor-pointer hover:scale-110 transition-transform">
                      <Heart className={`w-6 h-6 ${iconColorClass} ${iconColor === "white" ? "fill-white" : "fill-gray-900"}`} />
                      <span className={`text-xs font-bold ${iconColorClass}`}>{feed.likes}</span>
                    </button>

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

            {/* Feed Content (Always visible) */}
            <ScrollArea
              className="bg-white/50 backdrop-blur-sm flex-1 w-full md:w-[475px] md:max-w-[475px]"
            >
              <div className="p-4 md:p-6 space-y-3">
                <style>{`
                  .bn-container {
                    max-width: 100% !important;
                    width: 100% !important;
                    background: transparent !important;
                  }
                  .bn-editor {
                    max-width: 100% !important;
                    width: 100% !important;
                    padding: 0 !important;
                    background: transparent !important;
                  }
                  .bn-block-content {
                    max-width: 100% !important;
                  }
                  .ProseMirror {
                    background: transparent !important;
                    color: #111827 !important;
                  }
                  .ProseMirror p, .ProseMirror h1, .ProseMirror h2, .ProseMirror h3, .ProseMirror h4, .ProseMirror h5, .ProseMirror h6, .ProseMirror li, .ProseMirror span {
                    color: #111827 !important;
                  }
                  @media (min-width: 768px) {
                    .bn-container, .bn-editor, .bn-block-content {
                      max-width: 427px !important;
                      width: 427px !important;
                    }
                  }
                `}</style>
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

                  {/* Content */}
                  <div className="text-gray-900 font-medium text-sm leading-relaxed">
                    <BlockNoteView
                      editor={editor}
                      editable={false}
                      theme="light"
                    />
                  </div>
                </div>
              </div>
            </ScrollArea>
          </div>

          {/* Comments Panel (Right - slides in) */}
          {showComments && (
            <div className="flex-1 bg-white/50 backdrop-blur-sm border-l border-gray-200/50 flex flex-col overflow-hidden">
              {/* Comments Header */}
              <div className="px-6 py-4 border-b border-gray-200/50 flex items-center justify-between">
                <h3 className="text-lg font-bold text-gray-900">
                  댓글 {totalCommentCount}개
                </h3>
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

              {/* Comment Input - 일반 댓글 작성용 (항상 표시) */}
              <div className="p-4 border-t border-gray-200/50">
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
                        className="absolute right-0 top-1/2 -translate-y-1/2 w-5 h-5 rounded-full bg-gray-200 hover:bg-gray-300 flex items-center justify-center transition-colors cursor-pointer"
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
  );
}
