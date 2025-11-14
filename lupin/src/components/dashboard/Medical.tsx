import { Card } from "../ui/card";
import { Badge } from "../ui/badge";
import { Button } from "../ui/button";
import {
  Calendar as CalendarIcon,
  Clock,
  FileText,
  MessageCircle,
  XCircle,
} from "lucide-react";
import { Prescription } from "@/types/dashboard.types";

interface MedicalProps {
  setShowAppointment: (show: boolean) => void;
  setShowChat: (show: boolean) => void;
  setSelectedPrescription: (prescription: Prescription | null) => void;
}

export default function Medical({
  setShowAppointment,
  setShowChat,
  setSelectedPrescription,
}: MedicalProps) {
  const prescriptions: Prescription[] = [
    {
      id: 1,
      name: "감기약 처방",
      date: "11월 10일",
      doctor: "이의사",
      medicines: ["타이레놀 500mg", "콧물약", "기침약"],
      diagnosis: "급성 상기도 감염",
      instructions:
        "하루 3회, 식후 30분에 복용하세요. 충분한 휴식과 수분 섭취가 필요합니다.",
    },
    {
      id: 2,
      name: "소화제 처방",
      date: "10월 28일",
      doctor: "최의사",
      medicines: ["소화제", "제산제"],
      diagnosis: "소화불량",
      instructions: "하루 2회, 식후에 복용하세요.",
    },
    {
      id: 3,
      name: "진통제 처방",
      date: "10월 15일",
      doctor: "김의사",
      medicines: ["이부프로펜 200mg"],
      diagnosis: "근육통",
      instructions: "통증이 있을 때 4-6시간 간격으로 복용하세요.",
    },
    {
      id: 4,
      name: "알레르기약",
      date: "10월 1일",
      doctor: "박의사",
      medicines: ["항히스타민제"],
      diagnosis: "알레르기성 비염",
      instructions: "하루 1회, 취침 전 복용하세요.",
    },
  ];

  const appointments = [
    {
      id: 1,
      type: "내과 상담",
      doctor: "김의사",
      date: "11월 15일",
      time: "오후 3시",
      status: "예정",
      hasChat: true,
    },
    {
      id: 2,
      type: "정형외과",
      doctor: "이의사",
      date: "11월 10일",
      time: "오전 10시",
      status: "완료",
      hasChat: false,
    },
    {
      id: 3,
      type: "피부과",
      doctor: "박의사",
      date: "11월 5일",
      time: "오후 2시",
      status: "완료",
      hasChat: false,
    },
    {
      id: 4,
      type: "내과",
      doctor: "최의사",
      date: "10월 28일",
      time: "오전 11시",
      status: "완료",
      hasChat: false,
    },
  ];

  return (
    <div className="h-full overflow-auto p-8">
      <div className="max-w-7xl mx-auto space-y-6">
        <div>
          <h1 className="text-5xl font-black text-gray-900 mb-2">
            비대면 진료
          </h1>
          <p className="text-gray-700 font-medium text-lg">
            전문 의료진과 상담하세요
          </p>
        </div>

        <div className="grid grid-cols-4 gap-6">
          {/* New Appointment */}
          <Card className="col-span-2 backdrop-blur-2xl bg-white/60 border border-gray-200 shadow-xl hover:shadow-2xl transition-all h-64">
            <div className="h-full p-6 flex flex-col items-center justify-center text-center space-y-4">
              <div className="w-16 h-16 bg-gradient-to-br from-[#C93831] to-[#B02F28] rounded-3xl flex items-center justify-center shadow-xl">
                <CalendarIcon className="w-8 h-8 text-white" />
              </div>
              <div>
                <h3 className="text-2xl font-black text-gray-900 mb-1">
                  새 진료 예약
                </h3>
                <p className="text-gray-600 font-medium text-sm">
                  의료진과 비대면 상담
                </p>
              </div>
              <Button
                onClick={() => setShowAppointment(true)}
                className="bg-gradient-to-r from-[#C93831] to-[#B02F28] hover:from-[#B02F28] hover:to-[#C93831] text-white font-bold px-6 py-4 rounded-2xl border-0"
              >
                예약하기
              </Button>
            </div>
          </Card>

          {/* Appointments */}
          <Card className="col-span-2 backdrop-blur-2xl bg-white/60 border border-gray-200 shadow-xl overflow-hidden h-64">
            <div className="p-4 h-full flex flex-col">
              <h3 className="text-lg font-black text-gray-900 mb-3 flex items-center gap-2">
                <Clock className="w-5 h-5 text-[#C93831]" />
                예약 내역
              </h3>

              <div
                className="flex-1 overflow-auto pr-2"
                style={{
                  scrollbarWidth: "thin",
                  scrollbarColor: "#C93831 #f0f0f0",
                }}
              >
                <div className="space-y-2">
                  {appointments.map((apt) => (
                    <div
                      key={apt.id}
                      className={`p-3 rounded-xl ${
                        apt.status === "예정" ? "bg-white/80" : "bg-gray-100/50"
                      }`}
                    >
                      <div className="flex items-start justify-between mb-1">
                        <div>
                          <div className="font-bold text-gray-900 text-sm">
                            {apt.type}
                          </div>
                          <div className="text-xs text-gray-600">
                            {apt.doctor} 원장
                          </div>
                        </div>
                        <Badge
                          className={`${
                            apt.status === "예정"
                              ? "bg-green-500"
                              : "bg-gray-500"
                          } text-white font-bold border-0 text-xs`}
                        >
                          {apt.status}
                        </Badge>
                      </div>
                      <div className="text-xs text-gray-600 font-medium mb-2">
                        {apt.date} {apt.time}
                      </div>
                      <div className="flex gap-2">
                        {apt.hasChat && (
                          <Button
                            onClick={() => setShowChat(true)}
                            variant="outline"
                            size="sm"
                            className="flex-1 rounded-lg text-xs border-blue-300 text-blue-600 hover:bg-blue-50"
                          >
                            <MessageCircle className="w-3 h-3 mr-1" />
                            채팅
                          </Button>
                        )}
                        {apt.status === "예정" && (
                          <Button
                            variant="outline"
                            size="sm"
                            className="flex-1 rounded-lg text-xs border-red-300 text-red-600 hover:bg-red-50"
                          >
                            <XCircle className="w-3 h-3 mr-1" />
                            취소
                          </Button>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          </Card>

          {/* Prescriptions */}
          <Card className="col-span-4 backdrop-blur-2xl bg-white/60 border border-gray-200 shadow-xl overflow-hidden h-48">
            <div className="p-4 h-full flex flex-col">
              <h3 className="text-lg font-black text-gray-900 mb-3 flex items-center gap-2">
                <FileText className="w-5 h-5 text-[#C93831]" />
                처방전
              </h3>

              <div
                className="flex-1 overflow-auto pr-2"
                style={{
                  scrollbarWidth: "thin",
                  scrollbarColor: "#C93831 #f0f0f0",
                }}
              >
                <div className="grid grid-cols-4 gap-3">
                  {prescriptions.map((pres) => (
                    <div
                      key={pres.id}
                      className="p-3 rounded-xl bg-white/80 border border-gray-200"
                    >
                      <div className="font-bold text-gray-900 mb-1 text-sm">
                        {pres.name}
                      </div>
                      <div className="text-xs text-gray-600 mb-1">
                        {pres.doctor} 원장
                      </div>
                      <div className="text-xs text-gray-500 mb-2">
                        {pres.date}
                      </div>
                      <Button
                        size="sm"
                        variant="outline"
                        className="w-full rounded-lg text-xs"
                        onClick={() => setSelectedPrescription(pres)}
                      >
                        상세보기
                      </Button>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          </Card>
        </div>
      </div>
    </div>
  );
}
