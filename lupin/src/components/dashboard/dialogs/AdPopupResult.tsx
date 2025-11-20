// AdPopResult.tsx
import { DialogClose } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { CheckCircle2, XCircle } from "lucide-react";

interface AdPopResultProps {
  status: "success" | "fail";
  failReason?: string; // 선택: 실패 이유 메시지
}

export default function AdPopResult({ status, failReason }: AdPopResultProps) {
  const isSuccess = status === "success";

  return (
    <>
      {/* 상단 이미지 + 오버레이 영역 */}
      <div className="relative">
        <img
          src={
            isSuccess
              ? "https://images.unsplash.com/photo-1517832207067-4db24a2ae47c?w=600&q=75&fit=crop"
              : "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=600&q=75&fit=crop"
          }
          alt={isSuccess ? "웰빙 챌린지 응모 완료" : "웰빙 챌린지 응모 실패"}
          className="w-full h-auto object-cover"
        />
        <div className="absolute inset-0 bg-gradient-to-t from-black/80 via-black/40 to-transparent flex flex-col justify-end p-8">
          <div className="text-white space-y-3">
            <h2 className="text-3xl font-black drop-shadow-[0_2px_4px_rgba(0,0,0,0.8)] flex items-center gap-2">
              {isSuccess ? (
                <>
                  <CheckCircle2 className="w-8 h-8 text-emerald-300" />
                  응모가 완료되었어요!
                </>
              ) : (
                <>
                  <XCircle className="w-8 h-8 text-red-300" />
                  아쉽지만 선착순이 마감되었어요 😭
                </>
              )}
            </h2>

            {isSuccess ? (
              <p className="text-lg font-medium drop-shadow-[0_2px_3px_rgba(0,0,0,0.8)]">
                응모 성공!.
                <br />
                상품은 알림을 확인해주세요.
              </p>
            ) : (
              <p className="text-lg font-medium drop-shadow-[0_2px_3px_rgba(0,0,0,0.8)]">
                {failReason ??
                  "선착순 인원이 모두 찼기 때문에 응모가 불가합니다."}
                <br />
                다음 이벤트를 기대해 주세요.
              </p>
            )}
          </div>
        </div>
      </div>

      {/* 하단 버튼 영역 */}
      <div className="p-4 bg-white border-t flex items-center justify-end">
        <DialogClose asChild>
          <Button className="font-bold">닫기</Button>
        </DialogClose>
      </div>
    </>
  );
}
