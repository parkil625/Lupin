/**
 * AdPopupDialog.tsx
 *
 * 광고 팝업 다이얼로그 컴포넌트
 * - 홈 화면 진입 시 표시되는 광고 팝업
 * - X 버튼으로 닫기
 * - 24시간 보지 않기 기능
 */

import { Dialog, DialogContent } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";

interface AdPopupDialogProps {
  open: boolean;
  onClose: () => void;
  onDontShowFor24Hours: () => void;
  onJoinChallenge: () => void;
}

export default function AdPopupDialog({
  open,
  onClose,
  onDontShowFor24Hours,
  onJoinChallenge,
}: AdPopupDialogProps) {
  return (
    <Dialog open={open} onOpenChange={onClose}>
      <DialogContent className="max-w-2xl p-0 gap-0 bg-transparent border-0 shadow-none">
        <div className="relative">
          {/* 광고 이미지 영역 */}
          <div className="relative bg-white rounded-2xl overflow-hidden shadow-2xl">
            <div className="relative">
              <img
                src="https://images.unsplash.com/photo-1517836357463-d25dfeac3438?w=1200&h=800&fit=crop"
                alt="웰빙 챌린지 광고"
                className="w-full h-auto object-cover"
              />

              {/* 광고 내용 오버레이 */}
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
                      onClick={onJoinChallenge}
                      className="bg-gradient-to-r from-[#C93831] to-[#B02F28] hover:from-[#B02F28] hover:to-[#C93831] text-white font-bold text-lg px-8 py-6 rounded-xl"
                    >
                      지금 바로 응모하기
                    </Button>
                  </div>
                </div>
              </div>
            </div>

            {/* 하단 버튼 영역 */}
            <div className="p-4 bg-white border-t flex items-center justify-between">
              <button
                onClick={onDontShowFor24Hours}
                className="text-sm text-gray-600 hover:text-gray-900 font-medium underline"
              >
                24시간 동안 보지 않기
              </button>
              <Button onClick={onClose} variant="outline" className="font-bold">
                닫기
              </Button>
            </div>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
}
