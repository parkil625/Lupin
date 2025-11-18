/**
 * MemberDetailDialog.tsx
 *
 * 회원 상세 정보 다이얼로그 컴포넌트 (의사용)
 * - 회원 개인 정보 조회
 * - 활동 기록 및 건강 정보 표시
 * - 채팅 시작 및 처방전 작성 버튼
 */
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from "@/components/ui/dialog";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import { MessageCircle, FileText, User } from "lucide-react";
import { Member } from "@/types/dashboard.types";

interface MemberDetailDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  member: Member | null;
  onStartChat: (member: Member) => void;
  onWritePrescription: (member: Member) => void;
}

export default function MemberDetailDialog({
  open,
  onOpenChange,
  member,
  onStartChat,
  onWritePrescription
}: MemberDetailDialogProps) {
  if (!member) return null;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-3xl">
        <DialogHeader>
          <DialogTitle className="text-2xl font-black">회원 상세 정보</DialogTitle>
          <DialogDescription>회원의 진료 기록 및 정보를 확인할 수 있습니다.</DialogDescription>
        </DialogHeader>
        <div className="space-y-6 p-4">
          <div className="flex items-center gap-6">
            <Avatar className="w-20 h-20 border-4 border-white shadow-xl">
              <AvatarFallback className="bg-white">
                <User className="w-10 h-10 text-gray-400" />
              </AvatarFallback>
            </Avatar>
            <div>
              <h3 className="text-2xl font-black text-gray-900 mb-1">{member.name}</h3>
              <div className="text-gray-700 font-medium">{member.age}세 / {member.gender}</div>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div className="p-4 rounded-xl bg-gradient-to-br from-blue-50 to-cyan-50 border border-blue-200">
              <div className="text-sm text-gray-600 mb-1">최근 방문</div>
              <div className="font-bold text-gray-900">{member.lastVisit}</div>
            </div>
            <div className="p-4 rounded-xl bg-gradient-to-br from-purple-50 to-pink-50 border border-purple-200">
              <div className="text-sm text-gray-600 mb-1">진료 사유</div>
              <div className="font-bold text-gray-900">{member.condition}</div>
            </div>
          </div>

          <div className="p-6 rounded-2xl bg-gradient-to-br from-green-50 to-emerald-50 border border-green-200">
            <h4 className="text-lg font-black text-gray-900 mb-3">진료 기록</h4>
            <div className="space-y-2">
              <div className="p-3 bg-white rounded-lg">
                <div className="font-bold text-sm text-gray-900">2024-11-10 - 정기 검진</div>
                <div className="text-xs text-gray-600">혈압: 120/80, 혈당: 정상</div>
              </div>
              <div className="p-3 bg-white rounded-lg">
                <div className="font-bold text-sm text-gray-900">2024-10-15 - 건강 상담</div>
                <div className="text-xs text-gray-600">운동 처방, 식이요법 권장</div>
              </div>
            </div>
          </div>

          <div className="flex gap-3">
            <Button
              onClick={() => onStartChat(member)}
              className="flex-1 bg-gradient-to-r from-[#C93831] to-[#B02F28] text-white font-bold rounded-2xl h-12"
            >
              <MessageCircle className="w-5 h-5 mr-2" />
              채팅 시작
            </Button>
            <Button
              onClick={() => onWritePrescription(member)}
              className="flex-1 bg-gradient-to-r from-blue-500 to-cyan-500 text-white font-bold rounded-2xl h-12"
            >
              <FileText className="w-5 h-5 mr-2" />
              처방전 작성
            </Button>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
}
