/**
 * Medical.tsx
 *
 * 진료 관리 페이지 컴포넌트 (2단 레이아웃)
 * - 좌측: 예약 내역 및 처방전 조회
 * - 우측: 실시간 채팅 또는 진료 예약.
 */

import React, { useState, useEffect, useCallback, useRef } from "react";
import { Card } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
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
import { useWebSocket } from "@/hooks/useWebSocket";
import { chatApi, ChatMessageResponse } from "@/api/chatApi";
import { appointmentApi, AppointmentResponse } from "@/api/appointmentApi";
import { userApi } from "@/api/userApi";
import { prescriptionApi, PrescriptionResponse } from "@/api/prescriptionApi";
import { toast } from "sonner";
import UserHoverCard from "@/components/dashboard/shared/UserHoverCard";

interface MedicalProps {
  setSelectedPrescription: (prescription: PrescriptionResponse | null) => void;
}

export default function Medical({ setSelectedPrescription }: MedicalProps) {
  // 현재 로그인한 환자 정보 (localStorage에서 가져오기)
  const currentUserId = parseInt(localStorage.getItem("userId") || "1");
  const currentPatientId = currentUserId; // 환자의 경우 userId와 patientId가 동일

  const [chatMessage, setChatMessage] = useState("");
  const [messages, setMessages] = useState<ChatMessageResponse[]>([]);
  const [appointments, setAppointments] = useState<AppointmentResponse[]>([]);
  const [statusFilter, setStatusFilter] = useState<string>("ALL"); // 예약 상태 필터
  const [isLoadingAppointments, setIsLoadingAppointments] = useState(true);
  const [isLoadingPrescriptions, setIsLoadingPrescriptions] = useState(true);

  const [prescriptions, setPrescriptions] = useState<PrescriptionResponse[]>(
    []
  );

  // 의사 프로필 정보 저장 (doctorId -> { avatar, activeDays, department })
  const [doctorProfiles, setDoctorProfiles] = useState<
    Record<number, { avatar?: string; activeDays?: number; department?: string }>
  >({});

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
  const [viewState, setViewState] = useState<"FORM" | "SUCCESS" | "LIST">(
    "FORM"
  );
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
          thoracic_surgery: "흉부외과",
          obstetrics_gynecology: "산부인과",
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
    // SCHEDULED 또는 IN_PROGRESS 상태인 경우에만 채팅방 열기 시도
    if (
      appointment.status === "SCHEDULED" ||
      appointment.status === "IN_PROGRESS"
    ) {
      try {
        // 채팅 가능 여부 확인
        const available = await appointmentApi.isChatAvailable(appointment.id);
        if (!available) {
          const lockMessage = await appointmentApi.getChatLockMessage(
            appointment.id
          );
          toast.error(lockMessage);
          return;
        }

        // 의사 프로필 정보 로드 (없는 경우에만)
        if (!doctorProfiles[appointment.doctorId]) {
          try {
            const [userProfile, userStats] = await Promise.all([
              userApi.getUserById(appointment.doctorId),
              userApi.getUserStats(appointment.doctorId),
            ]);

            setDoctorProfiles((prev) => ({
              ...prev,
              [appointment.doctorId]: {
                avatar: userProfile.avatar,
                activeDays: userStats.activeDays,
                department: userProfile.department,
              },
            }));
          } catch (e) {
            console.error("의사 프로필 로드 실패", e);
          }
        }

        setActiveAppointment({
          id: appointment.id,
          doctorId: appointment.doctorId,
          doctorName: appointment.doctorName,
          type: "진료 상담",
        });
        setIsChatEnded(false);
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
        // 메시지는 유지 (취소된 예약의 채팅 기록도 서버에 남음)
      }
    } catch (error) {
      console.error("예약 취소 실패:", error);
      toast.error("예약 취소에 실패했습니다.");
    }
  };

  // 예약 확인 핸들러
  const handleConfirmAppointment = async () => {
    if (!selectedDepartment || !selectedDate || !selectedTime) return;

    // 예약 개수 제한 체크 (SCHEDULED 상태인 예약만 카운트)
    const activeAppointmentsCount = appointments.filter(
      (apt) => apt.status === "SCHEDULED" || apt.status === "IN_PROGRESS"
    ).length;

    if (activeAppointmentsCount >= 5) {
      toast.error(
        "예약은 최대 5개까지만 가능합니다. 기존 예약을 취소하거나 완료 후 진행해주세요."
      );
      return;
    }

    // 진료과 한글 이름 매핑
    const departmentNames: Record<string, string> = {
      internal: "내과",
      surgery: "외과",
      psychiatry: "신경정신과",
      dermatology: "피부과",
      thoracic_surgery: "흉부외과",
      obstetrics_gynecology: "산부인과",
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

          // 예약 목록 다시 로드 (서버에서 최신 데이터, viewState 변경 방지, 스켈레톤 표시 안 함)
          await loadAppointments(true, true);
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

  // 메시지 수신 콜백을 useRef로 관리하여 불필요한 재연결 방지
  const handleMessageReceivedRef = useRef((message: ChatMessageResponse) => {
    setMessages((prev) => [...prev, message]);
    // 본인이 보낸 메시지는 알림 표시 안함
    if (message.senderId !== currentUserId) {
      toast.success("새 메시지가 도착했습니다");
    }
  });

  // currentUserId 변경 시 ref 업데이트
  useEffect(() => {
    handleMessageReceivedRef.current = async (
      message: ChatMessageResponse & { type?: string; doctorName?: string }
    ) => {
      // 진료 종료 알림 처리
      if (message.type === "CONSULTATION_END") {
        const doctorName = message.doctorName || "담당 의사";

        // 1. 동기적으로 alert를 가장 먼저 표시 (블로킹)
        alert("진료가 종료되었습니다.\n예약 목록으로 이동합니다.");

        // 2. alert를 닫은 후 상태 초기화 (메시지는 유지 - 서버에서 관리)
        setActiveAppointment(null);
        setIsChatEnded(true);
        setViewState("LIST");

        // 3. 백그라운드에서 데이터 새로고침
        try {
          const appointmentsData = await appointmentApi.getPatientAppointments(
            currentPatientId
          );
          setAppointments(appointmentsData);

          const prescriptionsData =
            await prescriptionApi.getPatientPrescriptions(currentPatientId);
          setPrescriptions(prescriptionsData);

          toast.success(`${doctorName} 의사님의 진료가 완료되었습니다.`);
        } catch (error) {
          console.error("데이터 새로고침 실패:", error);
          toast.success(`${doctorName} 의사님의 진료가 완료되었습니다.`);
        }
        return;
      }

      // 일반 채팅 메시지 처리
      setMessages((prev) => [...prev, message]);
      if (message.senderId !== currentUserId) {
        toast.success("새 메시지가 도착했습니다");

        // 처방전 발급 메시지인 경우 처방전 목록 새로고침
        if (message.content.includes("처방전")) {
          try {
            // [중요] DB 저장 완료까지 0.5초 대기 후 조회 (Race Condition 방지)
            await new Promise((resolve) => setTimeout(resolve, 500));

            const data = await prescriptionApi.getPatientPrescriptions(
              currentPatientId
            );
            setPrescriptions(data);

            // 알림 강화
            toast.success("📋 처방전이 도착했습니다! 확인해보세요.");
          } catch {
            // 에러 무시 (조용히 처리)
            setPrescriptions([]);
          }
        }
      }
    };
  }, [currentUserId, currentPatientId]);

  // 메시지 수신 콜백 (안정적인 참조 유지)
  const handleMessageReceived = useCallback((message: ChatMessageResponse) => {
    handleMessageReceivedRef.current(message);
  }, []);

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
      return;
    }

    const loadMessages = async () => {
      try {
        const loadedMessages = await chatApi.getAllMessagesByRoomId(roomId);
        if (Array.isArray(loadedMessages)) {
          setMessages(loadedMessages);
        } else {
          setMessages([]);
        }

        // 메시지 읽음 처리 (REST API 사용)
        if (isConnected) {
          await chatApi.markAsRead(roomId, currentUserId);
        }
      } catch (error) {
        console.error("메시지 로드 실패:", error);
        setMessages([]);
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
  // [처방전] 상태 및 로직
  // -------------------------------------------------------------------------

  // 초기 마운트 여부 추적
  const isInitialMount = useRef(true);

  // 예약 목록 로드 함수
  const loadAppointments = useCallback(
    async (skipViewChange = false, skipLoading = false) => {
      try {
        if (!skipLoading) {
          setIsLoadingAppointments(true);
        }
        const data = await appointmentApi.getPatientAppointments(
          currentPatientId
        );
        setAppointments(data);

        // 예약이 있으면 즉시 LIST 뷰로 전환 (초기 마운트 시에만)
        if (
          !skipViewChange &&
          isInitialMount.current &&
          data.length > 0 &&
          viewState === "FORM"
        ) {
          setViewState("LIST");
        }

        // 의사 프로필 로드 (고유한 doctorId만) - 백그라운드에서 처리
        const uniqueDoctorIds = [...new Set(data.map((apt) => apt.doctorId))];
        for (const doctorId of uniqueDoctorIds) {
          try {
            const profile = await userApi.getUserById(doctorId);
            const stats = await userApi.getUserStats(doctorId);
            setDoctorProfiles((prev) => ({
              ...prev,
              [doctorId]: {
                avatar: profile.avatar,
                activeDays: stats.activeDays,
                department: profile.department,
              },
            }));
          } catch (error) {
            console.error(`의사 프로필 로드 실패 (ID: ${doctorId}):`, error);
          }
        }
      } catch (error) {
        console.error("예약 목록 로드 실패:", error);
        toast.error("예약 목록을 불러오는데 실패했습니다.");
      } finally {
        setIsLoadingAppointments(false);
      }
    },
    [currentPatientId, viewState]
  );

  // 처방전 로드 함수
  const loadPrescriptions = useCallback(async () => {
    try {
      setIsLoadingPrescriptions(true);
      const data = await prescriptionApi.getPatientPrescriptions(
        currentPatientId
      );
      setPrescriptions(data);
    } catch {
      // 에러 발생 시 빈 배열로 설정 (500 에러 무시)
      setPrescriptions([]);
    } finally {
      setIsLoadingPrescriptions(false);
    }
  }, [currentPatientId]);

  // 초기 예약 목록 및 처방전 로드 + 1분마다 예약 목록 자동 갱신
  useEffect(() => {
    // 초기 로드를 IIFE로 감싸서 cascading render 방지
    (async () => {
      await loadAppointments();
      await loadPrescriptions();

      // 초기 마운트 완료 표시
      isInitialMount.current = false;
    })();

    // 1분마다 예약 목록 갱신 (예약 시간이 되면 진료 중으로 자동 변경)
    const interval = setInterval(() => {
      void loadAppointments(true); // skipViewChange = true로 전달
    }, 60000);

    return () => clearInterval(interval);
  }, [loadAppointments, loadPrescriptions]);

  // [bfcache 최적화] 페이지 표시/숨김 이벤트 처리
  useEffect(() => {
    const handleVisibilityChange = () => {
      if (document.visibilityState === "visible") {
        // 페이지가 다시 보일 때 (뒤로가기 등) 데이터 갱신
        void loadAppointments(true);
        void loadPrescriptions();
      }
    };

    const handlePageShow = (event: PageTransitionEvent) => {
      if (event.persisted) {
        // bfcache에서 복원된 경우
        void loadAppointments(true);
        void loadPrescriptions();
      }
    };

    document.addEventListener("visibilitychange", handleVisibilityChange);
    window.addEventListener("pageshow", handlePageShow);

    return () => {
      document.removeEventListener("visibilitychange", handleVisibilityChange);
      window.removeEventListener("pageshow", handlePageShow);
    };
  }, [loadAppointments, loadPrescriptions]);

  // 처방전 발급 이벤트 처리 (처방전 목록 새로고침)
  useEffect(() => {
    const handlePrescriptionCreated = async (event: Event) => {
      const customEvent = event as CustomEvent<{ patientId: number }>;
      const { patientId } = customEvent.detail;

      // 현재 환자의 처방전인 경우에만 새로고침
      if (patientId === currentPatientId) {
        try {
          const data = await prescriptionApi.getPatientPrescriptions(
            currentPatientId
          );
          setPrescriptions(data);
        } catch {
          setPrescriptions([]);
        }
      }
    };

    window.addEventListener("prescription-created", handlePrescriptionCreated);

    return () => {
      window.removeEventListener(
        "prescription-created",
        handlePrescriptionCreated
      );
    };
  }, [currentPatientId]);

  // 알림에서 채팅방 자동 오픈 이벤트 처리
  useEffect(() => {
    const handleOpenAppointmentChat = async (event: Event) => {
      const customEvent = event as CustomEvent<{ appointmentId: number }>;
      const { appointmentId } = customEvent.detail;

      try {
        // 해당 예약 찾기
        const appointment = appointments.find(
          (apt) => apt.id === appointmentId
        );

        if (appointment) {
          // 채팅 가능 여부 확인
          const available = await appointmentApi.isChatAvailable(appointmentId);

          if (!available) {
            const lockMessage = await appointmentApi.getChatLockMessage(
              appointmentId
            );
            toast.error(lockMessage);
            return;
          }

          // 의사 프로필 정보 로드 (없는 경우에만)
          if (!doctorProfiles[appointment.doctorId]) {
            try {
              const profile = await userApi.getUserById(appointment.doctorId);
              const stats = await userApi.getUserStats(appointment.doctorId);
              setDoctorProfiles((prev) => ({
                ...prev,
                [appointment.doctorId]: {
                  avatar: profile.avatar,
                  activeDays: stats.activeDays,
                  department: profile.department,
                },
              }));
            } catch (error) {
              console.error(
                `의사 프로필 로드 실패 (ID: ${appointment.doctorId}):`,
                error
              );
            }
          }

          // 채팅방 열기
          setActiveAppointment({
            id: appointment.id,
            doctorId: appointment.doctorId,
            doctorName: appointment.doctorName,
            type: "진료 상담",
          });
          setIsChatEnded(false);
          // 메시지는 useEffect에서 roomId 변경 시 자동으로 로드됨
        } else {
          toast.error("해당 예약을 찾을 수 없습니다.");
        }
      } catch (error) {
        console.error("채팅방 오픈 실패:", error);
        toast.error("채팅방을 여는 중 오류가 발생했습니다.");
      }
    };

    window.addEventListener("openAppointmentChat", handleOpenAppointmentChat);

    return () => {
      window.removeEventListener(
        "openAppointmentChat",
        handleOpenAppointmentChat
      );
    };
  }, [appointments, doctorProfiles]);

  // 진료 종료 이벤트 처리 (환자 측)
  useEffect(() => {
    const handleConsultationEnded = async (event: Event) => {
      const customEvent = event as CustomEvent<{ doctorName: string }>;
      const doctorName = customEvent.detail?.doctorName || "담당 의사";

      // 채팅 상태 초기화 (메시지는 유지)
      setActiveAppointment(null);
      setIsChatEnded(true);
      setViewState("LIST");

      // 예약 목록 및 처방전 새로고침
      try {
        const appointmentsData = await appointmentApi.getPatientAppointments(
          currentPatientId
        );
        setAppointments(appointmentsData);

        const prescriptionsData = await prescriptionApi.getPatientPrescriptions(
          currentPatientId
        );
        setPrescriptions(prescriptionsData);

        toast.success(`${doctorName} 의사님의 진료가 완료되었습니다.`);
      } catch (error) {
        console.error("데이터 새로고침 실패:", error);
        toast.success(`${doctorName} 의사님의 진료가 완료되었습니다.`);
      }
    };

    window.addEventListener("consultationEnded", handleConsultationEnded);

    return () => {
      window.removeEventListener("consultationEnded", handleConsultationEnded);
    };
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
              <div className="p-4 pb-2 flex-shrink-0">
                <div className="flex items-center justify-between mb-3">
                  <h3 className="text-lg font-black text-gray-900 flex items-center gap-2">
                    <Clock className="w-5 h-5 text-[#C93831]" />
                    예약 내역
                  </h3>
                  <Select value={statusFilter} onValueChange={setStatusFilter}>
                    <SelectTrigger className="w-[130px] h-8 text-xs cursor-pointer border border-gray-300 bg-white hover:bg-gray-100 transition-colors">
                      <SelectValue placeholder="전체" />
                    </SelectTrigger>
                    <SelectContent className="bg-white">
                      <SelectItem value="ALL" className="cursor-pointer">
                        전체
                      </SelectItem>
                      <SelectItem value="SCHEDULED" className="cursor-pointer">
                        진료 예정
                      </SelectItem>
                      <SelectItem
                        value="IN_PROGRESS"
                        className="cursor-pointer"
                      >
                        진료 중
                      </SelectItem>
                      <SelectItem value="COMPLETED" className="cursor-pointer">
                        진료 완료
                      </SelectItem>
                      <SelectItem value="CANCELLED" className="cursor-pointer">
                        취소됨
                      </SelectItem>
                    </SelectContent>
                  </Select>
                </div>
              </div>
              <div className="flex-1 overflow-y-auto px-4 pb-4 min-h-0 custom-scrollbar">
                <div className="space-y-2">
                  {isLoadingAppointments ? (
                    // 스켈레톤 UI - 카드 전체 (랭킹과 동일한 색상)
                    <>
                      {[1, 2, 3].map((i) => (
                        <div
                          key={i}
                          className="rounded-xl animate-pulse h-24"
                          style={{ backgroundColor: "rgba(201, 56, 49, 0.15)" }}
                        ></div>
                      ))}
                    </>
                  ) : (
                    appointments
                      .filter((apt) => {
                        // 상태 필터 적용
                        if (
                          statusFilter !== "ALL" &&
                          apt.status !== statusFilter
                        ) {
                          return false;
                        }

                        // CANCELLED 상태인 예약은 최대 5개까지만 표시
                        if (apt.status === "CANCELLED") {
                          const cancelledAppointments = appointments.filter(
                            (a) => a.status === "CANCELLED"
                          );
                          const cancelledIndex =
                            cancelledAppointments.findIndex(
                              (a) => a.id === apt.id
                            );
                          return cancelledIndex < 5;
                        }
                        return true; // 다른 상태는 모두 표시
                      })
                      .map((apt) => {
                        const appointmentDate = new Date(apt.date);
                        const formattedDate =
                          appointmentDate.toLocaleDateString("ko-KR", {
                            month: "long",
                            day: "numeric",
                          });
                        const formattedTime =
                          appointmentDate.toLocaleTimeString("ko-KR", {
                            hour: "numeric",
                            minute: "2-digit",
                          });

                        const statusConfig = {
                          SCHEDULED: { label: "진료 예정", color: "#7950F2" },
                          IN_PROGRESS: { label: "진료 중", color: "#20C997" },
                          COMPLETED: { label: "진료 완료", color: "#868E96" },
                          CANCELLED: { label: "취소됨", color: "#E03131" },
                        };
                        const config = statusConfig[apt.status] || {
                          label: apt.status,
                          color: "#868E96",
                        };
                        const isScheduled = apt.status === "SCHEDULED";
                        const isInProgress = apt.status === "IN_PROGRESS";

                        return (
                          <div
                            key={apt.id}
                            onClick={() => handleAppointmentClick(apt)}
                            className={`p-3 rounded-xl ${
                              isScheduled || isInProgress
                                ? "bg-white/80 hover:bg-white cursor-pointer"
                                : "bg-white/80 cursor-default"
                            }`}
                          >
                            <div className="flex items-start justify-between mb-1">
                              <div className="flex-1 min-w-0">
                                <div className="font-bold text-gray-900 text-sm">
                                  {apt.doctorName} 의사
                                </div>
                                <div className="text-xs text-gray-600">
                                  {apt.departmentName || "진료 예약"}
                                </div>
                              </div>
                              <Badge
                                style={{ backgroundColor: config.color }}
                                className="text-white font-bold border-0 text-xs px-3 py-1 whitespace-nowrap flex-shrink-0 ml-2"
                              >
                                {config.label}
                              </Badge>
                            </div>
                            <div className="text-xs text-gray-600 font-medium mb-2">
                              {formattedDate} {formattedTime}
                            </div>
                            {isScheduled && (
                              <Button
                                size="sm"
                                onClick={(e) =>
                                  handleCancelAppointment(apt.id, e)
                                }
                                className="w-full rounded-lg text-xs bg-[#C93831] hover:bg-[#B02F28] active:scale-[0.98] transition-all text-white border-0 cursor-pointer"
                              >
                                <XCircle className="w-3 h-3 mr-1" />
                                취소
                              </Button>
                            )}
                          </div>
                        );
                      })
                  )}
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
              <div className="flex-1 overflow-y-auto px-4 pb-4 custom-scrollbar">
                <div className="space-y-2">
                  {isLoadingPrescriptions ? (
                    // 스켈레톤 UI - 카드 전체 (랭킹과 동일한 색상)
                    <>
                      {[1, 2].map((i) => (
                        <div
                          key={i}
                          className="rounded-xl animate-pulse h-28"
                          style={{ backgroundColor: "rgba(201, 56, 49, 0.15)" }}
                        ></div>
                      ))}
                    </>
                  ) : prescriptions.length === 0 ? (
                    <div className="text-center py-8 text-gray-500 text-sm">
                      처방전이 없습니다
                    </div>
                  ) : (
                    prescriptions.map((pres) => (
                      <div
                        key={pres.id}
                        className="p-3 rounded-xl bg-white/80 border border-gray-200"
                      >
                        <div className="font-bold text-gray-900 mb-1 text-sm">
                          {pres.diagnosis}
                        </div>
                        <div className="text-xs text-gray-600 mb-1">
                          {pres.doctorName} 의사
                        </div>
                        <div className="text-xs text-gray-500 mb-2">
                          {new Date(pres.date).toLocaleDateString("ko-KR")}
                        </div>
                        <Button
                          size="sm"
                          className="w-full rounded-lg text-xs cursor-pointer bg-[#C93831] hover:bg-[#B02F28] active:scale-[0.98] transition-all text-white border-0"
                          onClick={() => setSelectedPrescription(pres)}
                        >
                          상세보기
                        </Button>
                      </div>
                    ))
                  )}
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
                      <UserHoverCard
                        name={activeAppointment?.doctorName}
                        department={
                          activeAppointment?.doctorId
                            ? doctorProfiles[activeAppointment.doctorId]?.department || "의사"
                            : "의사"
                        }
                        size="md"
                        avatarUrl={
                          activeAppointment?.doctorId
                            ? doctorProfiles[activeAppointment.doctorId]?.avatar
                            : undefined
                        }
                        activeDays={
                          activeAppointment?.doctorId
                            ? doctorProfiles[activeAppointment.doctorId]
                                ?.activeDays
                            : undefined
                        }
                        userId={activeAppointment?.doctorId}
                      />
                      <div>
                        <div className="font-bold text-gray-900">
                          {activeAppointment?.doctorName || "알 수 없음"} 의사
                        </div>
                        <div className="text-xs text-gray-600">온라인</div>
                      </div>
                    </div>
                    <div className="flex items-center gap-3">
                      <div className="flex items-center gap-2 px-3 py-1 bg-green-100 rounded-full">
                        <div className="w-2 h-2 bg-green-500 rounded-full"></div>
                        <span className="text-xs font-bold text-green-700">
                          진료 중
                        </span>
                      </div>
                      <Button
                        variant="ghost"
                        size="icon"
                        onClick={() => {
                          // 채팅 화면만 닫고, 메시지는 유지 (다시 열면 이어서 채팅 가능)
                          setActiveAppointment(null);
                          setIsChatEnded(false);
                          setViewState("LIST");
                        }}
                        className="rounded-full hover:bg-gray-200 cursor-pointer"
                      >
                        <XCircle className="w-5 h-5 text-gray-600" />
                      </Button>
                    </div>
                  </div>

                  <div className="flex-1 overflow-y-auto mb-4 pr-2 custom-scrollbar">
                    <div className="space-y-4">
                      {messages.map((msg) => {
                        const isMine = msg.senderId === currentUserId;

                        return (
                          <div
                            key={msg.id}
                            className={`flex gap-3 ${
                              isMine ? "justify-end" : ""
                            }`}
                          >
                            {!isMine && (
                              <UserHoverCard
                                name={msg.senderName}
                                department={
                                  activeAppointment?.doctorId
                                    ? doctorProfiles[activeAppointment.doctorId]?.department || "의사"
                                    : "의사"
                                }
                                size="sm"
                                avatarUrl={
                                  activeAppointment?.doctorId
                                    ? doctorProfiles[activeAppointment.doctorId]
                                        ?.avatar
                                    : undefined
                                }
                                activeDays={
                                  activeAppointment?.doctorId
                                    ? doctorProfiles[activeAppointment.doctorId]
                                        ?.activeDays
                                    : undefined
                                }
                                userId={activeAppointment?.doctorId}
                              />
                            )}
                            <div
                              className={`rounded-2xl p-3 max-w-md ${
                                isMine ? "bg-[#C93831] text-white" : "bg-white"
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
                      className="rounded-xl bg-white border-2 border-gray-300 focus-visible:ring-0 focus-visible:border-[#C93831] transition-all duration-300 placeholder:text-gray-400"
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
                      className="bg-[#C93831] hover:bg-[#B02F28] active:scale-[0.98] transition-all text-white rounded-xl border-0 cursor-pointer"
                    >
                      <Send className="w-4 h-4" />
                    </Button>
                  </div>
                </>
              ) : viewState === "SUCCESS" && lastCreatedAppointment ? (
                // 예약 성공 화면
                <div className="h-full overflow-y-auto custom-scrollbar flex flex-col items-center justify-center">
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
                      <p className="text-gray-600">예약 정보를 확인해주세요</p>
                    </div>

                    <div className="bg-gray-50 rounded-xl p-6 mb-6 space-y-4">
                      <div className="flex justify-between items-center">
                        <span className="text-gray-600 font-medium">
                          담당 의사
                        </span>
                        <span className="font-bold text-gray-900">
                          {lastCreatedAppointment.doctorName} 의사
                        </span>
                      </div>
                      <div className="flex justify-between items-center">
                        <span className="text-gray-600 font-medium">
                          진료과
                        </span>
                        <span className="font-bold text-gray-900">
                          {lastCreatedAppointment.departmentName}
                        </span>
                      </div>
                      <div className="flex justify-between items-center">
                        <span className="text-gray-600 font-medium">
                          예약 날짜
                        </span>
                        <span className="font-bold text-gray-900">
                          {lastCreatedAppointment.date}
                        </span>
                      </div>
                      <div className="flex justify-between items-center">
                        <span className="text-gray-600 font-medium">
                          예약 시간
                        </span>
                        <span className="font-bold text-[#C93831]">
                          {lastCreatedAppointment.time}
                        </span>
                      </div>
                    </div>

                    <Button
                      onClick={() => {
                        setViewState("LIST");
                        setLastCreatedAppointment(null);
                      }}
                      className="w-full rounded-xl h-12 bg-[#C93831] hover:bg-[#B02F28] active:scale-[0.98] transition-all text-white font-bold border-0 cursor-pointer"
                    >
                      예약 목록으로 가기
                    </Button>
                  </div>
                </div>
              ) : viewState === "LIST" ? (
                // 예약 목록 상세 화면
                <div className="h-full overflow-y-auto custom-scrollbar px-6 pt-8">
                  <div className="max-w-4xl mx-auto">
                    <div className="flex items-center justify-between mb-6">
                      <h2 className="text-2xl font-black text-gray-900">
                        예약 목록
                      </h2>
                      <Button
                        onClick={() => setViewState("FORM")}
                        className="rounded-xl bg-[#C93831] hover:bg-[#B02F28] text-white font-bold px-6 active:scale-[0.98] cursor-pointer"
                      >
                        예약하기
                      </Button>
                    </div>

                    <div className="space-y-4">
                      {isLoadingAppointments ? (
                        // 스켈레톤 UI - 카드 전체 (랭킹과 동일한 색상)
                        <>
                          {[1, 2, 3].map((i) => (
                            <Card
                              key={i}
                              className="rounded-xl animate-pulse h-40"
                              style={{
                                backgroundColor: "rgba(201, 56, 49, 0.15)",
                              }}
                            ></Card>
                          ))}
                        </>
                      ) : (
                        appointments
                          .filter(
                            (apt) =>
                              apt.status === "SCHEDULED" ||
                              apt.status === "IN_PROGRESS"
                          )
                          .map((apt) => {
                            const appointmentDate = new Date(apt.date);
                            const formattedDate =
                              appointmentDate.toLocaleDateString("ko-KR", {
                                year: "numeric",
                                month: "long",
                                day: "numeric",
                              });
                            const formattedTime =
                              appointmentDate.toLocaleTimeString("ko-KR", {
                                hour: "numeric",
                                minute: "2-digit",
                              });

                            const statusConfig = {
                              SCHEDULED: {
                                label: "진료 예정",
                                color: "#7950F2",
                              },
                              IN_PROGRESS: {
                                label: "진료 중",
                                color: "#20C997",
                              },
                              COMPLETED: {
                                label: "진료 완료",
                                color: "#868E96",
                              },
                              CANCELLED: { label: "취소됨", color: "#E03131" },
                            };
                            const config = statusConfig[apt.status] || {
                              label: apt.status,
                              color: "#868E96",
                            };
                            const isScheduled = apt.status === "SCHEDULED";
                            const isInProgress = apt.status === "IN_PROGRESS";

                            return (
                              <Card
                                key={apt.id}
                                className="p-4 bg-white/80 rounded-xl"
                              >
                                <div className="flex items-start justify-between mb-3">
                                  <div className="flex-1 min-w-0">
                                    <h3 className="font-bold text-gray-900 text-sm mb-1">
                                      {apt.doctorName} 의사
                                    </h3>
                                    <p className="text-xs text-gray-600">
                                      {apt.departmentName || "진료 예약"}
                                    </p>
                                  </div>
                                  <Badge
                                    style={{ backgroundColor: config.color }}
                                    className="text-white font-bold border-0 text-xs px-3 py-1 whitespace-nowrap flex-shrink-0 ml-2"
                                  >
                                    {config.label}
                                  </Badge>
                                </div>
                                <div className="mb-3">
                                  <div className="flex items-center gap-2 text-gray-700 text-xs">
                                    <Clock className="w-3 h-3" />
                                    <span className="font-medium">
                                      {formattedDate} {formattedTime}
                                    </span>
                                  </div>
                                </div>
                                <div className="space-y-2">
                                  {isInProgress && (
                                    <Button
                                      onClick={() =>
                                        handleAppointmentClick(apt)
                                      }
                                      className="w-full rounded-xl h-10 bg-[#20C997] hover:bg-[#18A37A] text-white font-bold text-sm border-0 shadow-md hover:shadow-lg transition-all cursor-pointer"
                                    >
                                      채팅 시작
                                    </Button>
                                  )}
                                  {isScheduled && (
                                    <>
                                      <div className="grid grid-cols-2 gap-2">
                                        <Button
                                          onClick={async () => {
                                            // 1시간 전 체크
                                            const appointmentDateTime =
                                              new Date(apt.date);
                                            const now = new Date();
                                            const diffInMs =
                                              appointmentDateTime.getTime() -
                                              now.getTime();
                                            const oneHourInMs = 60 * 60 * 1000;

                                            if (
                                              diffInMs <= oneHourInMs &&
                                              diffInMs > 0
                                            ) {
                                              toast.error(
                                                "곧 진료 시간이라 변경이 불가능합니다!"
                                              );
                                              return;
                                            }

                                            try {
                                              await appointmentApi.cancelAppointment(
                                                apt.id
                                              );
                                              // 예약 목록 갱신 (스켈레톤 표시 안 함)
                                              await loadAppointments(true, true);
                                              setViewState("FORM");
                                            } catch (error) {
                                              console.error(
                                                "예약 변경 실패:",
                                                error
                                              );
                                              toast.error(
                                                "예약 변경에 실패했습니다."
                                              );
                                            }
                                          }}
                                          className="rounded-xl h-10 bg-[#C93831] hover:bg-[#B02F28] active:scale-[0.98] transition-all text-white font-bold text-sm border-0 cursor-pointer"
                                        >
                                          예약 변경
                                        </Button>
                                        <Button
                                          onClick={(e) =>
                                            handleCancelAppointment(apt.id, e)
                                          }
                                          className="rounded-xl h-10 bg-[#C93831] hover:bg-[#B02F28] active:scale-[0.98] transition-all text-white font-bold text-sm border-0 cursor-pointer"
                                        >
                                          예약 취소
                                        </Button>
                                      </div>
                                    </>
                                  )}
                                </div>
                              </Card>
                            );
                          })
                      )}
                    </div>
                  </div>
                </div>
              ) : (
                // 인라인 예약 화면 (FORM 상태)
                <div className="h-full overflow-y-auto custom-scrollbar flex flex-col items-center justify-center">
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
                        <SelectTrigger className="rounded-xl cursor-pointer border border-gray-300 bg-white hover:bg-gray-100 focus:border-gray-300 focus:ring-0 focus:ring-offset-0 transition-colors">
                          <SelectValue placeholder="진료과를 선택하세요" />
                        </SelectTrigger>
                        <SelectContent className="bg-white">
                          <SelectItem
                            value="internal"
                            className="cursor-pointer"
                          >
                            내과
                          </SelectItem>
                          <SelectItem
                            value="surgery"
                            className="cursor-pointer"
                          >
                            외과
                          </SelectItem>
                          <SelectItem
                            value="psychiatry"
                            className="cursor-pointer"
                          >
                            신경정신과
                          </SelectItem>
                          <SelectItem
                            value="dermatology"
                            className="cursor-pointer"
                          >
                            피부과
                          </SelectItem>
                          <SelectItem
                            value="thoracic_surgery"
                            className="cursor-pointer"
                          >
                            흉부외과
                          </SelectItem>
                          <SelectItem
                            value="obstetrics_gynecology"
                            className="cursor-pointer"
                          >
                            산부인과
                          </SelectItem>
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
                        className="rounded-xl border [&_button]:cursor-pointer"
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
                                    ? "bg-[#C93831] hover:bg-[#B02F28] border-0"
                                    : "hover:border-[#C93831] hover:text-[#C93831]"
                                } ${
                                  isDisabled
                                    ? "opacity-50 cursor-not-allowed bg-gray-100"
                                    : "cursor-pointer"
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
                      className="w-full bg-[#C93831] hover:bg-[#B02F28] active:scale-[0.98] transition-all text-white font-bold rounded-xl h-12 border-0 cursor-pointer disabled:cursor-not-allowed"
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
