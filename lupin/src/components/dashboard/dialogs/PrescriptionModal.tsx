/**
 * PrescriptionModal.tsx
 *
 * 처방전 조회 모달 컴포넌트
 * - 발급된 처방전 상세 내용 표시
 * - 처방 약물 및 복용법 안내
 * - PDF 다운로드 기능
 */
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Download, FileText } from "lucide-react";
import { Prescription } from "@/types/dashboard.types";

interface PrescriptionModalProps {
  prescription: Prescription | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onDownload: (prescription: Prescription) => void;
}

export default function PrescriptionModal({
  prescription,
  open,
  onOpenChange,
  onDownload
}: PrescriptionModalProps) {
  if (!prescription) return null;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-2xl">
        <DialogHeader>
          <DialogTitle className="text-2xl font-black flex items-center gap-2">
            <FileText className="w-6 h-6 text-[#C93831]" />
            처방전 상세
          </DialogTitle>
        </DialogHeader>
        <div className="space-y-6">
          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <div className="text-sm text-gray-600 font-bold">처방명</div>
              <div className="font-black text-lg">{prescription.name}</div>
            </div>
            <div className="space-y-2">
              <div className="text-sm text-gray-600 font-bold">처방일</div>
              <div className="font-bold">{prescription.date}</div>
            </div>
            <div className="space-y-2">
              <div className="text-sm text-gray-600 font-bold">담당 의사</div>
              <div className="font-bold">{prescription.doctor}</div>
            </div>
            <div className="space-y-2">
              <div className="text-sm text-gray-600 font-bold">진단명</div>
              <div className="font-bold">{prescription.diagnosis}</div>
            </div>
          </div>

          <div className="space-y-2">
            <div className="text-sm text-gray-600 font-bold">처방 약물</div>
            <div className="flex gap-2 flex-wrap">
              {prescription.medicines.map((medicine, idx) => (
                <Badge
                  key={idx}
                  className="bg-gradient-to-r from-[#C93831] to-[#B02F28] text-white px-3 py-1 font-bold"
                >
                  {medicine}
                </Badge>
              ))}
            </div>
          </div>

          <div className="space-y-2">
            <div className="text-sm text-gray-600 font-bold">복용 방법</div>
            <div className="p-4 bg-gray-50 rounded-2xl">
              <p className="text-sm text-gray-700 font-medium">
                {prescription.instructions}
              </p>
            </div>
          </div>

          <Button
            onClick={() => onDownload(prescription)}
            className="w-full bg-gradient-to-r from-[#C93831] to-[#B02F28] hover:from-[#B02F28] hover:to-[#C93831] text-white font-bold rounded-2xl h-12"
          >
            <Download className="w-5 h-5 mr-2" />
            처방전 PDF 다운로드
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}
