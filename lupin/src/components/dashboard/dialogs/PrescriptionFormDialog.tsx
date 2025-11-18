/**
 * PrescriptionFormDialog.tsx
 *
 * 처방전 작성 다이얼로그 컴포넌트 (의사용)
 * - 약물 처방 정보 입력
 * - 복용 방법 및 기간 설정
 * - 처방전 저장 기능
 */
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from "@/components/ui/dialog";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Button } from "@/components/ui/button";
import { CheckCircle, PlusSquare, User } from "lucide-react";
import { Member } from "@/types/dashboard.types";

interface PrescriptionFormDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  member: Member | null;
  onSubmit: () => void;
}

export default function PrescriptionFormDialog({
  open,
  onOpenChange,
  member,
  onSubmit
}: PrescriptionFormDialogProps) {
  return (
    <Dialog open={open && !!member} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-3xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle className="text-2xl font-black">처방전 작성</DialogTitle>
          <DialogDescription>회원의 진단 및 처방 정보를 입력하세요.</DialogDescription>
        </DialogHeader>

        {member && (
          <div className="space-y-6">
            {/* Member Info */}
            <div className="p-4 rounded-2xl bg-gradient-to-br from-blue-50 to-cyan-50 border border-blue-200">
              <div className="flex items-center gap-4">
                <Avatar className="w-16 h-16">
                  <AvatarFallback className="bg-white">
                    <User className="w-8 h-8 text-gray-400" />
                  </AvatarFallback>
                </Avatar>
                <div>
                  <div className="font-black text-xl text-gray-900">{member.name}</div>
                  <div className="text-sm text-gray-600">
                    {member.age}세 · {member.gender}
                  </div>
                  <div className="text-sm text-gray-600">
                    마지막 방문: {member.lastVisit}
                  </div>
                </div>
              </div>
            </div>

            {/* Diagnosis */}
            <div>
              <Label className="text-base font-black mb-2 block">진단명</Label>
              <Input
                placeholder="진단명을 입력하세요 (예: 급성 상기도 감염)"
                className="rounded-xl"
              />
            </div>

            {/* Symptoms */}
            <div>
              <Label className="text-base font-black mb-2 block">증상</Label>
              <Textarea
                placeholder="회원의 주요 증상을 입력하세요"
                className="rounded-xl min-h-[100px]"
              />
            </div>

            {/* Medicines */}
            <div>
              <Label className="text-base font-black mb-2 block">처방 의약품</Label>
              <div className="space-y-3">
                <div className="flex gap-2">
                  <Input placeholder="약품명" className="rounded-xl flex-1" />
                  <Input placeholder="용량" className="rounded-xl w-32" />
                  <Input placeholder="횟수/일" className="rounded-xl w-32" />
                  <Input placeholder="일수" className="rounded-xl w-24" />
                </div>
                <Button
                  variant="outline"
                  className="w-full rounded-xl border-2 border-dashed border-gray-300 hover:border-[#C93831] hover:bg-red-50"
                >
                  <PlusSquare className="w-4 h-4 mr-2" />
                  약품 추가
                </Button>
              </div>
            </div>

            {/* Instructions */}
            <div>
              <Label className="text-base font-black mb-2 block">복용 방법 및 주의사항</Label>
              <Textarea
                placeholder="복용 방법, 주의사항, 부작용 등을 입력하세요"
                className="rounded-xl min-h-[120px]"
              />
            </div>

            {/* Next Appointment */}
            <div>
              <Label className="text-base font-black mb-2 block">다음 진료 예정일</Label>
              <Input
                type="date"
                className="rounded-xl"
              />
            </div>

            {/* Action Buttons */}
            <div className="flex gap-3 pt-4">
              <Button
                variant="outline"
                onClick={() => onOpenChange(false)}
                className="flex-1 rounded-2xl h-12 font-bold"
              >
                취소
              </Button>
              <Button
                onClick={onSubmit}
                className="flex-1 bg-gradient-to-r from-blue-500 to-cyan-500 text-white font-bold rounded-2xl h-12"
              >
                <CheckCircle className="w-5 h-5 mr-2" />
                처방전 저장
              </Button>
            </div>
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
}
