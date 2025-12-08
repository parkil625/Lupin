/**
 * Medical.tsx
 *
 * 진료 관리 페이지 컴포넌트 (2단 레이아웃)
 * - 좌측: 예약 내역 및 처방전 조회
 * - 우측: 실시간 채팅 또는 진료 예약
 */

import { useState, useEffect, useCallback, useRef } from "react";
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
import { Clock, FileText, XCircle, Send } from "lucide-react"; // CalendarIcon 대신 Calendar 사용
import { Prescription } from "@/types/dashboard.types";
import { useWebSocket } from "@/hooks/useWebSocket";
import { chatApi, ChatMessageResponse } from "@/api/chatApi";
import { appointmentApi } from "@/api/appointmentApi";
import { toast } from "sonner";

interface MedicalProps {
  setShowAppointment: (show: boolean) => void;
  setShowChat: (show: boolean) => void;
  setSelectedPrescription: (prescription: Prescription | null) => void;
}

export default function Medical({
  setSelectedPrescription,
}: // 원격의 props는 제거 (로컬의 인라인 예약 기능으로 대체)
MedicalProps) {
  // 현재 로그인한 환자 정보 (localStorage에서 가져오기)
  const currentUserId = parseInt(localStorage.getItem("userId") || "1");
  const currentPatientId = currentUserId; // 환자의 경우 userId와 patientId가 동일
  const doctorId = 21; // doctor01의 ID (테스트용)

  const [chatMessage, setChatMessage] = useState("");
  const [messages, setMessages] = useState<ChatMessageResponse[]>([]);

  // -------------------------------------------------------------------------
  // [HEAD] 예약 관련 상태 및 로직 (인라인 예약 기능을 위해 복구)
  // -------------------------------------------------------------------------

  const [activeAppointment, setActiveAppointment] = useState<{
    id: number;
    doctorId: number;
    doctorName: string;
    type: string;
  } | null>(null);
  const [isChatEnded, setIsChatEnded] = useState(false);

  // 예약 화면 상태 (채팅이 없으면 기본으로 예약 화면 표시)
  const [, setShowAppointmentView] = useState(true);
  const [selectedDepartment, setSelectedDepartment] = useState("");
  const [selectedDate, setSelectedDate] = useState<Date | undefined>(undefined);
  const [selectedTime, setSelectedTime] = useState("");

  // 한국 공휴일 (2024년 기준)
  const holidays = [
    new Date(2024, 0, 1), // 신정
    new Date(2024, 1, 9), // 설날 연휴
    new Date(2024, 1, 10), // 설날
    new Date(2024, 1, 11), // 설날 연휴
    new Date(2024, 2, 1), // 삼일절
    new Date(2024, 4, 5), // 어린이날
    new Date(2024, 4, 15), // 부처님 오신 날
    new Date(2024, 5, 6), // 현충일
    new Date(2024, 7, 15), // 광복절
    new Date(2024, 8, 16), // 추석 연휴
    new Date(2024, 8, 17), // 추석
    new Date(2024, 8, 18), // 추석 연휴
    new Date(2024, 9, 3), // 개천절
    new Date(2024, 9, 9), // 한글날
    new Date(2024, 11, 25), // 크리스마스
    // 2025년
    new Date(2025, 0, 1), // 신정
    new Date(2025, 0, 28), // 설날 연휴
    new Date(2025, 0, 29), // 설날
    new Date(2025, 0, 30), // 설날 연휴
    new Date(2025, 2, 1), // 삼일절
    new Date(2025, 4, 5), // 어린이날
    new Date(2025, 4, 5), // 부처님 오신 날
    new Date(2025, 5, 6), // 현충일
    new Date(2025, 7, 15), // 광복절
    new Date(2025, 9, 3), // 개천절
    new Date(2025, 9, 5), // 추석 연휴
    new Date(2025, 9, 6), // 추석
    new Date(2025, 9, 7), // 추석 연휴
    new Date(2025, 9, 9), // 한글날
    new Date(2025, 11, 25), // 크리스마스
  ];

  // 공휴일인지 확인
  const isHoliday = (date: Date) => {
    return holidays.some(
      (holiday) =>
        holiday.getFullYear() === date.getFullYear() &&
        holiday.getMonth() === date.getMonth() &&
        holiday.getDate() === date.getDate()
    );
  };

  // 지난 날짜인지 확인
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

  // 오늘인지 확인
  const isToday = (date: Date | undefined) => {
    if (!date) return false;
    return (
      date.getFullYear() === now.getFullYear() &&
      date.getMonth() === now.getMonth() &&
      date.getDate() === now.getDate()
    );
  };

  // 시간이 지났는지 확인 (오늘인 경우만)
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

  // 예약 가능 시간
  const availableTimes = [
    "09:00",
    "10:00",
    "11:00",
    "14:00",
    "15:00",
    "16:00",
    "17:00",
  ];
  const bookedTimes = ["10:00", "15:00"]; // 이미 예약된 시간 (예시)

  // 채팅방이 활성화되어야 하는지 확인
  const hasActiveChat = activeAppointment !== null && !isChatEnded;

  // 예약 확인 핸들러
  const handleConfirmAppointment = async () => {
    if (!selectedDepartment || !selectedDate || !selectedTime) return;

    const departmentNames: Record<string, string> = {
      internal: "내과",
      surgery: "외과",
      psychiatry: "신경정신과",
      dermatology: "피부과",
    };

    try {
      // 날짜 + 시간 조합 (ISO 8601 형식)
      const [hours, minutes] = selectedTime.split(":").map(Number);
      const appointmentDateTime = new Date(selectedDate);
      appointmentDateTime.setHours(hours, minutes, 0, 0);

      // 백엔드 API 호출
      const appointmentId = await appointmentApi.createAppointment({
        patientId: currentPatientId,
        doctorId: doctorId,
        date: appointmentDateTime.toISOString(),
      });

      console.log("✅ 예약 생성 성공:", appointmentId);

      setActiveAppointment({
        id: appointmentId,
        doctorId: doctorId,
        doctorName: "김민준",
        type: `${departmentNames[selectedDepartment]} 상담`,
      });
      setIsChatEnded(false);
      setShowAppointmentView(false);

      toast.success(
        `${selectedDate.toLocaleDateString(
          "ko-KR"
        )} ${selectedTime} 예약이 완료되었습니다`
      );

      // 상태 초기화
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

  // 스크롤 제어용 Ref
  const messagesEndRef = useRef<HTMLDivElement>(null);

  // WebSocket 연결 (예약 ID 기반)
  const roomId = activeAppointment
    ? `appointment_${activeAppointment.id}`
    : `temp_${currentPatientId}_${doctorId}`;

  // 메시지 수신 콜백 (HEAD의 로직 유지: 본인이 보낸 메시지는 알림 표시 안함)
  const handleMessageReceived = useCallback(
    (message: ChatMessageResponse) => {
      setMessages((prev) => [...prev, message]);
      // 본인이 보낸 메시지는 알림 표시 안함
      if (message.senderId !== currentUserId) {
        toast.success("새 메시지가 도착했습니다");
      }
    },
    [currentUserId]
  );

  const {
    isConnected,
    sendMessage: sendWebSocketMessage,
  } = useWebSocket({
    roomId,
    userId: currentUserId,
    onMessageReceived: handleMessageReceived,
  });

  // 자동 스크롤
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  // 메시지 로드
  useEffect(() => {
    const loadMessages = async () => {
      try {
        const loadedMessages = await chatApi.getAllMessagesByRoomId(roomId);
        setMessages(loadedMessages);

        // 메시지 읽음 처리 (REST API 사용)
        if (isConnected) {
          await chatApi.markAsRead(roomId, currentUserId);
        }
      } catch (error) {
        console.error("메시지 로드 실패:", error);
      }
    };

    loadMessages();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [roomId, isConnected, currentUserId]);

  const handleSendMessage = () => {
    if (!chatMessage.trim()) return;

    // WebSocket으로 메시지 전송
    sendWebSocketMessage(
      chatMessage,
      currentUserId,
      currentPatientId,
      doctorId
    );

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
            {/* 예약 내역 - 고정 높이 350px */}
            <Card className="backdrop-blur-2xl bg-white/60 border border-gray-200 shadow-xl h-[350px] flex flex-col overflow-hidden">
              <div className="p-4 flex-shrink-0">
                <h3 className="text-lg font-black text-gray-900 mb-3 flex items-center gap-2">
                  <Clock className="w-5 h-5 text-[#C93831]" />
                  예약 내역
                </h3>
              </div>
              <div className="flex-1 overflow-y-auto px-4 pb-4">
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
                      {apt.status === "예정" && (
                        <Button
                          variant="outline"
                          size="sm"
                          className="w-full rounded-lg text-xs border-red-300 text-red-600 hover:bg-red-50"
                        >
                          <XCircle className="w-3 h-3 mr-1" />
                          취소
                        </Button>
                      )}
                    </div>
                  ))}
                </div>
              </div>
            </Card>

            {/* 처방전 - 고정 높이 350px */}
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
              {/* [HEAD]의 인라인 예약/채팅 로직을 유지 */}
              {hasActiveChat ? (
                <>
                  <div className="flex items-center justify-between pb-4 border-b border-gray-200 mb-4">
                    <div className="flex items-center gap-3">
                      <Avatar className="w-10 h-10">
                        <AvatarFallback className="bg-gradient-to-br from-blue-600 to-blue-800 text-white font-black">
                          김
                        </AvatarFallback>
                      </Avatar>
                      <div>
                        <div className="font-bold text-gray-900">김의사</div>
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
                        disabled={(date) =>
                          isPastDate(date) || isHoliday(date)
                        }
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
