/**
 * FeedDetailDialogHome.tsx
 *
 * 피드 상세보기 다이얼로그 컴포넌트
 * - 홈 화면에서 피드 클릭 시 표시
 * - 이미지 캐러셀 기능
 * - 피드 상세 정보 표시
 * - 댓글 표시 및 작성 기능
 */
import { useState, useEffect } from "react";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from "@/components/ui/dialog";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Badge } from "@/components/ui/badge";
import { Heart, MessageCircle, Sparkles, ChevronLeft, ChevronRight, Send } from "lucide-react";
import { Feed, Comment } from "@/types/dashboard.types";
import { initialComments } from "@/mockdata/comments";

interface FeedDetailDialogHomeProps {
  feed: Feed | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  currentImageIndex: number;
  onPrevImage: () => void;
  onNextImage: () => void;
}

export default function FeedDetailDialogHome({
  feed,
  open,
  onOpenChange,
  currentImageIndex,
  onPrevImage,
  onNextImage
}: FeedDetailDialogHomeProps) {
  const [showComments, setShowComments] = useState(false);
  const [commentText, setCommentText] = useState("");
  const [comments, setComments] = useState<Comment[]>([]);

  // Feed가 변경되면 해당 피드의 댓글 로드
  useEffect(() => {
    if (feed) {
      setComments(initialComments[feed.id] || []);
      setShowComments(false); // 피드 변경 시 댓글창 닫기
    }
  }, [feed]);

  const handleSendComment = () => {
    if (commentText.trim() && feed) {
      const newComment: Comment = {
        id: Date.now(),
        author: "김루핀",
        avatar: "김",
        content: commentText,
        time: "방금 전",
        replies: []
      };
      setComments([...comments, newComment]);
      setCommentText("");
    }
  };

  if (!feed) return null;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-md p-0 max-h-[90vh]">
        <DialogHeader className="sr-only">
          <DialogTitle>피드 상세보기</DialogTitle>
          <DialogDescription>피드의 상세 내용을 확인할 수 있습니다.</DialogDescription>
        </DialogHeader>
        <div style={{ height: '85vh' }} className="relative">
          {/* Image Carousel */}
          <div className="relative h-3/4">
            <img
              src={feed.images[currentImageIndex] || feed.images[0]}
              alt={feed.activity}
              className="w-full h-full object-cover"
            />

            {feed.images.length > 1 && (
              <>
                <button
                  onClick={onPrevImage}
                  className="absolute left-2 top-1/2 -translate-y-1/2 w-8 h-8 rounded-full bg-black/50 text-white flex items-center justify-center hover:bg-black/70"
                >
                  <ChevronLeft className="w-5 h-5" />
                </button>
                <button
                  onClick={onNextImage}
                  className="absolute right-2 top-1/2 -translate-y-1/2 w-8 h-8 rounded-full bg-black/50 text-white flex items-center justify-center hover:bg-black/70"
                >
                  <ChevronRight className="w-5 h-5" />
                </button>
                <div className="absolute bottom-2 left-1/2 -translate-x-1/2 flex gap-1">
                  {feed.images.map((_, idx) => (
                    <div key={idx} className={`w-1.5 h-1.5 rounded-full ${idx === currentImageIndex ? 'bg-white' : 'bg-white/50'}`}></div>
                  ))}
                </div>
              </>
            )}

            {/* Author Info */}
            <div className="absolute top-4 left-4 flex items-center gap-3 backdrop-blur-xl bg-white/20 rounded-full px-4 py-2 border border-white/30">
              <Avatar className="w-8 h-8 border-2 border-white">
                <AvatarFallback className="bg-gradient-to-br from-[#C93831] to-[#B02F28] text-white font-black text-sm">
                  {feed.avatar}
                </AvatarFallback>
              </Avatar>
              <div>
                <div className="text-white text-xs font-bold">{feed.author}</div>
                <div className="text-white/80 text-xs">{feed.time}</div>
              </div>
            </div>

            {/* Right Actions */}
            <div className="absolute right-4 bottom-4 flex flex-col gap-4">
              <button className="flex flex-col items-center gap-1 group">
                <div className="w-12 h-12 rounded-full backdrop-blur-xl bg-white/20 border border-white/30 flex items-center justify-center hover:scale-110 transition-transform">
                  <Heart className="w-5 h-5 text-white" />
                </div>
                <span className="text-white text-xs font-bold">{feed.likes}</span>
              </button>

              <button
                className="flex flex-col items-center gap-1 group"
                onClick={() => setShowComments(!showComments)}
              >
                <div className={`w-12 h-12 rounded-full backdrop-blur-xl border border-white/30 flex items-center justify-center hover:scale-110 transition-transform ${showComments ? 'bg-white/40' : 'bg-white/20'}`}>
                  <MessageCircle className="w-5 h-5 text-white" />
                </div>
                <span className="text-white text-xs font-bold">{comments.length}</span>
              </button>
            </div>
          </div>

          {/* Content or Comments */}
          {!showComments ? (
            <div className="p-6 space-y-3 h-1/4 overflow-auto bg-white">
              <Badge className="bg-gradient-to-r from-yellow-400 to-orange-500 text-white px-3 py-1 font-bold border-0">
                <Sparkles className="w-3 h-3 mr-1" />
                +{feed.points}
              </Badge>

              <p className="text-gray-700 font-medium text-sm leading-relaxed">
                {feed.content}
              </p>

              <div className="flex gap-2 flex-wrap">
                <Badge className="bg-white border border-gray-300 text-gray-700 px-3 py-1 font-bold text-xs">
                  {feed.duration}
                </Badge>
                {Object.entries(feed.stats).map(([key, value]) => (
                  <Badge key={key} className="bg-red-50 border border-red-200 text-[#C93831] px-3 py-1 font-bold text-xs">
                    {value}
                  </Badge>
                ))}
              </div>
            </div>
          ) : (
            <div className="h-1/4 bg-white flex flex-col">
              {/* Comments List */}
              <ScrollArea className="flex-1 px-6 pt-4">
                <div className="space-y-3 pb-4">
                  {comments.length === 0 ? (
                    <div className="text-center text-gray-500 text-sm py-8">
                      첫 댓글을 남겨보세요!
                    </div>
                  ) : (
                    comments.map((comment) => (
                      <div key={comment.id} className="flex gap-3">
                        <Avatar className="w-8 h-8 flex-shrink-0">
                          <AvatarFallback className="bg-gradient-to-br from-[#C93831] to-[#B02F28] text-white font-black text-xs">
                            {comment.avatar}
                          </AvatarFallback>
                        </Avatar>
                        <div className="flex-1 min-w-0">
                          <div className="flex items-center gap-2 mb-1">
                            <span className="font-bold text-sm text-gray-900">{comment.author}</span>
                            <span className="text-xs text-gray-500">{comment.time}</span>
                          </div>
                          <p className="text-sm text-gray-700 break-words">{comment.content}</p>
                        </div>
                      </div>
                    ))
                  )}
                </div>
              </ScrollArea>

              {/* Comment Input */}
              <div className="p-4 border-t flex gap-2">
                <input
                  type="text"
                  placeholder="댓글을 입력하세요..."
                  value={commentText}
                  onChange={(e) => setCommentText(e.target.value)}
                  onKeyPress={(e) => e.key === 'Enter' && handleSendComment()}
                  className="flex-1 px-4 py-2 border border-gray-300 rounded-full text-sm focus:outline-none focus:ring-2 focus:ring-[#C93831] focus:border-transparent"
                />
                <button
                  onClick={handleSendComment}
                  disabled={!commentText.trim()}
                  className="w-10 h-10 rounded-full bg-gradient-to-r from-[#C93831] to-[#B02F28] text-white flex items-center justify-center hover:shadow-lg transition-shadow disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  <Send className="w-4 h-4" />
                </button>
              </div>
            </div>
          )}
        </div>
      </DialogContent>
    </Dialog>
  );
}
