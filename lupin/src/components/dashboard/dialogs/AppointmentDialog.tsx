/**
 * AppointmentDialog.tsx
 *
 * 예약 신청 다이얼로그 컴포넌트
 * - 진료과 선택 및 날짜/시간 선택
 * - 예약 가능 시간 표시
 * - 예약 확정 기능
 */
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Calendar } from "@/components/ui/calendar";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";

interface AppointmentDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  selectedDepartment: string;
  setSelectedDepartment: (department: string) => void;
  selectedDate: Date | undefined;
  setSelectedDate: (date: Date | undefined) => void;
  selectedTime: string;
  setSelectedTime: (time: string) => void;
  availableDates: Date[];
  availableTimes: string[];
  bookedTimes: string[];
  onConfirm: () => void;
}

export default function AppointmentDialog({
  open,
  onOpenChange,
  selectedDepartment,
  setSelectedDepartment,
  selectedDate,
  setSelectedDate,
  selectedTime,
  setSelectedTime,
  availableDates,
  availableTimes,
  bookedTimes,
  onConfirm
}: AppointmentDialogProps) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-2xl">
        <DialogHeader>
          <DialogTitle className="text-2xl font-black">진료 예약</DialogTitle>
          <DialogDescription>진료과와 날짜, 시간을 선택하여 예약할 수 있습니다.</DialogDescription>
        </DialogHeader>
        <div className="space-y-6">
          <div>
            <Label className="text-base font-black mb-2 block">진료과 선택</Label>
            <Select value={selectedDepartment} onValueChange={setSelectedDepartment}>
              <SelectTrigger className="rounded-xl">
                <SelectValue placeholder="진료과를 선택하세요" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="internal">내과</SelectItem>
                <SelectItem value="surgery">외과</SelectItem>
                <SelectItem value="psychiatry">신경정신과</SelectItem>
                <SelectItem value="dermatology">피부과</SelectItem>
              </SelectContent>
            </Select>
          </div>

          {selectedDepartment && (
            <>
              <div>
                <Label className="text-base font-black mb-2 block">날짜 선택</Label>
                <Calendar
                  mode="single"
                  selected={selectedDate}
                  onSelect={setSelectedDate}
                  modifiers={{
                    available: availableDates
                  }}
                  modifiersStyles={{
                    available: {
                      fontWeight: 'bold',
                      color: '#C93831'
                    }
                  }}
                  className="rounded-xl border"
                />
                <p className="text-xs text-gray-600 mt-2">* 빨간색 날짜만 선택 가능합니다</p>
              </div>

              {selectedDate && (
                <div>
                  <Label className="text-base font-black mb-2 block">시간 선택</Label>
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
                          className={`rounded-xl ${isSelected ? 'bg-[#C93831]' : ''} ${isBooked ? 'opacity-50' : ''}`}
                        >
                          {time}
                          {isBooked && " (예약됨)"}
                        </Button>
                      );
                    })}
                  </div>
                </div>
              )}
            </>
          )}

          <Button
            disabled={!selectedDepartment || !selectedDate || !selectedTime}
            className="w-full bg-gradient-to-r from-[#C93831] to-[#B02F28] text-white font-bold rounded-xl h-12"
            onClick={onConfirm}
          >
            예약 확인
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}
