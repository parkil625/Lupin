import { useState, useEffect } from "react";
import { format } from "date-fns";
import { ko } from "date-fns/locale";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Calendar } from "@/components/ui/calendar";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Calendar as CalendarIcon,
  User,
  Stethoscope,
  CheckCircle2,
  X,
} from "lucide-react";

interface AppointmentDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  availableDates: Date[];
  availableTimes: string[];
  bookedTimes: string[];
  selectedDepartment: string;
  setSelectedDepartment: (dept: string) => void;
  selectedDate: Date | undefined;
  setSelectedDate: (date: Date | undefined) => void;
  selectedTime: string;
  setSelectedTime: (time: string) => void;
  onConfirm: (doctorId: number, date: Date, time: string) => Promise<number | void>; // Returns appointment ID
  onCancel?: (appointmentId: number) => Promise<void>; // Optional cancel callback
}

export default function AppointmentDialog({
  open,
  onOpenChange,
  availableDates,
  availableTimes,
  bookedTimes,
  selectedDepartment,
  setSelectedDepartment,
  selectedDate,
  setSelectedDate,
  selectedTime,
  setSelectedTime,
  onConfirm,
  onCancel,
}: AppointmentDialogProps) {
  // --- 상태 관리 ---
  const [step, setStep] = useState<1 | "success">(1);
  const [selectedDoctorId, setSelectedDoctorId] = useState<string>("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [createdAppointmentId, setCreatedAppointmentId] = useState<number | null>(null);

  // 임시 데이터 - 실제로는 API에서 가져와야 함
  const departments = [
    { id: "1", name: "내과" },
    { id: "2", name: "외과" },
    { id: "3", name: "정형외과" },
  ];

  const doctors = [
    { id: "1", name: "김의사", departmentId: "1" },
    { id: "2", name: "이의사", departmentId: "2" },
    { id: "3", name: "박의사", departmentId: "3" },
  ];

  // 다이얼로그가 닫힐 때 초기화 (다음 열림을 위해)
  useEffect(() => {
    if (!open) {
      // 닫힐 때 상태 초기화 (다음 열림을 대비)
      const timer = setTimeout(() => {
        setStep(1 as const);
        setSelectedDepartment("");
        setSelectedDoctorId("");
        setSelectedDate(undefined);
        setSelectedTime("");
      }, 200); // 애니메이션 이후 초기화
      return () => clearTimeout(timer);
    }
  }, [open, setSelectedDepartment, setSelectedDate, setSelectedTime]);

  // 선택된 정보 객체 찾기 (화면 표시용)
  const selectedDept = departments.find((d) => d.id === selectedDepartment);
  const selectedDoctor = doctors.find((d) => d.id === selectedDoctorId);

  // 해당 과의 의사 목록 필터링
  const filteredDoctors = doctors.filter(
    (doc) => doc.departmentId === selectedDepartment
  );

  // 예약 확정 핸들러
  const handleConfirm = async () => {
    if (!selectedDepartment || !selectedDoctorId || !selectedDate || !selectedTime)
      return;

    setIsSubmitting(true);

    // 즉시 success 화면으로 전환 (사용자에게 빠른 피드백)
    setStep("success");

    // 부모 컴포넌트의 onConfirm 호출 (실제 API 호출)
    const doctorId = parseInt(selectedDoctorId);
    const appointmentId = await onConfirm(doctorId, selectedDate, selectedTime);

    // 생성된 예약 ID 저장
    if (typeof appointmentId === 'number') {
      setCreatedAppointmentId(appointmentId);
    }

    // API 완료 후 버튼 활성화
    setTimeout(() => {
      setIsSubmitting(false);
    }, 500);
  };

  // 예약 변경 (처음으로 돌아가기)
  const handleChange = () => {
    setStep(1);
  };

  // 예약 취소 (실제 예약 취소 API 호출)
  const handleCancelAppointment = async () => {
    if (!createdAppointmentId || !onCancel) {
      // onCancel이 없으면 그냥 닫기
      onOpenChange(false);
      return;
    }

    if (!confirm("예약을 취소하시겠습니까?")) {
      return;
    }

    setIsSubmitting(true);
    try {
      await onCancel(createdAppointmentId);
      onOpenChange(false);
    } catch (error) {
      console.error("예약 취소 실패:", error);
    } finally {
      setIsSubmitting(false);
    }
  };

  if (!open) return null;

  return (
    <div className="w-full h-full bg-white rounded-xl overflow-hidden">
      {/* 헤더 */}
      <div className="px-6 pt-6 pb-2 border-b border-gray-100">
        <div className="flex items-center justify-between mb-2">
          <h2 className="text-2xl font-black text-gray-900">
            {step === "success" ? "예약 대기" : "진료 예약"}
          </h2>
          <button
            onClick={() => onOpenChange(false)}
            className="p-1 hover:bg-gray-100 rounded-lg transition-colors"
          >
            <X className="w-5 h-5 text-gray-500" />
          </button>
        </div>
        <p className="text-sm text-gray-500">
          {step === "success"
            ? "예약 내용을 확인해주세요."
            : "진료과와 의료진, 날짜를 선택해주세요."}
        </p>
      </div>

      <div className="px-6 pb-6 pt-4">
          {/* STEP 1: 예약 정보 입력 */}
          {step === 1 && (
            <div className="space-y-6">
              {/* 진료과 선택 */}
              <div>
                <Label className="text-base font-bold mb-2 block text-gray-700">
                  진료과 선택
                </Label>
                <Select
                  value={selectedDepartment}
                  onValueChange={(val) => {
                    setSelectedDepartment(val);
                    setSelectedDoctorId(""); // 과 변경 시 의사 초기화
                  }}
                >
                  <SelectTrigger className="rounded-xl h-11">
                    <SelectValue placeholder="진료과를 선택하세요" />
                  </SelectTrigger>
                  <SelectContent>
                    {departments.map((dept) => (
                      <SelectItem key={dept.id} value={dept.id}>
                        {dept.name}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              {/* 의료진 선택 (진료과 선택 시 표시) */}
              {selectedDepartment && (
                <div className="animate-in fade-in slide-in-from-top-2 duration-300">
                  <Label className="text-base font-bold mb-2 block text-gray-700">
                    의료진 선택
                  </Label>
                  <Select
                    value={selectedDoctorId}
                    onValueChange={setSelectedDoctorId}
                  >
                    <SelectTrigger className="rounded-xl h-11">
                      <SelectValue placeholder="의료진을 선택하세요" />
                    </SelectTrigger>
                    <SelectContent>
                      {filteredDoctors.map((doc) => (
                        <SelectItem key={doc.id} value={doc.id}>
                          {doc.name} 선생님
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
              )}

              {/* 날짜 선택 (의료진 선택 시 표시) */}
              {selectedDoctorId && (
                <div className="animate-in fade-in slide-in-from-top-2 duration-300">
                  <Label className="text-base font-bold mb-2 block text-gray-700">
                    날짜 선택
                  </Label>
                  <div className="border rounded-xl p-3 flex justify-center bg-gray-50/50">
                    <Calendar
                      mode="single"
                      selected={selectedDate}
                      onSelect={setSelectedDate}
                      locale={ko}
                      modifiers={{ available: availableDates }}
                      modifiersStyles={{
                        available: { fontWeight: "bold", color: "#C93831" },
                      }}
                      className="rounded-md bg-white shadow-sm"
                    />
                  </div>
                  <p className="text-xs text-gray-500 mt-2 ml-1">
                    * 붉은색 날짜만 예약 가능합니다.
                  </p>
                </div>
              )}

              {/* 시간 선택 (날짜 선택 시 표시) */}
              {selectedDate && (
                <div className="animate-in fade-in slide-in-from-top-2 duration-300">
                  <Label className="text-base font-bold mb-2 block text-gray-700">
                    시간 선택
                  </Label>
                  <div className="grid grid-cols-3 gap-2">
                    {availableTimes.map((time) => {
                      const isBooked = bookedTimes.includes(time);
                      const isSelected = selectedTime === time;
                      return (
                        <Button
                          key={time}
                          variant={isSelected ? "default" : "outline"}
                          disabled={isBooked}
                          onClick={() => setSelectedTime(time)}
                          className={`rounded-xl h-10 text-sm ${
                            isSelected ? "bg-[#C93831] hover:bg-[#B02F28]" : ""
                          } ${isBooked ? "opacity-40 bg-gray-100" : ""}`}
                        >
                          {time}
                        </Button>
                      );
                    })}
                  </div>
                </div>
              )}

              {/* 예약 하기 버튼 */}
              <Button
                disabled={
                  !selectedDepartment ||
                  !selectedDoctorId ||
                  !selectedDate ||
                  !selectedTime ||
                  isSubmitting
                }
                className="w-full bg-[#C93831] hover:bg-[#B02F28] text-white font-bold rounded-xl h-12 text-lg shadow-md mt-4"
                onClick={handleConfirm}
              >
                {isSubmitting ? "처리 중..." : "예약하기"}
              </Button>
            </div>
          )}

          {/* STEP 2 (Success): 예약 대기/완료 화면 */}
          {step === "success" && selectedDept && selectedDoctor && (
            <div className="flex flex-col items-center animate-in zoom-in-95 duration-300 py-2">
              {/* 완료 아이콘 */}
              <div className="mb-6 bg-green-50 p-4 rounded-full">
                <CheckCircle2 className="w-10 h-10 text-green-600" />
              </div>

              {/* 예약 정보 카드 */}
              <div className="w-full bg-gray-50 rounded-2xl p-5 space-y-4 mb-6 border border-gray-100 shadow-sm">
                {/* 진료과 */}
                <div className="flex items-center gap-4 p-2">
                  <div className="bg-white p-2 rounded-lg shadow-sm text-blue-600">
                    <Stethoscope className="w-5 h-5" />
                  </div>
                  <div>
                    <p className="text-xs text-gray-500 font-medium">진료과</p>
                    <p className="text-sm font-bold text-gray-900">
                      {selectedDept.name}
                    </p>
                  </div>
                </div>

                {/* 담당 의사 */}
                <div className="flex items-center gap-4 p-2 border-t border-gray-100 pt-4">
                  <div className="bg-white p-2 rounded-lg shadow-sm text-blue-600">
                    <User className="w-5 h-5" />
                  </div>
                  <div>
                    <p className="text-xs text-gray-500 font-medium">
                      담당 의사
                    </p>
                    <p className="text-sm font-bold text-gray-900">
                      {selectedDoctor.name} 선생님
                    </p>
                  </div>
                </div>

                {/* 예약 시간 */}
                <div className="flex items-center gap-4 p-2 border-t border-gray-100 pt-4">
                  <div className="bg-white p-2 rounded-lg shadow-sm text-blue-600">
                    <CalendarIcon className="w-5 h-5" />
                  </div>
                  <div>
                    <p className="text-xs text-gray-500 font-medium">
                      예약 일시
                    </p>
                    <p className="text-sm font-bold text-gray-900">
                      {selectedDate &&
                        format(selectedDate, "M월 d일 (eee)", {
                          locale: ko,
                        })}{" "}
                      {selectedTime}
                    </p>
                  </div>
                </div>
              </div>

              {/* 하단 버튼 (변경 / 취소) */}
              <div className="flex w-full gap-3 mt-2">
                <Button
                  variant="outline"
                  className="flex-1 h-12 rounded-xl border-gray-300 text-gray-700 font-bold hover:bg-gray-50"
                  onClick={handleChange}
                  disabled={isSubmitting}
                >
                  {isSubmitting ? "처리 중..." : "예약 변경"}
                </Button>
                <Button
                  variant="outline"
                  className="flex-1 h-12 rounded-xl border-red-100 text-red-600 bg-red-50 hover:bg-red-100 hover:text-red-700 font-bold hover:border-red-200"
                  onClick={handleCancelAppointment}
                  disabled={isSubmitting}
                >
                  {isSubmitting ? "처리 중..." : "예약 취소"}
                </Button>
              </div>
            </div>
          )}
      </div>
    </div>
  );
}
