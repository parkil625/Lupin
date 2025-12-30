import { AxiosError } from "axios";
import { useState, useEffect } from "react";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Button } from "@/components/ui/button";
import { CheckCircle, PlusSquare, Trash2, User } from "lucide-react";
import {
  prescriptionApi,
  MedicineItem,
  MedicineResponse,
} from "@/api/prescriptionApi";

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
    {
      medicineName: "",
      dosage: "",
      frequency: "",
      durationDays: undefined,
      instructions: "",
    },
  ]);
  const [searchQuery, setSearchQuery] = useState("");
  const [searchResults, setSearchResults] = useState<MedicineResponse[]>([]);
  const [, setIsSearching] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [existingPrescription, setExistingPrescription] = useState<boolean>(false);

  // 다이얼로그가 열릴 때 기존 처방전 확인
  useEffect(() => {
    if (!open) return;

    console.log("처방전 다이얼로그 열림:", { appointmentId, patientId, patientName });

    const checkExistingPrescription = async () => {
      const response = await prescriptionApi.getByAppointmentId(appointmentId);
      console.log("기존 처방전 확인:", response);
      setExistingPrescription(response !== null);
    };

    checkExistingPrescription();
  }, [open, appointmentId, patientId, patientName]);

  const handleAddMedicine = () => {
    setMedicines([
      ...medicines,
      {
        medicineName: "",
        dosage: "",
        frequency: "",
        durationDays: undefined,
        instructions: "",
      },
    ]);
  };

  const handleRemoveMedicine = (index: number) => {
    if (medicines.length > 1) {
      setMedicines(medicines.filter((_, i) => i !== index));
    }
  };

  const handleMedicineChange = (
    index: number,
    field: keyof MedicineItem,
    value: string | number | undefined // 또는 MedicineItem[keyof MedicineItem]
  ) => {
    const updated = [...medicines];
    // 타입스크립트 설정에 따라 여기서 타입 단언(as)이 필요할 수 있습니다.
    updated[index] = { ...updated[index], [field]: value } as MedicineItem;
    setMedicines(updated);
  };

  const handleSearchMedicine = async (query?: string) => {
    const searchTerm = query || searchQuery;
    if (!searchTerm.trim() || searchTerm.length < 2) {
      setSearchResults([]);
      return;
    }

    setIsSearching(true);
    try {
      const results = await prescriptionApi.searchMedicines(searchTerm);
      setSearchResults(results);
    } catch (error) {
      console.error("약품 검색 실패:", error);
      setSearchResults([]);
    } finally {
      setIsSearching(false);
    }
  };

  const [selectedMedicineIndex, setSelectedMedicineIndex] = useState<number>(0);

  const handleSelectMedicine = (medicine: MedicineResponse) => {
    const updated = [...medicines];
    updated[selectedMedicineIndex] = {
      ...updated[selectedMedicineIndex],
      medicineId: medicine.id,
      medicineName: medicine.name,
      dosage: medicine.standardDosage || "",
    };
    setMedicines(updated);
    setSearchResults([]);
    setSearchQuery("");
  };

  const handleSubmit = async () => {
    console.log("=== 처방전 저장 버튼 클릭됨 ===");
    console.log("현재 상태:", { diagnosis, medicines, appointmentId, patientId });

    // 유효성 검사
    if (!diagnosis.trim()) {
      console.log("유효성 검사 실패: 진단명 없음");
      alert("진단명을 입력해주세요.");
      return;
    }

    const validMedicines = medicines.filter(
      (m) => m.medicineName.trim() && m.dosage.trim() && m.frequency.trim()
    );
    console.log("유효한 약품 개수:", validMedicines.length);

    if (validMedicines.length === 0) {
      console.log("유효성 검사 실패: 약품 없음");
      alert("최소 하나 이상의 약품을 처방해야 합니다.");
      return;
    }

    console.log("유효성 검사 통과, API 요청 시작");
    setIsSubmitting(true);
    try {
      const requestData = {
        appointmentId,
        patientId,
        diagnosis,
        medicines: validMedicines,
      };

      console.log("처방전 발급 요청 데이터:", JSON.stringify(requestData, null, 2));

      const response = await prescriptionApi.create(requestData);

      console.log("처방전 발급 성공:", response);

      alert("처방전이 성공적으로 발급되었습니다.");
      onSuccess();
      onOpenChange(false);

      // 초기화
      setDiagnosis("");
      setMedicines([
        {
          medicineName: "",
          dosage: "",
          frequency: "",
          durationDays: undefined,
          instructions: "",
        },
      ]);
    } catch (error) {
      // error를 AxiosError로 간주하고, 응답 데이터에 message(string)가 있다고 정의
      const axiosError = error as AxiosError<{ message: string }>;

      console.error("처방전 발급 실패:", error);
      console.error("에러 상세:", axiosError.response?.data);

      // axiosError 변수를 사용하여 안전하게 접근
      alert(
        axiosError.response?.data?.message || "처방전 발급에 실패했습니다."
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
      <Dialog open={open} onOpenChange={onOpenChange}>
        <DialogContent className="max-w-4xl max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle className="text-2xl font-black">
              처방전 발급
            </DialogTitle>
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
                  <div className="font-black text-lg text-gray-900">
                    {patientName}
                  </div>
                  <div className="text-sm text-gray-600">
                    예약 #{appointmentId}
                  </div>
                </div>
              </div>
            </div>

            {/* 진단명 */}
            <div>
              <Label className="text-base font-black mb-2 block">
                진단명 *
              </Label>
              <Input
                placeholder="진단명을 입력하세요 (예: 급성 상기도 감염)"
                className="rounded-xl"
                value={diagnosis}
                onChange={(e) => setDiagnosis(e.target.value)}
              />
            </div>

            {/* 처방 의약품 */}
            <div>
              <Label className="text-base font-black mb-2 block">
                처방 의약품 *
              </Label>
              <div className="space-y-3">
                {medicines.map((medicine, index) => (
                  <div
                    key={index}
                    className="p-4 rounded-xl border bg-gray-50 space-y-3"
                  >
                    {/* 약품 검색 */}
                    <div className="relative">
                      <div className="flex gap-2">
                        <div className="flex-1 relative">
                          <Input
                            placeholder="약품명으로 검색 (타이레놀, 부루펜 등)"
                            className="rounded-xl"
                            value={
                              index === selectedMedicineIndex
                                ? searchQuery
                                : medicine.medicineName
                            }
                            onChange={(e) => {
                              setSelectedMedicineIndex(index);
                              setSearchQuery(e.target.value);
                              handleMedicineChange(
                                index,
                                "medicineName",
                                e.target.value
                              );
                              handleSearchMedicine(e.target.value);
                            }}
                            onFocus={() => {
                              setSelectedMedicineIndex(index);
                              setSearchQuery(medicine.medicineName);
                              if (medicine.medicineName.length >= 2) {
                                handleSearchMedicine(medicine.medicineName);
                              }
                            }}
                          />
                          {index === selectedMedicineIndex &&
                            searchResults.length > 0 && (
                              <div className="absolute z-10 mt-1 w-full p-2 rounded-xl border bg-white shadow-lg max-h-60 overflow-y-auto">
                                {searchResults.map((med) => (
                                  <div
                                    key={med.id}
                                    className="p-3 hover:bg-blue-50 rounded-lg cursor-pointer border-b last:border-b-0"
                                    onClick={() => handleSelectMedicine(med)}
                                  >
                                    <div className="font-bold text-sm text-gray-900">
                                      {med.name}
                                    </div>
                                    {med.description && (
                                      <div className="text-xs text-gray-500 mt-1">
                                        {med.description}
                                      </div>
                                    )}
                                  </div>
                                ))}
                              </div>
                            )}
                        </div>
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
                    </div>

                    {/* 용량, 빈도, 일수 */}
                    <div className="flex gap-2">
                      <Input
                        placeholder="용량 (예: 500mg)"
                        className="rounded-xl flex-1"
                        value={medicine.dosage}
                        onChange={(e) =>
                          handleMedicineChange(index, "dosage", e.target.value)
                        }
                      />
                      <Input
                        placeholder="복용 빈도 (예: 1일 3회)"
                        className="rounded-xl flex-1"
                        value={medicine.frequency}
                        onChange={(e) =>
                          handleMedicineChange(
                            index,
                            "frequency",
                            e.target.value
                          )
                        }
                      />
                      <Input
                        type="number"
                        placeholder="일수"
                        className="rounded-xl w-24"
                        value={medicine.durationDays || ""}
                        onChange={(e) =>
                          handleMedicineChange(
                            index,
                            "durationDays",
                            e.target.value
                              ? parseInt(e.target.value)
                              : undefined
                          )
                        }
                      />
                    </div>

                    {/* 복용 지침 */}
                    <Textarea
                      placeholder="복용 지침 (선택사항)"
                      className="rounded-xl"
                      value={medicine.instructions || ""}
                      onChange={(e) =>
                        handleMedicineChange(
                          index,
                          "instructions",
                          e.target.value
                        )
                      }
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
                {isSubmitting
                  ? (existingPrescription ? "수정 중..." : "발급 중...")
                  : (existingPrescription ? "처방전 수정" : "처방전 발급")
                }
              </Button>
            </div>
          </div>
        </DialogContent>
      </Dialog>
  );
}
