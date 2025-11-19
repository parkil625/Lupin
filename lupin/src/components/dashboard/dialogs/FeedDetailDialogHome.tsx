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
  Pencil,
  Flame,
  Zap,
  User,
} from "lucide-react";
import { Feed, Comment } from "@/types/dashboard.types";
import { commentApi } from "@/api";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { useCreateBlockNote } from "@blocknote/react";
import { BlockNoteView } from "@blocknote/mantine";
import "@blocknote/mantine/style.css";

interface FeedDetailDialogHomeProps {
  feed: Feed | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  currentImageIndex: number;
  onPrevImage: () => void;
  onNextImage: () => void;
  onEdit?: (feed: Feed) => void;
  onDelete?: (feedId: number) => void;
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
  const [iconColor, setIconColor] = useState<'white' | 'black'>('white');

  // 이미지 밝기 분석하여 아이콘 색상 결정
  useEffect(() => {
    if (feed?.images && feed.images.length > 0) {
      const img = new Image();
      img.crossOrigin = "Anonymous";
      img.src = feed.images[currentImageIndex] || feed.images[0];

      img.onload = () => {
        const canvas = document.createElement('canvas');
        const ctx = canvas.getContext('2d');
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
          const brightness = (0.299 * r + 0.587 * g + 0.114 * b);
          totalBrightness += brightness;
          totalAlpha += a;
        }

        const avgBrightness = totalBrightness / (data.length / 4);
        const avgAlpha = totalAlpha / (data.length / 4);

        // 투명한 배경이면 검정색, 아니면 평균 밝기에 따라 결정
        if (avgAlpha < 200) {
          setIconColor('black');
        } else {
          // 평균 밝기가 128보다 크면 어두운 아이콘, 작으면 밝은 아이콘
          setIconColor(avgBrightness > 128 ? 'black' : 'white');
        }
      };
    } else {
      // 이미지 없을 때는 밝은 배경이므로 검은색
      setIconColor('black');
    }
  }, [feed?.images, currentImageIndex]);

  // BlockNote 에디터 생성 (읽기 전용)
  const initialContent = useMemo(() => {
    if (!feed?.content) return undefined;
    try {
      // JSON인지 확인
      const parsed = JSON.parse(feed.content);
      return parsed;
    } catch {
      // 일반 텍스트인 경우 BlockNote 기본 형식으로 변환
      return [
        {
          type: "paragraph",
          content: feed.content,
        },
      ];
    }
  }, [feed?.content]);

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
      } catch (e) {
        // 일반 텍스트인 경우 BlockNote 기본 형식으로 변환
        const textBlocks = [
          {
            type: "paragraph",
            content: feed.content,
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

        // 답글 정보도 함께 로드
        const commentsWithReplies = await Promise.all(
          commentList.map(async (comment: any) => {
            try {
              const replies = await commentApi.getRepliesByCommentId(comment.id);
              return { ...comment, replies: replies || [] };
            } catch (error) {
              return { ...comment, replies: [] };
            }
          })
        );

        setComments(commentsWithReplies);
      } catch (error) {
        console.error("댓글 데이터 로드 실패:", error);
        setComments([]);
      }

      setShowComments(false); // 피드 변경 시 댓글창 닫기
    };

    fetchComments();
  }, [feed]);

  const handleSendComment = () => {
    if (commentText.trim() && feed) {
      // 일반 댓글 추가
      const newComment: Comment = {
        id: Date.now(),
        author: "김루핀",
        avatar: "김",
        content: commentText,
        time: "방금 전",
        replies: [],
      };
      setComments([...comments, newComment]);
      setCommentText("");
    }
  };

  const handleSendReply = () => {
    if (replyCommentText.trim() && feed && replyingTo !== null) {
      // 루트 댓글에만 답글 추가
      setComments(
        comments.map((comment) => {
          if (comment.id === replyingTo) {
            const newReply: Comment = {
              id: Date.now(),
              author: "김루핀",
              avatar: "김",
              content: replyCommentText,
              time: "방금 전",
              parentId: replyingTo,
              replies: [],
            };
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

  // 댓글 렌더링 함수 (재귀적으로 대댓글 표시)
  const renderComment = (comment: Comment, depth: number = 0) => {
    const isReply = depth > 0;
    const hasReplies = comment.replies && comment.replies.length > 0;
    const isCollapsed = collapsedComments.has(comment.id);
    const isReplying = replyingTo === comment.id;
    const likeInfo = commentLikes[comment.id] || { liked: false, count: 0 };

    return (
      <div key={comment.id} className={isReply ? "ml-8 mt-3" : ""}>
        <div className="flex gap-3">
          <Avatar className="w-8 h-8 flex-shrink-0">
            <AvatarFallback className="bg-white">
              <User className="w-4 h-4 text-gray-400" />
            </AvatarFallback>
          </Avatar>
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

            {/* 좋아요 + 답글 버튼 */}
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
              {/* 답글 버튼은 루트 댓글에만 표시 */}
              {depth === 0 && (
                <button
                  onClick={() => setReplyingTo(isReplying ? null : comment.id)}
                  className="text-xs text-gray-600 hover:text-[#C93831] font-semibold"
                >
                  답글
                </button>
              )}
            </div>

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

            {/* 접기/펼치기 버튼 */}
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
        className={`p-0 h-[95vh] max-h-[95vh] overflow-hidden backdrop-blur-2xl bg-white/60 border border-gray-200/30 shadow-2xl transition-all duration-300 ${
          showComments
            ? "!w-[825px] !max-w-[825px]"
            : "!w-[475px] !max-w-[475px]"
        }`}
        style={{
          width: showComments ? "825px" : "475px",
          maxWidth: showComments ? "825px" : "475px",
        }}
      >
        <DialogHeader className="sr-only">
          <DialogTitle>피드 상세보기</DialogTitle>
          <DialogDescription>
            피드의 상세 내용을 확인할 수 있습니다.
          </DialogDescription>
        </DialogHeader>
        <div className="relative h-full flex overflow-hidden">
          {/* Main Feed Content (Left) */}
          <div className="w-[475px] max-w-[475px] flex-shrink-0 flex flex-col overflow-hidden">
            {feed.images && feed.images.length > 0 ? (
              <>
                {/* Image Carousel */}
                <div className="relative h-[545px] w-full max-w-[475px] overflow-hidden flex-shrink-0">
                  <img
                    src={feed.images[currentImageIndex] || feed.images[0]}
                    alt={feed.activity}
                    className="w-full h-full object-cover"
                    style={{ maxWidth: "475px", width: "475px", height: "545px" }}
                  />

                  {feed.images.length > 1 && (
                    <>
                      {currentImageIndex > 0 && (
                        <button
                          onClick={onPrevImage}
                          className="absolute left-2 top-1/2 -translate-y-1/2 hover:opacity-70 transition-opacity"
                        >
                          <ChevronLeft className={`w-8 h-8 ${iconColor === 'white' ? 'text-white' : 'text-black'}`} />
                        </button>
                      )}
                      {currentImageIndex < feed.images.length - 1 && (
                        <button
                          onClick={onNextImage}
                          className="absolute right-2 top-1/2 -translate-y-1/2 hover:opacity-70 transition-opacity"
                        >
                          <ChevronRight className={`w-8 h-8 ${iconColor === 'white' ? 'text-white' : 'text-black'}`} />
                        </button>
                      )}
                      <div className="absolute bottom-2 left-1/2 -translate-x-1/2 flex gap-1">
                        {feed.images.map((_, idx) => (
                          <div
                            key={idx}
                            className={`w-1.5 h-1.5 rounded-full ${
                              idx === currentImageIndex ? "bg-white" : "bg-white/50"
                            }`}
                          ></div>
                        ))}
                      </div>
                    </>
                  )}

                  {/* Author Avatar Only */}
                  <div className="absolute top-4 left-4">
                    <Avatar className="w-10 h-10 border-2 border-white shadow-lg">
                      <AvatarFallback className="bg-white">
                        <User className="w-5 h-5 text-gray-400" />
                      </AvatarFallback>
                    </Avatar>
                  </div>

                  {/* Menu Button */}
                  <div className="absolute top-4 right-4">
                    <DropdownMenu>
                      <DropdownMenuTrigger asChild>
                        <button className="hover:opacity-70 transition-opacity">
                          <MoreVertical className={`w-6 h-6 ${iconColor === 'white' ? 'text-white' : 'text-black'}`} />
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
                    <div className="flex flex-col items-center gap-1">
                      <div className="w-12 h-12 rounded-full flex items-center justify-center">
                        <Heart
                          className={`w-6 h-6 fill-red-500 text-red-500`}
                        />
                      </div>
                      <span
                        className={`text-xs font-bold ${iconColor === 'white' ? 'text-white' : 'text-black'}`}
                      >
                        {feed.likes}
                      </span>
                    </div>

                    <button
                      className="flex flex-col items-center gap-1 group"
                      onClick={() => setShowComments(!showComments)}
                    >
                      <div className="w-12 h-12 rounded-full flex items-center justify-center hover:scale-110 transition-transform">
                        <MessageCircle
                          className={`w-6 h-6 ${iconColor === 'white' ? 'text-white' : 'text-black'}`}
                        />
                      </div>
                      <span
                        className={`text-xs font-bold ${iconColor === 'white' ? 'text-white' : 'text-black'}`}
                      >
                        {totalCommentCount}
                      </span>
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
                      <AvatarFallback className="bg-white">
                        <User className="w-5 h-5 text-gray-400" />
                      </AvatarFallback>
                    </Avatar>

                    {/* Menu Button */}
                    <DropdownMenu>
                      <DropdownMenuTrigger asChild>
                        <button className="hover:opacity-70 transition-opacity">
                          <MoreVertical className={`w-6 h-6 ${iconColor === 'white' ? 'text-white' : 'text-black'}`} />
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
                    <div className="flex flex-col items-center gap-1">
                      <div className="w-12 h-12 rounded-full flex items-center justify-center">
                        <Heart
                          className={`w-6 h-6 fill-red-500 text-red-500`}
                        />
                      </div>
                      <span
                        className={`text-xs font-bold ${iconColor === 'white' ? 'text-white' : 'text-black'}`}
                      >
                        {feed.likes}
                      </span>
                    </div>

                    <button
                      className="flex flex-col items-center gap-1 group"
                      onClick={() => setShowComments(!showComments)}
                    >
                      <div className="w-12 h-12 rounded-full flex items-center justify-center hover:scale-110 transition-transform">
                        <MessageCircle
                          className={`w-6 h-6 ${iconColor === 'white' ? 'text-white' : 'text-black'}`}
                        />
                      </div>
                      <span
                        className={`text-xs font-bold ${iconColor === 'white' ? 'text-white' : 'text-black'}`}
                      >
                        {totalCommentCount}
                      </span>
                    </button>
                  </div>
                </div>
              </>
            )}

            {/* Feed Content (Always visible) */}
            <ScrollArea
              className="bg-transparent"
              style={{
                width: "475px",
                maxWidth: "475px",
                height: "calc(95vh - 545px)",
                maxHeight: "calc(95vh - 545px)"
              }}
            >
              <div className="p-6 space-y-3">
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
                      {feed.stats.calories && (
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
            <div className="flex-1 bg-transparent border-l border-gray-200/30 flex flex-col">
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
              <ScrollArea className="flex-1 px-6 pt-4">
                <div className="space-y-4 pb-4">
                  {comments.length === 0 ? (
                    <div className="text-center text-gray-500 text-sm py-8">
                      첫 댓글을 남겨보세요!
                    </div>
                  ) : (
                    sortedComments.map((comment) => renderComment(comment))
                  )}
                </div>
              </ScrollArea>

              {/* Comment Input - 일반 댓글 작성용 (항상 표시) */}
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
      </DialogContent>
    </Dialog>
  );
}
