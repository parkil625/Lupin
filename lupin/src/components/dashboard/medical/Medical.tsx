/**
 * Medical.tsx
 *
 * 진료 관리 페이지 컴포넌트 (2단 레이아웃)
 * - 좌측: 예약 내역 및 처방전 조회
 * - 우측: 실시간 채팅 또는 진료 예약
 */

import React, { useState, useEffect, useCallback, useRef } from "react";
import { Card } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Calendar } from "@/components/ui/calendar";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Clock, FileText, XCircle, Send } from "lucide-react";
import { Prescription } from "@/types/dashboard.types";
import { useWebSocket } from "@/hooks/useWebSocket";
import { chatApi, ChatMessageResponse } from "@/api/chatApi";
import { appointmentApi, AppointmentResponse } from "@/api/appointmentApi";
import { userApi } from "@/api/userApi";
import { toast } from "sonner";

interface MedicalProps {
  setShowAppointment: (show: boolean) => void;
  setShowChat: (show: boolean) => void;
  setSelectedPrescription: (prescription: Prescription | null) => void;
}

export default function Medical({ setSelectedPrescription }: MedicalProps) {
  // 현재 로그인한 환자 정보
  const currentUserId = parseInt(localStorage.getItem("userId") || "1");
  const currentPatientId = currentUserId;

  const [chatMessage, setChatMessage] = useState("");
  const [messages, setMessages] = useState<ChatMessageResponse[]>([]);
  const [appointments, setAppointments] = useState<AppointmentResponse[]>([]);

  // -------------------------------------------------------------------------
  // [HEAD] 예약 관련 상태 및 로직
  // -------------------------------------------------------------------------

  const [activeAppointment, setActiveAppointment] = useState<{
    id: number;
    doctorId: number;
    doctorName: string;
    type: string;
  } | null>(null);
  const [isChatEnded, setIsChatEnded] = useState(false);

  const [, setShowAppointmentView] = useState(true);
  const [selectedDepartment, setSelectedDepartment] = useState("");
  const [selectedDate, setSelectedDate] = useState<Date | undefined>(undefined);
  const [selectedTime, setSelectedTime] = useState("");

  // 한국 공휴일 (2024-2025)
  const holidays = [
    new Date(2024, 0, 1),
    new Date(2024, 1, 9),
    new Date(2024, 1, 10),
    new Date(2024, 1, 11),
    new Date(2024, 2, 1),
    new Date(2024, 4, 5),
    new Date(2024, 4, 15),
    new Date(2024, 5, 6),
    new Date(2024, 7, 15),
    new Date(2024, 8, 16),
    new Date(2024, 8, 17),
    new Date(2024, 8, 18),
    new Date(2024, 9, 3),
    new Date(2024, 9, 9),
    new Date(2024, 11, 25),
    new Date(2025, 0, 1),
    new Date(2025, 0, 28),
    new Date(2025, 0, 29),
    new Date(2025, 0, 30),
    new Date(2025, 2, 1),
    new Date(2025, 4, 5),
    new Date(2025, 4, 6), // 대체공휴일 고려
    new Date(2025, 5, 6),
    new Date(2025, 7, 15),
    new Date(2025, 9, 3),
    new Date(2025, 9, 5),
    new Date(2025, 9, 6),
    new Date(2025, 9, 7),
    new Date(2025, 9, 9),
    new Date(2025, 11, 25),
  ];

  const isHoliday = (date: Date) => {
    return holidays.some(
      (holiday) =>
        holiday.getFullYear() === date.getFullYear() &&
        holiday.getMonth() === date.getMonth() &&
        holiday.getDate() === date.getDate()
    );
  };

  const now = new Date();
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());

  const isPastDate = (date: Date) => {
    const dateOnly = new Date(
      date.getFullYear(),
      date.getMonth(),
      date.getDate()
    );
    return dateOnly < today;
  };

  const isToday = (date: Date | undefined) => {
    if (!date) return false;
    return (
      date.getFullYear() === now.getFullYear() &&
      date.getMonth() === now.getMonth() &&
      date.getDate() === now.getDate()
    );
  };

  const isPastTime = (time: string) => {
    if (!selectedDate || !isToday(selectedDate)) return false;
    const [hours, minutes] = time.split(":").map(Number);
    const currentHours = now.getHours();
    const currentMinutes = now.getMinutes();
    return (
      hours < currentHours ||
      (hours === currentHours && minutes <= currentMinutes)
    );
  };

  const availableTimes = [
    "09:00",
    "10:00",
    "11:00",
    "14:00",
    "15:00",
    "16:00",
    "17:00",
  ];
  const bookedTimes: string[] = [];

  const hasActiveChat = activeAppointment !== null && !isChatEnded;

  const handleAppointmentClick = (appointment: AppointmentResponse) => {
    if (appointment.status === "SCHEDULED") {
      setActiveAppointment({
        id: appointment.id,
        doctorId: appointment.doctorId,
        doctorName: appointment.doctorName,
        type: "진료 상담",
      });
      setIsChatEnded(false);
      setShowAppointmentView(false);
    }
  };

  const handleCancelAppointment = async (
    appointmentId: number,
    e: React.MouseEvent
  ) => {
    e.stopPropagation();
    if (!confirm("예약을 취소하시겠습니까?")) return;

    try {
      await appointmentApi.cancelAppointment(appointmentId);
      toast.success("예약이 취소되었습니다.");
      const data = await appointmentApi.getPatientAppointments(
        currentPatientId
      );
      setAppointments(data);

      if (activeAppointment?.id === appointmentId) {
        setActiveAppointment(null);
        setIsChatEnded(true);
      }
    } catch (error) {
      console.error("예약 취소 실패:", error);
      toast.error("예약 취소에 실패했습니다.");
    }
  };

  // -------------------------------------------------------------------------
  // [수정됨] 예약 확인 핸들러: 한국 시간(KST) 문자열 전송
  // -------------------------------------------------------------------------
  const handleConfirmAppointment = async () => {
    if (!selectedDepartment || !selectedDate || !selectedTime) return;

    const departmentNames: Record<string, string> = {
      internal: "내과",
      surgery: "외과",
      psychiatry: "신경정신과",
      dermatology: "피부과",
    };

    const departmentKoreanName = departmentNames[selectedDepartment];

    let selectedDoctor: { id: number; name: string; department: string };
    try {
      const doctors = await userApi.getDoctorsByDepartment(
        departmentKoreanName
      );

      if (doctors.length === 0) {
        toast.error("해당 진료과에 배정된 의사가 없습니다.");
        return;
      }
      selectedDoctor = doctors[0];
    } catch (error) {
      console.error("의사 조회 실패:", error);
      toast.error("의사 정보를 불러오는데 실패했습니다.");
      return;
    }

    try {
      // [수정 포인트] toISOString()을 쓰면 UTC로 변환되므로 시간이 바뀝니다.
      // 선택한 날짜와 시간을 그대로 조합하여 전송합니다.
      const [hours, minutes] = selectedTime.split(":").map(Number);

      const year = selectedDate.getFullYear();
      const month = String(selectedDate.getMonth() + 1).padStart(2, "0");
      const day = String(selectedDate.getDate()).padStart(2, "0");
      const hoursStr = String(hours).padStart(2, "0");
      const minutesStr = String(minutes).padStart(2, "0");

      // 결과: "2024-12-10T14:00:00" (Local Time)
      // 백엔드의 LocalDateTime이 이 문자열을 받으면 시간대 변환 없이 그대로 14:00으로 저장합니다.
      const localISOTime = `${year}-${month}-${day}T${hoursStr}:${minutesStr}:00`;

      const appointmentId = await appointmentApi.createAppointment({
        patientId: currentPatientId,
        doctorId: selectedDoctor.id,
        date: localISOTime,
      });

      console.log("✅ 예약 생성 성공 (Local Time):", localISOTime);

      setActiveAppointment({
        id: appointmentId,
        doctorId: selectedDoctor.id,
        doctorName: selectedDoctor.name,
        type: `${departmentKoreanName} 상담`,
      });
      setIsChatEnded(false);
      setShowAppointmentView(false);

      toast.success(
        `${selectedDate.toLocaleDateString(
          "ko-KR"
        )} ${selectedTime} ${departmentKoreanName} 예약이 완료되었습니다`
      );

      const data = await appointmentApi.getPatientAppointments(
        currentPatientId
      );
      setAppointments(data);

      setSelectedDepartment("");
      setSelectedDate(undefined);
      setSelectedTime("");
    } catch (error) {
      console.error("❌ 예약 생성 실패:", error);
      toast.error("예약 생성에 실패했습니다. 다시 시도해주세요.");
    }
  };

  // -------------------------------------------------------------------------
  // [공통] 웹소켓 및 메시지 로직
  // -------------------------------------------------------------------------

  const messagesEndRef = useRef<HTMLDivElement>(null);
  const roomId = activeAppointment ? `appointment_${activeAppointment.id}` : "";

  const handleMessageReceived = useCallback(
    (message: ChatMessageResponse) => {
      setMessages((prev) => [...prev, message]);
      if (message.senderId !== currentUserId) {
        toast.success("새 메시지가 도착했습니다");
      }
    },
    [currentUserId]
  );

  const { isConnected, sendMessage: sendWebSocketMessage } = useWebSocket({
    roomId,
    userId: currentUserId,
    onMessageReceived: handleMessageReceived,
  });

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  useEffect(() => {
    const loadMessages = async () => {
      try {
        const loadedMessages = await chatApi.getAllMessagesByRoomId(roomId);
        setMessages(loadedMessages);
        if (isConnected) {
          await chatApi.markAsRead(roomId, currentUserId);
        }
      } catch (error) {
        console.error("메시지 로드 실패:", error);
      }
    };
    loadMessages();
  }, [roomId, isConnected, currentUserId]);

  const handleSendMessage = () => {
    if (!chatMessage.trim()) return;
    sendWebSocketMessage(chatMessage, currentUserId);
    setChatMessage("");
  };

  // -------------------------------------------------------------------------
  // [공통] 목업 데이터
  // -------------------------------------------------------------------------

  const prescriptions: Prescription[] = [
    {
      id: 1,
      name: "감기약 처방",
      date: "11월 10일",
      doctor: "이의사",
      medicines: ["타이레놀 500mg", "콧물약", "기침약"],
      diagnosis: "급성 상기도 감염",
      instructions: "하루 3회, 식후 30분에 복용하세요.",
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
      doctor: "김준호 의사",
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

  useEffect(() => {
    const loadAppointments = async () => {
      try {
        const data = await appointmentApi.getPatientAppointments(
          currentPatientId
        );
        setAppointments(data);
      } catch (error) {
        console.error("예약 목록 로드 실패:", error);
        toast.error("예약 목록을 불러오는데 실패했습니다.");
      }
    };
    loadAppointments();
  }, [currentPatientId]);

  // -------------------------------------------------------------------------
  // [렌더링]
  // -------------------------------------------------------------------------

  return (
    <div className="h-full overflow-auto p-8">
      <div className="max-w-[1200px] mx-auto">
        <div className="mb-6">
          <h1 className="text-5xl font-black text-gray-900 mb-2">
            비대면 진료
          </h1>
          <p className="text-gray-700 font-medium text-lg">
            전문 의료진과 상담하세요
          </p>
        </div>

        <div className="h-[calc(100vh-200px)] flex gap-4">
          {/* 좌측: 예약 내역 및 처방전 */}
          <div className="w-96 flex flex-col gap-4">
            <Card className="backdrop-blur-2xl bg-white/60 border border-gray-200 shadow-xl h-[350px] flex flex-col overflow-hidden">
              <div className="p-4 flex-shrink-0">
                <h3 className="text-lg font-black text-gray-900 mb-3 flex items-center gap-2">
                  <Clock className="w-5 h-5 text-[#C93831]" />
                  예약 내역
                </h3>
              </div>
              <div className="flex-1 overflow-y-auto px-4 pb-4">
                <div className="space-y-2">
                  {appointments.map((apt) => {
                    const appointmentDate = new Date(apt.date);
                    const formattedDate = appointmentDate.toLocaleDateString(
                      "ko-KR",
                      { month: "long", day: "numeric" }
                    );
                    const formattedTime = appointmentDate.toLocaleTimeString(
                      "ko-KR",
                      { hour: "numeric", minute: "2-digit" }
                    );

                    const statusMap: Record<string, string> = {
                      SCHEDULED: "예정",
                      IN_PROGRESS: "진행중",
                      COMPLETED: "완료",
                      CANCELLED: "취소됨",
                    };
                    const displayStatus = statusMap[apt.status] || apt.status;
                    const isScheduled = apt.status === "SCHEDULED";

                    // [수정 포인트] 백엔드에서 받아온 department 값을 표시합니다.
                    // 타입 정의 파일(api/appointmentApi.ts)에 department 필드가 추가되어야 합니다.
                    const departmentDisplay = apt.department || "진료";

                    return (
                      <div
                        key={apt.id}
                        onClick={() => handleAppointmentClick(apt)}
                        className={`p-3 rounded-xl ${
                          isScheduled
                            ? "bg-white/80 hover:bg-white cursor-pointer"
                            : "bg-gray-100/50"
                        }`}
                      >
                        <div className="flex items-start justify-between mb-1">
                          <div>
                            <div className="font-bold text-gray-900 text-sm">
                              {apt.doctorName} 의사
                            </div>
                            {/* [수정 포인트] 진료 과목 표시 */}
                            <div className="text-xs text-gray-600 font-medium text-[#C93831]">
                              {departmentDisplay} 예약
                            </div>
                          </div>
                          <Badge
                            className={`${
                              isScheduled ? "bg-green-500" : "bg-gray-500"
                            } text-white font-bold border-0 text-xs`}
                          >
                            {displayStatus}
                          </Badge>
                        </div>
                        <div className="text-xs text-gray-600 font-medium mb-2">
                          {formattedDate} {formattedTime}
                        </div>
                        {isScheduled && (
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={(e) => handleCancelAppointment(apt.id, e)}
                            className="w-full rounded-lg text-xs border-red-300 text-red-600 hover:bg-red-50"
                          >
                            <XCircle className="w-3 h-3 mr-1" />
                            취소
                          </Button>
                        )}
                      </div>
                    );
                  })}
                </div>
              </div>
            </Card>

            {/* 처방전 섹션 (기존 코드 유지) */}
            <Card className="backdrop-blur-2xl bg-white/60 border border-gray-200 shadow-xl h-[350px] flex flex-col overflow-hidden">
              <div className="p-4 flex-shrink-0">
                <h3 className="text-lg font-black text-gray-900 mb-3 flex items-center gap-2">
                  <FileText className="w-5 h-5 text-[#C93831]" />
                  처방전
                </h3>
              </div>
              <div className="flex-1 overflow-y-auto px-4 pb-4">
                <div className="space-y-2">
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
            </Card>
          </div>

          {/* 우측: 채팅 / 예약 화면 */}
          <Card className="flex-1 backdrop-blur-2xl bg-white/60 border border-gray-200 shadow-2xl">
            <div className="h-full flex flex-col p-6">
              {hasActiveChat ? (
                <>
                  <div className="flex items-center justify-between pb-4 border-b border-gray-200 mb-4">
                    <div className="flex items-center gap-3">
                      <Avatar className="w-10 h-10">
                        <AvatarFallback className="bg-gradient-to-br from-blue-600 to-blue-800 text-white font-black">
                          {activeAppointment?.doctorName?.charAt(0) || "의"}
                        </AvatarFallback>
                      </Avatar>
                      <div>
                        <div className="font-bold text-gray-900">
                          {activeAppointment?.doctorName || "알 수 없음"} 의사
                        </div>
                        <div className="text-xs text-gray-600">온라인</div>
                      </div>
                    </div>
                    <div className="flex items-center gap-2 px-3 py-1 bg-green-100 rounded-full">
                      <div className="w-2 h-2 bg-green-500 rounded-full"></div>
                      <span className="text-xs font-bold text-green-700">
                        진료 중
                      </span>
                    </div>
                  </div>

                  <div className="flex-1 overflow-y-auto mb-4 pr-2">
                    <div className="space-y-4">
                      {messages.map((msg) => {
                        const isMine = msg.senderId === currentUserId;
                        const senderInitial = isMine
                          ? "김"
                          : msg.senderName.charAt(0);

                        return (
                          <div
                            key={msg.id}
                            className={`flex gap-3 ${
                              isMine ? "justify-end" : ""
                            }`}
                          >
                            {!isMine && (
                              <Avatar className="w-8 h-8">
                                <AvatarFallback className="bg-gradient-to-br from-blue-600 to-blue-800 text-white font-black text-xs">
                                  {senderInitial}
                                </AvatarFallback>
                              </Avatar>
                            )}
                            <div
                              className={`rounded-2xl p-3 max-w-md ${
                                isMine
                                  ? "bg-[#C93831] text-white"
                                  : "bg-gray-100"
                              }`}
                            >
                              {!isMine && (
                                <div className="font-bold text-xs text-gray-900 mb-1">
                                  {msg.senderName}
                                </div>
                              )}
                              <div className="text-sm">{msg.content}</div>
                              <div
                                className={`text-xs mt-1 ${
                                  isMine ? "text-white/80" : "text-gray-500"
                                }`}
                              >
                                {new Date(msg.sentAt).toLocaleTimeString(
                                  "ko-KR",
                                  {
                                    hour: "2-digit",
                                    minute: "2-digit",
                                  }
                                )}
                              </div>
                            </div>
                          </div>
                        );
                      })}
                      <div ref={messagesEndRef} />
                    </div>
                  </div>

                  <div className="flex gap-2">
                    <Input
                      placeholder="메시지 입력..."
                      className="rounded-xl"
                      value={chatMessage}
                      onChange={(e) => setChatMessage(e.target.value)}
                      onKeyPress={(e) => {
                        if (e.key === "Enter") {
                          handleSendMessage();
                        }
                      }}
                    />
                    <Button
                      onClick={handleSendMessage}
                      className="bg-gradient-to-r from-[#C93831] to-[#B02F28] text-white rounded-xl"
                    >
                      <Send className="w-4 h-4" />
                    </Button>
                  </div>
                </>
              ) : (
                // 인라인 예약 화면 (HEAD의 로직 유지)
                <div className="h-full overflow-y-auto flex flex-col items-center justify-center">
                  <div className="w-[320px]">
                    <h2 className="text-2xl font-black text-gray-900 mb-4 text-center">
                      진료 예약
                    </h2>

                    {/* 진료과 선택 */}
                    <div className="mb-4">
                      <Label className="text-base font-black mb-2 block">
                        진료과 선택
                      </Label>
                      <Select
                        value={selectedDepartment}
                        onValueChange={setSelectedDepartment}
                      >
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

                    {/* 날짜 선택 */}
                    <div className="mb-4">
                      <Label className="text-base font-black mb-2 block">
                        날짜 선택
                      </Label>
                      <Calendar
                        mode="single"
                        selected={selectedDate}
                        onSelect={setSelectedDate}
                        disabled={(date) => isPastDate(date) || isHoliday(date)}
                        modifiers={{
                          holiday: holidays,
                        }}
                        modifiersStyles={{
                          holiday: {
                            color: "#C93831",
                          },
                        }}
                        className="rounded-xl border"
                      />
                      <p className="text-xs text-gray-600 mt-2">
                        * 빨간색 날짜는 공휴일입니다 (선택 불가)
                      </p>
                    </div>

                    {/* 시간 선택 */}
                    {selectedDate && (
                      <div className="mb-4">
                        <Label className="text-base font-black mb-2 block">
                          시간 선택
                        </Label>
                        <div className="grid grid-cols-4 gap-2">
                          {availableTimes.map((time) => {
                            const isBooked = bookedTimes.includes(time);
                            const isPast = isPastTime(time);
                            const isDisabled = isBooked || isPast;
                            const isSelected = selectedTime === time;
                            return (
                              <Button
                                key={time}
                                variant={isSelected ? "default" : "outline"}
                                disabled={isDisabled}
                                onClick={() => setSelectedTime(time)}
                                className={`rounded-xl ${
                                  isSelected
                                    ? "bg-[#C93831] hover:bg-[#B02F28]"
                                    : ""
                                } ${isDisabled ? "opacity-50" : ""}`}
                              >
                                {time}
                                {isBooked && " (예약됨)"}
                                {isPast && !isBooked && " (마감)"}
                              </Button>
                            );
                          })}
                        </div>
                      </div>
                    )}

                    {/* 예약하기 버튼 */}
                    <Button
                      disabled={
                        !selectedDepartment || !selectedDate || !selectedTime
                      }
                      className="w-full bg-gradient-to-r from-[#C93831] to-[#B02F28] text-white font-bold rounded-xl h-12"
                      onClick={handleConfirmAppointment}
                    >
                      예약하기
                    </Button>
                  </div>
                </div>
              )}
            </div>
          </Card>
        </div>
      </div>
    </div>
  );
}
