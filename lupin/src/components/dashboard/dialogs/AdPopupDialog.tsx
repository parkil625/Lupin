import { useState } from "react";
import { Dialog, DialogContent, DialogTitle, DialogDescription } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import AdPopResult from "./AdPopupResult";

async function applyEvent(): Promise<{ success: boolean; reason?: string }> {
  // TODO: 실제 API 호출로 교체
  return { success: true };
}

interface AdPopupDialogProps {
  open: boolean;
  onClose: () => void;
  onDontShowFor24Hours: () => void;
  onJoinChallenge?: () => void; // 성공 후 부모에게 알려줄 용도(선택)
}

export default function AdPopupDialog({
  open,
  onClose,
  onDontShowFor24Hours,
  onJoinChallenge,
}: AdPopupDialogProps) {
  const [resultStatus, setResultStatus] = useState<"idle" | "success" | "fail">(
    "idle"
  );
  const [failReason, setFailReason] = useState<string | undefined>(undefined);
  const showResult = resultStatus !== "idle";

  // "지금 바로 응모하기" 클릭 시 : 팝업 유지 + 내부 화면만 결과로 전환
  const handleJoinClick = async () => {
    try {
      const res = await applyEvent();

      if (res.success) {
        setResultStatus("success");
      } else {
        setResultStatus("fail");
        setFailReason(res.reason ?? "선착순 인원이 마감되었습니다.");
      }
    } catch (e) {
      setResultStatus("fail");
      setFailReason("잠시 후 다시 시도해 주세요.");
    }
  };

  // Dialog 열림/닫힘 상태 변경 시
  const handleOpenChange = (isOpen: boolean) => {
    if (!isOpen) {
      // ✅ 닫히는 시점에, 성공 상태였다면 부모에 알림
      if (resultStatus === "success" && onJoinChallenge) {
        onJoinChallenge();
      }

      // 상태 초기화
      setResultStatus("idle");
      setFailReason(undefined);

      onClose();
    }
  };

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent className="max-w-2xl p-0 gap-0 bg-transparent border-0 shadow-none">
        <DialogTitle className="sr-only">웰빙 챌린지 이벤트</DialogTitle>
        <DialogDescription className="sr-only">
          {showResult
            ? resultStatus === "success"
              ? "응모가 완료되었습니다."
              : "응모에 실패했습니다."
            : "웰빙 챌린지 이벤트에 지금 참여하고 푸짐한 상품을 받아가세요!"}
        </DialogDescription>
        <div className="relative">
          <div className="relative bg-white rounded-2xl overflow-hidden shadow-2xl">
            {showResult ? (
              <AdPopResult
                status={resultStatus === "success" ? "success" : "fail"}
                failReason={failReason}
              />
            ) : (
              <>
                <div className="relative">
                  <img
                    src="https://images.unsplash.com/photo-1517836357463-d25dfeac3438?w=1200&h=800&fit=crop"
                    alt="웰빙 챌린지 광고"
                    className="w-full h-auto object-cover"
                  />

                  <div className="absolute inset-0 bg-gradient-to-t from-black/80 via-black/40 to-transparent flex flex-col justify-end p-8">
                    <div className="text-white space-y-4">
                      <h2 className="text-4xl font-black drop-shadow-[0_2px_4px_rgba(0,0,0,0.8)]">
                        웰빙 챌린지 이벤트
                      </h2>
                      <p className="text-xl font-medium drop-shadow-[0_2px_3px_rgba(0,0,0,0.8)]">
                        지금 참여하고 푸짐한 상품을 받아가세요!
                        <br />
                        매주 추첨으로 럭키박스 증정!
                      </p>
                      <div className="flex items-center gap-3 pt-2">
                        <div className="px-4 py-2 bg-white/20 backdrop-blur-sm rounded-full font-medium drop-shadow-[0_1px_2px_rgba(0,0,0,0.8)]">
                          매주 월요일 오전 9시
                        </div>
                      </div>
                      <div className="pt-2">
                        <Button
                          onClick={handleJoinClick}
                          className="bg-gradient-to-r from-[#C93831] to-[#B02F28] hover:from-[#B02F28] hover:to-[#C93831] text-white font-bold text-lg px-8 py-6 rounded-xl"
                        >
                          지금 바로 응모하기
                        </Button>
                      </div>
                    </div>
                  </div>
                </div>

                <div className="p-4 bg-white border-t flex items-center justify-between">
                  <button
                    onClick={onDontShowFor24Hours}
                    className="text-sm text-gray-600 hover:text-gray-900 font-medium underline"
                  >
                    24시간 동안 보지 않기
                  </button>
                  <Button
                    onClick={onClose}
                    variant="outline"
                    className="font-bold"
                  >
                    닫기
                  </Button>
                </div>
              </>
            )}
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
}
