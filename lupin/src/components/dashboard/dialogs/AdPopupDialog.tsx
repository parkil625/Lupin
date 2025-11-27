import { Dialog, DialogContent } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Trophy, X } from "lucide-react";

interface AdPopupDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onHideForToday: () => void;
  isWinner?: boolean;
  prizeInfo?: {
    prizeName: string;
    drawDate: string;
  };
}

export default function AdPopupDialog({
  open,
  onOpenChange,
  onHideForToday,
  isWinner = false,
  prizeInfo,
}: AdPopupDialogProps) {
  if (isWinner && prizeInfo) {
    return (
      <Dialog open={open} onOpenChange={onOpenChange}>
        <DialogContent className="sm:max-w-md p-0 overflow-hidden">
          <div className="bg-gradient-to-br from-yellow-400 via-orange-500 to-red-500 p-8 text-center text-white">
            <Trophy className="w-20 h-20 mx-auto mb-4 animate-bounce" />
            <h2 className="text-3xl font-black mb-2">축하합니다!</h2>
            <p className="text-xl font-bold mb-4">당첨되셨습니다!</p>
            <div className="bg-white/20 rounded-2xl p-4 mb-4">
              <p className="text-2xl font-black">{prizeInfo.prizeName}</p>
              <p className="text-sm opacity-80">{prizeInfo.drawDate} 추첨</p>
            </div>
            <Button
              onClick={() => onOpenChange(false)}
              className="bg-white text-orange-500 hover:bg-gray-100 font-bold"
            >
              확인
            </Button>
          </div>
        </DialogContent>
      </Dialog>
    );
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md p-0 overflow-hidden">
        <div className="relative">
          <button
            onClick={() => onOpenChange(false)}
            className="absolute top-2 right-2 z-10 p-1 rounded-full bg-black/50 text-white hover:bg-black/70"
          >
            <X className="w-5 h-5" />
          </button>
          <div className="bg-gradient-to-br from-purple-500 to-pink-500 p-8 text-center text-white">
            <h2 className="text-2xl font-black mb-4">오늘의 이벤트</h2>
            <p className="mb-6">운동을 기록하고 추첨권을 받으세요!</p>
            <img
              src="https://images.unsplash.com/photo-1571019614242-c5c5dee9f50b?w=300&q=75"
              alt="운동"
              className="rounded-xl mx-auto mb-4"
            />
          </div>
          <div className="p-4 bg-white flex justify-between items-center">
            <button
              onClick={onHideForToday}
              className="text-sm text-gray-500 hover:text-gray-700"
            >
              오늘 하루 보지 않기
            </button>
            <Button onClick={() => onOpenChange(false)}>닫기</Button>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
}
