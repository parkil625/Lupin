/**
 * PrescriptionModal.tsx
 *
 * 처방전 조회 모달 컴포넌트
 * - 발급된 처방전 상세 내용 표시
 * - 처방 약물 및 복용법 안내
 * - PDF 다운로드 기능
 */
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Download, FileText } from "lucide-react";
import { PrescriptionResponse } from "@/api/prescriptionApi";
import { generatePrescriptionPDF } from "@/utils/prescriptionPdfGenerator";

interface PrescriptionModalProps {
  prescription: PrescriptionResponse | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onDownload: (prescription: PrescriptionResponse) => void;
}

export default function PrescriptionModal({
  prescription,
  open,
  onOpenChange,
  onDownload,
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
          <DialogDescription>
            발급된 처방전의 상세 정보를 확인할 수 있습니다.
          </DialogDescription>
        </DialogHeader>
        <div className="space-y-6 max-h-[600px] overflow-y-auto pr-2 custom-scrollbar">
          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <div className="text-sm text-gray-600 font-bold">환자명</div>
              <div className="font-black text-lg">
                {prescription.patientName}
              </div>
            </div>
            <div className="space-y-2">
              <div className="text-sm text-gray-600 font-bold">처방일</div>
              <div className="font-bold">{prescription.date}</div>
            </div>
            <div className="space-y-2">
              <div className="text-sm text-gray-600 font-bold">담당 의사</div>
              <div className="font-bold">{prescription.doctorName}</div>
            </div>
            <div className="space-y-2">
              <div className="text-sm text-gray-600 font-bold">진단명</div>
              <div className="font-bold">{prescription.diagnosis}</div>
            </div>
          </div>

          <div className="space-y-2">
            <div className="text-sm text-gray-600 font-bold">처방 약물</div>
            <div className="space-y-3">
              {prescription.medicineDetails &&
              prescription.medicineDetails.length > 0 ? (
                prescription.medicineDetails.map((medicine, idx) => (
                  <div key={idx} className="p-3 bg-gray-50 rounded-xl">
                    <div className="font-bold text-gray-900">
                      {medicine.name}
                    </div>
                    {medicine.precautions && (
                      <div className="mt-2 p-2 bg-yellow-50 border border-yellow-200 rounded-lg">
                        <div className="text-xs font-bold text-yellow-800 mb-1">
                          ⚠️ 주의사항
                        </div>
                        <div className="text-xs text-yellow-700">
                          {medicine.precautions}
                        </div>
                      </div>
                    )}
                  </div>
                ))
              ) : (
                <div className="text-gray-500 text-sm">
                  처방된 약물이 없습니다.
                </div>
              )}
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
            onClick={() => {
              console.log(
                "[PrescriptionModal] 처방전 PDF 다운로드 시작:",
                prescription
              );
              generatePrescriptionPDF(prescription);
              onDownload(prescription); // [Fix] 부모 컴포넌트 알림 & 에러 해결
            }}
            className="w-full bg-gradient-to-r from-[#C93831] to-[#B02F28] hover:from-[#B02F28] hover:to-[#C93831] text-white font-bold rounded-2xl h-12 cursor-pointer"
          >
            <Download className="w-5 h-5 mr-2" />
            처방전 PDF 다운로드
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}
