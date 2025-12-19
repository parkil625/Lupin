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
import { Clock, FileText, XCircle, Send } from "lucide-react"; // CalendarIcon 대신 Calendar 사용
import { Prescription } from "@/types/dashboard.types";
import { useWebSocket } from "@/hooks/useWebSocket";
import { chatApi, ChatMessageResponse } from "@/api/chatApi";
import { appointmentApi, AppointmentResponse } from "@/api/appointmentApi";
import { userApi } from "@/api/userApi";
import { toast } from "sonner";

interface MedicalProps {
  setShowChat: (show: boolean) => void;
  setSelectedPrescription: (prescription: Prescription | null) => void;
}

export default function Medical({
  setSelectedPrescription,
}: MedicalProps) {
  // 현재 로그인한 환자 정보 (localStorage에서 가져오기)
  const currentUserId = parseInt(localStorage.getItem("userId") || "1");
  const currentPatientId = currentUserId; // 환자의 경우 userId와 patientId가 동일

  const [chatMessage, setChatMessage] = useState("");
  const [messages, setMessages] = useState<ChatMessageResponse[]>([]);
  const [appointments, setAppointments] = useState<AppointmentResponse[]>([]);

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
  const [viewState, setViewState] = useState<"FORM" | "SUCCESS">("FORM");
  const [selectedDepartment, setSelectedDepartment] = useState("");
  const [selectedDate, setSelectedDate] = useState<Date | undefined>(undefined);
  const [selectedTime, setSelectedTime] = useState("");
  const [bookedTimes, setBookedTimes] = useState<string[]>([]);
  const [lastCreatedAppointment, setLastCreatedAppointment] = useState<{
    doctorName: string;
    departmentName: string;
    date: string;
    time: string;
  } | null>(null);

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

  // 채팅방이 활성화되어야 하는지 확인
  const hasActiveChat = activeAppointment !== null && !isChatEnded;

  // 선택된 진료과와 날짜가 변경될 때 예약된 시간 조회
  useEffect(() => {
    const fetchBookedTimes = async () => {
      if (!selectedDepartment || !selectedDate) {
        setBookedTimes([]);
        return;
      }

      try {
        // 진료과 한글 이름 매핑
        const departmentNames: Record<string, string> = {
          internal: "내과",
          surgery: "외과",
          psychiatry: "신경정신과",
          dermatology: "피부과",
        };

        const departmentKoreanName = departmentNames[selectedDepartment];

        // 의사 조회
        const doctors = await userApi.getDoctorsByDepartment(
          departmentKoreanName
        );

        if (doctors.length === 0) {
          setBookedTimes([]);
          return;
        }

        // 첫 번째 의사의 예약된 시간 조회
        const doctorId = doctors[0].id;
        // 로컬 날짜를 YYYY-MM-DD 형식으로 변환 (타임존 문제 방지)
        const year = selectedDate.getFullYear();
        const month = String(selectedDate.getMonth() + 1).padStart(2, "0");
        const day = String(selectedDate.getDate()).padStart(2, "0");
        const dateStr = `${year}-${month}-${day}`;

        const booked = await appointmentApi.getBookedTimes(doctorId, dateStr);
        setBookedTimes(booked);
      } catch (error) {
        console.error("예약된 시간 조회 실패:", error);
        setBookedTimes([]);
      }
    };

    fetchBookedTimes();
  }, [selectedDepartment, selectedDate]);

  // 예약 클릭 핸들러 - 채팅방 열기
  const handleAppointmentClick = async (appointment: AppointmentResponse) => {
    // SCHEDULED 상태인 경우에만 채팅방 열기 시도
    if (appointment.status === "SCHEDULED") {
      try {
        // 채팅 가능 여부 확인
        const available = await appointmentApi.isChatAvailable(appointment.id);

        if (!available) {
          const lockMessage = await appointmentApi.getChatLockMessage(appointment.id);
          toast.error(lockMessage);
          return;
        }

        setActiveAppointment({
          id: appointment.id,
          doctorId: appointment.doctorId,
          doctorName: appointment.doctorName,
          type: "진료 상담",
        });
        setIsChatEnded(false);
        setShowAppointmentView(false);
      } catch (error) {
        console.error("채팅 가능 여부 확인 실패:", error);
        toast.error("채팅 시작 중 오류가 발생했습니다.");
      }
    }
  };

  // 예약 취소 핸들러
  const handleCancelAppointment = async (
    appointmentId: number,
    e: React.MouseEvent
  ) => {
    e.stopPropagation(); // 이벤트 버블링 방지

    if (!confirm("예약을 취소하시겠습니까?")) {
      return;
    }

    try {
      await appointmentApi.cancelAppointment(appointmentId);
      toast.success("예약이 취소되었습니다.");

      // 예약 목록 다시 로드
      const data = await appointmentApi.getPatientAppointments(
        currentPatientId
      );
      setAppointments(data);

      // 현재 채팅 중인 예약이 취소된 경우 채팅방 닫기
      if (activeAppointment?.id === appointmentId) {
        setActiveAppointment(null);
        setIsChatEnded(true);
      }
    } catch (error) {
      console.error("예약 취소 실패:", error);
      toast.error("예약 취소에 실패했습니다.");
    }
  };

  // 예약 확인 핸들러
  const handleConfirmAppointment = async () => {
    if (!selectedDepartment || !selectedDate || !selectedTime) return;

    // 진료과 한글 이름 매핑
    const departmentNames: Record<string, string> = {
      internal: "내과",
      surgery: "외과",
      psychiatry: "신경정신과",
      dermatology: "피부과",
    };

    const departmentKoreanName = departmentNames[selectedDepartment];

    // 의사 조회
    let selectedDoctor: { id: number; name: string; department: string };
    try {
      // API를 통해 진료과별 의사 조회 (한글 진료과명 사용)
      const doctors = await userApi.getDoctorsByDepartment(
        departmentKoreanName
      );

      if (doctors.length === 0) {
        toast.error("해당 진료과에 배정된 의사가 없습니다.");
        return;
      }

      // 첫 번째 의사 선택
      selectedDoctor = doctors[0];
    } catch (error) {
      console.error("의사 조회 실패:", error);
      toast.error("의사 정보를 불러오는데 실패했습니다.");
      return;
    }

    try {
      // 날짜 + 시간 조합 (로컬 시간 유지)
      const [hours, minutes] = selectedTime.split(":").map(Number);
      const year = selectedDate.getFullYear();
      const month = String(selectedDate.getMonth() + 1).padStart(2, "0");
      const day = String(selectedDate.getDate()).padStart(2, "0");
      const hoursStr = String(hours).padStart(2, "0");
      const minutesStr = String(minutes).padStart(2, "0");

      // ISO 8601 형식이지만 타임존 정보 없이 로컬 시간으로 전송
      const dateTimeStr = `${year}-${month}-${day}T${hoursStr}:${minutesStr}:00`;

      // 백엔드 API 호출
      const appointmentId = await appointmentApi.createAppointment({
        patientId: currentPatientId,
        doctorId: selectedDoctor.id,
        date: dateTimeStr,
      });

      console.log("✅ 예약 생성 성공:", appointmentId);

      // 예약 정보 저장 (성공 화면에 표시용)
      setLastCreatedAppointment({
        doctorName: selectedDoctor.name,
        departmentName: departmentKoreanName,
        date: selectedDate.toLocaleDateString("ko-KR"),
        time: selectedTime,
      });

      // 선택된 시간을 즉시 bookedTimes에 추가 (UI 즉시 반영)
      setBookedTimes((prev) => [...prev, selectedTime]);
      setSelectedTime("");

      // 예약 목록 즉시 업데이트 (낙관적 업데이트)
      const newAppointment: AppointmentResponse = {
        id: appointmentId,
        patientId: currentPatientId,
        patientName: "",
        doctorId: selectedDoctor.id,
        doctorName: selectedDoctor.name,
        departmentName: departmentKoreanName,
        date: dateTimeStr,
        status: "SCHEDULED",
      };
      setAppointments((prev) => [newAppointment, ...prev]);

      // 성공 화면으로 전환
      setViewState("SUCCESS");

      // 약간의 딜레이 후 서버에서 최신 데이터 조회 (Redis 캐시 무효화 대기)
      setTimeout(async () => {
        try {
          // 로컬 날짜를 YYYY-MM-DD 형식으로 변환 (타임존 문제 방지)
          const year = selectedDate.getFullYear();
          const month = String(selectedDate.getMonth() + 1).padStart(2, "0");
          const day = String(selectedDate.getDate()).padStart(2, "0");
          const dateStr = `${year}-${month}-${day}`;
          const updatedBookedTimes = await appointmentApi.getBookedTimes(
            selectedDoctor.id,
            dateStr
          );
          setBookedTimes(updatedBookedTimes);

          // 예약 목록 다시 로드 (서버에서 최신 데이터)
          const data = await appointmentApi.getPatientAppointments(
            currentPatientId
          );
          setAppointments(data);
        } catch (error) {
          console.error("예약 목록 갱신 실패:", error);
        }
      }, 500);
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

  // WebSocket 연결 (예약이 있을 때만)
  const roomId = activeAppointment ? `appointment_${activeAppointment.id}` : "";

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

  const { isConnected, sendMessage: sendWebSocketMessage } = useWebSocket({
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
    // roomId가 없으면 메시지 로드하지 않음
    if (!roomId) {
      setMessages([]);
      return;
    }

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
  }, [roomId, isConnected, currentUserId]);

  const handleSendMessage = () => {
    if (!chatMessage.trim()) return;

    // WebSocket으로 메시지 전송
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

  // 예약 목록 로드
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
                  {appointments.map((apt) => {
                    const appointmentDate = new Date(apt.date);
                    const formattedDate = appointmentDate.toLocaleDateString(
                      "ko-KR",
                      {
                        month: "long",
                        day: "numeric",
                      }
                    );
                    const formattedTime = appointmentDate.toLocaleTimeString(
                      "ko-KR",
                      {
                        hour: "numeric",
                        minute: "2-digit",
                      }
                    );

                    const statusMap = {
                      SCHEDULED: "예정",
                      IN_PROGRESS: "진행중",
                      COMPLETED: "완료",
                      CANCELLED: "취소됨",
                    };
                    const displayStatus = statusMap[apt.status] || apt.status;
                    const isScheduled = apt.status === "SCHEDULED";

                    return (
                      <div
                        key={apt.id}
                        onClick={() => handleAppointmentClick(apt)}
                        className={`p-3 rounded-xl ${
                          isScheduled
                            ? "bg-white/80 hover:bg-white cursor-pointer"
                            : "bg-gray-100/50 cursor-default"
                        }`}
                      >
                        <div className="flex items-start justify-between mb-1">
                          <div>
                            <div className="font-bold text-gray-900 text-sm">
                              {apt.doctorName} 의사
                            </div>
                            <div className="text-xs text-gray-600">
                              {apt.departmentName || "진료 예약"}
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
              ) : viewState === "SUCCESS" && lastCreatedAppointment ? (
                // 예약 성공 화면
                <div className="h-full overflow-y-auto flex flex-col items-center justify-center">
                  <div className="w-[400px] bg-white rounded-2xl shadow-lg p-8">
                    <div className="text-center mb-6">
                      <div className="w-16 h-16 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-4">
                        <svg
                          className="w-8 h-8 text-green-600"
                          fill="none"
                          stroke="currentColor"
                          viewBox="0 0 24 24"
                        >
                          <path
                            strokeLinecap="round"
                            strokeLinejoin="round"
                            strokeWidth={2}
                            d="M5 13l4 4L19 7"
                          />
                        </svg>
                      </div>
                      <h2 className="text-2xl font-black text-gray-900 mb-2">
                        예약이 완료되었습니다
                      </h2>
                      <p className="text-gray-600">
                        예약 정보를 확인해주세요
                      </p>
                    </div>

                    <div className="bg-gray-50 rounded-xl p-6 mb-6 space-y-4">
                      <div className="flex justify-between items-center">
                        <span className="text-gray-600 font-medium">담당 의사</span>
                        <span className="font-bold text-gray-900">
                          {lastCreatedAppointment.doctorName} 의사
                        </span>
                      </div>
                      <div className="flex justify-between items-center">
                        <span className="text-gray-600 font-medium">진료과</span>
                        <span className="font-bold text-gray-900">
                          {lastCreatedAppointment.departmentName}
                        </span>
                      </div>
                      <div className="flex justify-between items-center">
                        <span className="text-gray-600 font-medium">예약 날짜</span>
                        <span className="font-bold text-gray-900">
                          {lastCreatedAppointment.date}
                        </span>
                      </div>
                      <div className="flex justify-between items-center">
                        <span className="text-gray-600 font-medium">예약 시간</span>
                        <span className="font-bold text-[#C93831]">
                          {lastCreatedAppointment.time}
                        </span>
                      </div>
                    </div>

                    <Button
                      onClick={() => {
                        setViewState("FORM");
                        setLastCreatedAppointment(null);
                        setSelectedDepartment("");
                        setSelectedDate(undefined);
                        setSelectedTime("");
                      }}
                      className="w-full bg-gradient-to-r from-[#C93831] to-[#B02F28] text-white font-bold rounded-xl h-12"
                    >
                      확인
                    </Button>
                  </div>
                </div>
              ) : (
                // 인라인 예약 화면 (FORM 상태)
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
                        onSelect={(date) => {
                          setSelectedDate(date);
                          setSelectedTime(""); // 날짜 변경 시 시간 초기화
                        }}
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
                                onClick={() =>
                                  !isDisabled && setSelectedTime(time)
                                }
                                className={`rounded-xl ${
                                  isSelected
                                    ? "bg-[#C93831] hover:bg-[#B02F28]"
                                    : ""
                                } ${
                                  isDisabled
                                    ? "opacity-50 cursor-not-allowed bg-gray-100"
                                    : ""
                                }`}
                              >
                                {time}
                                {isBooked && (
                                  <span className="block text-[10px]">
                                    (예약됨)
                                  </span>
                                )}
                                {isPast && !isBooked && (
                                  <span className="block text-[10px]">
                                    (마감)
                                  </span>
                                )}
                              </Button>
                            );
                          })}
                        </div>
                      </div>
                    )}

                    {/* 예약하기 버튼 */}
                    <Button
                      disabled={
                        !selectedDepartment ||
                        !selectedDate ||
                        !selectedTime ||
                        bookedTimes.includes(selectedTime)
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
