import { useState } from "react";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Button } from "@/components/ui/button";
import { CheckCircle, PlusSquare, Trash2, User, Search } from "lucide-react";
import { prescriptionApi, MedicineItem, MedicineResponse } from "@/api/prescriptionApi";

interface PrescriptionDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  appointmentId: number;
  patientId: number;
  patientName: string;
  onSuccess: () => void;
}

export default function PrescriptionDialog({
  open,
  onOpenChange,
  appointmentId,
  patientId,
  patientName,
  onSuccess,
}: PrescriptionDialogProps) {
  const [diagnosis, setDiagnosis] = useState("");
  const [medicines, setMedicines] = useState<MedicineItem[]>([
    { medicineName: "", dosage: "", frequency: "", durationDays: undefined, instructions: "" }
  ]);
  const [searchQuery, setSearchQuery] = useState("");
  const [searchResults, setSearchResults] = useState<MedicineResponse[]>([]);
  const [isSearching, setIsSearching] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleAddMedicine = () => {
    setMedicines([
      ...medicines,
      { medicineName: "", dosage: "", frequency: "", durationDays: undefined, instructions: "" }
    ]);
  };

  const handleRemoveMedicine = (index: number) => {
    if (medicines.length > 1) {
      setMedicines(medicines.filter((_, i) => i !== index));
    }
  };

  const handleMedicineChange = (index: number, field: keyof MedicineItem, value: any) => {
    const updated = [...medicines];
    updated[index] = { ...updated[index], [field]: value };
    setMedicines(updated);
  };

  const handleSearchMedicine = async () => {
    if (!searchQuery.trim()) return;

    setIsSearching(true);
    try {
      const results = await prescriptionApi.searchMedicines(searchQuery);
      setSearchResults(results);
    } catch (error) {
      console.error("약품 검색 실패:", error);
      alert("약품 검색에 실패했습니다.");
    } finally {
      setIsSearching(false);
    }
  };

  const handleSelectMedicine = (medicine: MedicineResponse, index: number) => {
    const updated = [...medicines];
    updated[index] = {
      ...updated[index],
      medicineId: medicine.id,
      medicineName: medicine.name,
      dosage: medicine.standardDosage || "",
    };
    setMedicines(updated);
    setSearchResults([]);
    setSearchQuery("");
  };

  const handleSubmit = async () => {
    // 유효성 검사
    if (!diagnosis.trim()) {
      alert("진단명을 입력해주세요.");
      return;
    }

    const validMedicines = medicines.filter(m => m.medicineName.trim() && m.dosage.trim() && m.frequency.trim());
    if (validMedicines.length === 0) {
      alert("최소 하나 이상의 약품을 처방해야 합니다.");
      return;
    }

    setIsSubmitting(true);
    try {
      await prescriptionApi.create({
        appointmentId,
        patientId,
        diagnosis,
        medicines: validMedicines,
      });

      alert("처방전이 성공적으로 발급되었습니다.");
      onSuccess();
      onOpenChange(false);

      // 초기화
      setDiagnosis("");
      setMedicines([{ medicineName: "", dosage: "", frequency: "", durationDays: undefined, instructions: "" }]);
    } catch (error: any) {
      console.error("처방전 발급 실패:", error);
      alert(error.response?.data?.message || "처방전 발급에 실패했습니다.");
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-4xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle className="text-2xl font-black">처방전 발급</DialogTitle>
        </DialogHeader>

        <div className="space-y-6">
          {/* 환자 정보 */}
          <div className="p-4 rounded-2xl bg-gradient-to-br from-blue-50 to-cyan-50 border border-blue-200">
            <div className="flex items-center gap-4">
              <Avatar className="w-12 h-12">
                <AvatarFallback className="bg-white">
                  <User className="w-6 h-6 text-gray-400" />
                </AvatarFallback>
              </Avatar>
              <div>
                <div className="font-black text-lg text-gray-900">{patientName}</div>
                <div className="text-sm text-gray-600">예약 #{appointmentId}</div>
              </div>
            </div>
          </div>

          {/* 진단명 */}
          <div>
            <Label className="text-base font-black mb-2 block">진단명 *</Label>
            <Input
              placeholder="진단명을 입력하세요 (예: 급성 상기도 감염)"
              className="rounded-xl"
              value={diagnosis}
              onChange={(e) => setDiagnosis(e.target.value)}
            />
          </div>

          {/* 약품 검색 */}
          <div>
            <Label className="text-base font-black mb-2 block">약품 검색</Label>
            <div className="flex gap-2">
              <Input
                placeholder="약품명으로 검색"
                className="rounded-xl"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                onKeyPress={(e) => e.key === 'Enter' && handleSearchMedicine()}
              />
              <Button
                variant="outline"
                className="rounded-xl"
                onClick={handleSearchMedicine}
                disabled={isSearching}
              >
                <Search className="w-4 h-4 mr-2" />
                검색
              </Button>
            </div>

            {searchResults.length > 0 && (
              <div className="mt-2 p-3 rounded-xl border bg-white max-h-48 overflow-y-auto">
                {searchResults.map((medicine) => (
                  <div
                    key={medicine.id}
                    className="p-2 hover:bg-gray-50 rounded cursor-pointer"
                    onClick={() => handleSelectMedicine(medicine, 0)}
                  >
                    <div className="font-semibold">{medicine.name}</div>
                    <div className="text-sm text-gray-600">
                      {medicine.manufacturer} · {medicine.standardDosage} {medicine.unit}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* 처방 의약품 */}
          <div>
            <Label className="text-base font-black mb-2 block">처방 의약품 *</Label>
            <div className="space-y-3">
              {medicines.map((medicine, index) => (
                <div key={index} className="p-4 rounded-xl border bg-gray-50 space-y-3">
                  <div className="flex gap-2">
                    <Input
                      placeholder="약품명"
                      className="rounded-xl flex-1"
                      value={medicine.medicineName}
                      onChange={(e) => handleMedicineChange(index, "medicineName", e.target.value)}
                    />
                    <Input
                      placeholder="용량 (예: 500mg)"
                      className="rounded-xl w-32"
                      value={medicine.dosage}
                      onChange={(e) => handleMedicineChange(index, "dosage", e.target.value)}
                    />
                    <Input
                      placeholder="복용 빈도 (예: 1일 3회)"
                      className="rounded-xl w-40"
                      value={medicine.frequency}
                      onChange={(e) => handleMedicineChange(index, "frequency", e.target.value)}
                    />
                    <Input
                      type="number"
                      placeholder="일수"
                      className="rounded-xl w-24"
                      value={medicine.durationDays || ""}
                      onChange={(e) => handleMedicineChange(index, "durationDays", e.target.value ? parseInt(e.target.value) : undefined)}
                    />
                    {medicines.length > 1 && (
                      <Button
                        variant="ghost"
                        size="icon"
                        className="rounded-xl text-red-600"
                        onClick={() => handleRemoveMedicine(index)}
                      >
                        <Trash2 className="w-4 h-4" />
                      </Button>
                    )}
                  </div>
                  <Textarea
                    placeholder="복용 지침 (선택사항)"
                    className="rounded-xl"
                    value={medicine.instructions || ""}
                    onChange={(e) => handleMedicineChange(index, "instructions", e.target.value)}
                  />
                </div>
              ))}
              <Button
                variant="outline"
                className="w-full rounded-xl border-2 border-dashed border-gray-300 hover:border-[#C93831] hover:bg-red-50"
                onClick={handleAddMedicine}
              >
                <PlusSquare className="w-4 h-4 mr-2" />
                약품 추가
              </Button>
            </div>
          </div>

          {/* 버튼 */}
          <div className="flex gap-3 pt-4">
            <Button
              variant="outline"
              onClick={() => onOpenChange(false)}
              className="flex-1 rounded-2xl h-12 font-bold"
              disabled={isSubmitting}
            >
              취소
            </Button>
            <Button
              onClick={handleSubmit}
              className="flex-1 bg-gradient-to-r from-[#C93831] to-[#B02F28] text-white font-bold rounded-2xl h-12"
              disabled={isSubmitting}
            >
              <CheckCircle className="w-5 h-5 mr-2" />
              {isSubmitting ? "발급 중..." : "처방전 발급"}
            </Button>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
}
