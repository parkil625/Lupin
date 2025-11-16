/**
 * FeedDetailDialogHome.tsx
 *
 * 피드 상세보기 다이얼로그 컴포넌트
 * - 홈 화면에서 피드 클릭 시 표시
 * - 이미지 캐러셀 기능
 * - 피드 상세 정보 표시
 */
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from "@/components/ui/dialog";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Badge } from "@/components/ui/badge";
import { Heart, MessageCircle, Sparkles, ChevronLeft, ChevronRight } from "lucide-react";
import { Feed } from "@/types/dashboard.types";
import { toast } from "sonner";

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
                onClick={() => {
                  toast.info("댓글을 보려면 피드 메뉴에서 확인하세요");
                }}
              >
                <div className="w-12 h-12 rounded-full backdrop-blur-xl bg-white/20 border border-white/30 flex items-center justify-center hover:scale-110 transition-transform">
                  <MessageCircle className="w-5 h-5 text-white" />
                </div>
                <span className="text-white text-xs font-bold">{feed.comments}</span>
              </button>
            </div>
          </div>

          {/* Content */}
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
        </div>
      </DialogContent>
    </Dialog>
  );
}
