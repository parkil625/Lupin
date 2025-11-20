import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Frown } from "lucide-react";

interface LotteryResultDialogProps {
  open: boolean;
  onClose: () => void;
}

export default function LotteryResultDialog({
  open,
  onClose,
}: LotteryResultDialogProps) {
  return (
    <Dialog open={open} onOpenChange={onClose}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <div className="text-center mb-4">
            <div className="w-20 h-20 bg-gradient-to-br from-gray-400 to-gray-500 rounded-full flex items-center justify-center mx-auto mb-4">
              <Frown className="w-10 h-10 text-white" />
            </div>
            <DialogTitle className="text-2xl font-black text-gray-900">
              아쉽네요...
            </DialogTitle>
            <DialogDescription className="text-lg font-medium text-gray-600 mt-2">
              이번 추첨에서 당첨되지 않았습니다
            </DialogDescription>
          </div>
        </DialogHeader>

        <div className="text-center py-4">
          <p className="text-gray-600 mb-6">
            다음 추첨에서 행운을 빕니다!
            <br />
            운동을 계속하면 추첨권을 더 모을 수 있어요.
          </p>
          <Button
            onClick={onClose}
            className="bg-gradient-to-r from-[#C93831] to-[#B02F28] text-white px-8"
          >
            확인
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}
