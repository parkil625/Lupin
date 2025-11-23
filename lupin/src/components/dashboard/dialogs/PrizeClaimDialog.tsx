import { useState } from "react";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Trophy, Sparkles } from "lucide-react";

interface PrizeClaimDialogProps {
  open: boolean;
  onClose: () => void;
  prizeAmount: string; // "100만원" or "50만원"
}

export default function PrizeClaimDialog({
  open,
  onClose,
  prizeAmount,
}: PrizeClaimDialogProps) {
  const [bankName, setBankName] = useState("");
  const [accountNumber, setAccountNumber] = useState("");
  const [accountHolder, setAccountHolder] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isSubmitted, setIsSubmitted] = useState(false);

  const handleSubmit = async () => {
    if (!bankName || !accountNumber || !accountHolder) {
      alert("모든 정보를 입력해주세요.");
      return;
    }

    setIsSubmitting(true);

    try {
      // API 호출 (실제 구현 시)
      // await lotteryApi.claimPrize(ticketId, bankName, accountNumber, accountHolder);

      // 테스트용 딜레이
      await new Promise((resolve) => setTimeout(resolve, 1000));

      setIsSubmitted(true);
    } catch (error) {
      console.error("상금 수령 신청 실패:", error);
      alert("상금 수령 신청에 실패했습니다.");
    } finally {
      setIsSubmitting(false);
    }
  };

  if (isSubmitted) {
    return (
      <Dialog open={open} onOpenChange={onClose}>
        <DialogContent className="sm:max-w-md">
          <div className="text-center py-8">
            <div className="w-20 h-20 bg-gradient-to-br from-green-400 to-emerald-500 rounded-full flex items-center justify-center mx-auto mb-6">
              <Sparkles className="w-10 h-10 text-white" />
            </div>
            <h3 className="text-2xl font-black text-gray-900 mb-3">
              신청 완료!
            </h3>
            <p className="text-gray-600 mb-6">
              상금 수령 정보가 정상적으로 등록되었습니다.
              <br />
              영업일 기준 3-5일 내에 입금됩니다.
            </p>
            <Button
              onClick={onClose}
              className="bg-gradient-to-r from-[#C93831] to-[#B02F28] text-white"
            >
              확인
            </Button>
          </div>
        </DialogContent>
      </Dialog>
    );
  }

  return (
    <Dialog open={open} onOpenChange={onClose}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <div className="text-center mb-4">
            <div className="w-20 h-20 bg-gradient-to-br from-yellow-400 to-orange-500 rounded-full flex items-center justify-center mx-auto mb-4 animate-bounce">
              <Trophy className="w-10 h-10 text-white" />
            </div>
            <DialogTitle className="text-2xl font-black text-gray-900">
              축하합니다!
            </DialogTitle>
            <DialogDescription className="text-lg font-bold text-[#C93831] mt-2">
              {prizeAmount} 당첨!
            </DialogDescription>
          </div>
        </DialogHeader>

        <div className="space-y-4 py-4">
          <p className="text-sm text-gray-600 text-center mb-4">
            상금 수령을 위해 계좌 정보를 입력해주세요.
          </p>

          <div className="space-y-2">
            <Label htmlFor="bankName">은행명</Label>
            <Input
              id="bankName"
              placeholder="예: 국민은행"
              value={bankName}
              onChange={(e) => setBankName(e.target.value)}
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="accountNumber">계좌번호</Label>
            <Input
              id="accountNumber"
              placeholder="- 없이 숫자만 입력"
              value={accountNumber}
              onChange={(e) =>
                setAccountNumber(e.target.value.replace(/[^0-9]/g, ""))
              }
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="accountHolder">예금주</Label>
            <Input
              id="accountHolder"
              placeholder="예금주명"
              value={accountHolder}
              onChange={(e) => setAccountHolder(e.target.value)}
            />
          </div>
        </div>

        <div className="flex gap-3">
          <Button variant="outline" onClick={onClose} className="flex-1">
            나중에
          </Button>
          <Button
            onClick={handleSubmit}
            disabled={isSubmitting}
            className="flex-1 bg-gradient-to-r from-[#C93831] to-[#B02F28] text-white"
          >
            {isSubmitting ? "처리중..." : "수령 신청"}
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}
