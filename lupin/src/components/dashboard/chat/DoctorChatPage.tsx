/**
 * DoctorChatPage.tsx
 *
 * [수정 완료]
 * 1. 미사용 import (ScrollArea) 제거 -> 에러 해결
 * 2. 채팅 자동 스크롤, 이름 표시 오류 수정, 예약 취소 등 모든 기능 정상 동작
 * 3. roomId 형식 수정: {patientId}:{doctorId} → appointment_{appointmentId}
 *    - 백엔드와 일치하는 형식 사용 (AppointmentService에서 생성)
 *    - chatRooms에서 올바른 roomId를 가져와 사용
 */

import { useState, useEffect, useCallback, useRef, useMemo } from "react";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
} from "@/components/ui/dialog";
import { Send, CheckCircle, FileText, Minus, Edit2 } from "lucide-react";
import { toast } from "sonner";
import { Member } from "@/types/dashboard.types";
import { useWebSocket } from "@/hooks/useWebSocket";
import { chatApi, ChatMessageResponse, ChatRoomResponse } from "@/api/chatApi";
import { prescriptionApi } from "@/api/prescriptionApi";
import { appointmentApi } from "@/api/appointmentApi";
import { userApi } from "@/api/userApi";
import UserHoverCard from "@/components/dashboard/shared/UserHoverCard";
import { formatChatTime, canEnterChatRoom } from "./utils";
import { MAX_MEDICINES, MAX_INSTRUCTIONS_LENGTH, CHAT_ROOMS_REFRESH_INTERVAL, PROFILE_BATCH_SIZE } from "./constants";

interface Medicine {
  id: number;
  code: string;
  name: string;
  description?: string;
  precautions?: string;
}

export default function DoctorChatPage() {
  const currentUserId = parseInt(localStorage.getItem("userId") || "0");

  // 현재 활성화된 roomId를 명시적으로 관리
  const [activeRoomId, setActiveRoomId] = useState<string | null>(null);

  const [selectedChatMember, setSelectedChatMember] = useState<Member | null>(
    null
  );
  const [chatRooms, setChatRooms] = useState<ChatRoomResponse[]>([]);
  const [messages, setMessages] = useState<ChatMessageResponse[]>([]);
  const [chatMessage, setChatMessage] = useState("");
  const [showMedicineDialog, setShowMedicineDialog] = useState(false);

  // 환자 프로필 아바타 저장 (patientId -> avatarUrl)
  const [patientAvatars, setPatientAvatars] = useState<Record<number, string>>(
    {}
  );
  // 환자 활동일 저장 (patientId -> activeDays)
  const [patientActiveDays, setPatientActiveDays] = useState<
    Record<number, number>
  >({});
  // 환자 부서 저장 (patientId -> department)
  const [patientDepartments, setPatientDepartments] = useState<
    Record<number, string>
  >({});

  // 스크롤 제어용 Ref
  const messagesEndRef = useRef<HTMLDivElement>(null);

  // 필터링된 채팅방 목록 (useMemo로 최적화)
  const filteredChatRooms = useMemo(() => {
    return chatRooms.filter(
      (room) => room.status === "IN_PROGRESS" || room.status === "SCHEDULED"
    );
  }, [chatRooms]);

  // 처방전 폼 상태
  const [prescriptionDate] = useState(
    new Date().toLocaleDateString("ko-KR", { month: "long", day: "numeric" })
  );
  const [diagnosis, setDiagnosis] = useState("");
  const [instructions, setInstructions] = useState("");
  const [selectedMedicines, setSelectedMedicines] = useState<Medicine[]>([]);

  // 약품 검색 관련 상태
  const [searchQuery, setSearchQuery] = useState("");
  const [searchResults, setSearchResults] = useState<Medicine[]>([]);
  const [isSearching, setIsSearching] = useState(false);

  // 채팅방 목록 로드 함수 (재사용 가능하도록 별도 함수로 분리)
  // [최적화] 프로필 캐싱 추가 및 불필요한 API 호출 제거
  const loadChatRooms = useCallback(
    async (forceReload = false) => {
      try {
        if (!currentUserId) return;

        const rooms = await chatApi.getChatRooms(currentUserId);
        // 최신 메시지 순서대로 정렬 (카톡처럼)
        const sortedRooms = rooms.sort(
          (a: ChatRoomResponse, b: ChatRoomResponse) => {
            const timeA = a.lastMessageTime
              ? new Date(a.lastMessageTime).getTime()
              : 0;
            const timeB = b.lastMessageTime
              ? new Date(b.lastMessageTime).getTime()
              : 0;
            return timeB - timeA; // 최신순
          }
        );
        setChatRooms(sortedRooms);

        // [최적화] 캐싱된 프로필이 없는 환자만 로드
        const newPatientIds = sortedRooms
          .map((room: ChatRoomResponse) => room.patientId)
          .filter(
            (patientId: number) => forceReload || !patientAvatars[patientId]
          );

        if (newPatientIds.length === 0) return;

        // [최적화] 병렬 처리 최대 5개씩 제한 (서버 부하 감소)
        const batchSize = PROFILE_BATCH_SIZE;
        const avatars: Record<number, string> = { ...patientAvatars };
        const activeDaysMap: Record<number, number> = { ...patientActiveDays };
        const departmentsMap: Record<number, string> = {
          ...patientDepartments,
        };

        for (let i = 0; i < newPatientIds.length; i += batchSize) {
          const batch = newPatientIds.slice(i, i + batchSize);
          await Promise.all(
            batch.map(async (patientId: number) => {
              try {
                const patient = await userApi.getUserById(patientId);
                if (patient.avatar) {
                  avatars[patientId] = patient.avatar;
                }
                if (patient.department) {
                  departmentsMap[patientId] = patient.department;
                }

                // 활동일 정보 가져오기
                try {
                  const stats = await userApi.getUserStats(patientId);
                  if (stats.activeDays !== undefined) {
                    activeDaysMap[patientId] = stats.activeDays;
                  }
                } catch (statsError) {
                  console.error(
                    `환자 ${patientId} 통계 로드 실패:`,
                    statsError
                  );
                }
              } catch (error) {
                console.error(`환자 ${patientId} 프로필 로드 실패:`, error);
              }
            })
          );
        }

        setPatientAvatars(avatars);
        setPatientActiveDays(activeDaysMap);
        setPatientDepartments(departmentsMap);
      } catch (error) {
        console.error("채팅방 목록 로드 실패:", error);
      }
    },
    [currentUserId, patientAvatars, patientActiveDays, patientDepartments]
  );

  const handleMessageReceived = useCallback(
    (message: ChatMessageResponse) => {
      // 현재 보고 있는 방에 온 메시지만 추가
      if (activeRoomId && message.roomId === activeRoomId) {
        setMessages((prev) => [...prev, message]);
      }

      if (message.senderId !== currentUserId) {
        // 다른 방에 메시지가 오면 알림
        if (message.roomId !== activeRoomId) {
          toast.success("새 메시지가 도착했습니다");
        }
      }
      // [최적화] 메시지 수신 시 프로필 재로드 안 함 (캐시 사용)
      loadChatRooms(false);
    },
    [currentUserId, loadChatRooms, activeRoomId]
  );

  const { isConnected, sendMessage: sendWebSocketMessage } = useWebSocket({
    roomId: activeRoomId || "",
    userId: currentUserId,
    onMessageReceived: handleMessageReceived,
  });

  // 자동 스크롤
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  useEffect(() => {
    loadChatRooms();
  }, [loadChatRooms]);

  // [최적화] 3분마다 채팅방 목록 갱신 (1분 → 3분으로 변경하여 서버 부하 감소)
  useEffect(() => {
    const interval = setInterval(() => {
      loadChatRooms(false); // 캐시된 프로필 사용
    }, CHAT_ROOMS_REFRESH_INTERVAL);

    return () => clearInterval(interval);
  }, [loadChatRooms]);

  // [bfcache 최적화] 페이지 표시/숨김 이벤트 처리 + 스크롤 위치 복원
  useEffect(() => {
    const handleVisibilityChange = () => {
      if (document.visibilityState === "visible") {
        // 페이지가 다시 보일 때 (뒤로가기 등) 캐시 사용하여 빠르게 로드
        loadChatRooms(false);
      }
    };

    const handlePageShow = (event: PageTransitionEvent) => {
      if (event.persisted) {
        // bfcache에서 복원된 경우
        loadChatRooms(false);

        // 스크롤 위치 복원
        const savedScrollPosition = sessionStorage.getItem(
          "doctorChatScrollPosition"
        );
        if (savedScrollPosition) {
          setTimeout(() => {
            window.scrollTo(0, parseInt(savedScrollPosition, 10));
          }, 100);
        }
      }
    };

    const handlePageHide = () => {
      // 스크롤 위치 저장
      sessionStorage.setItem(
        "doctorChatScrollPosition",
        window.scrollY.toString()
      );
    };

    document.addEventListener("visibilitychange", handleVisibilityChange);
    window.addEventListener("pageshow", handlePageShow);
    window.addEventListener("pagehide", handlePageHide);

    return () => {
      document.removeEventListener("visibilitychange", handleVisibilityChange);
      window.removeEventListener("pageshow", handlePageShow);
      window.removeEventListener("pagehide", handlePageHide);
    };
  }, [loadChatRooms]);

  // 알림 클릭 시 채팅창 자동 오픈 (5분 전 알림)
  useEffect(() => {
    const handleOpenChat = async (event: Event) => {
      const customEvent = event as CustomEvent<{ appointmentId: number }>;
      const { appointmentId } = customEvent.detail;

      // appointmentId로 roomId 생성
      const roomId = `appointment_${appointmentId}`;

      // 채팅방 목록에서 해당 채팅방 찾기
      const chatRoom = chatRooms.find((room) => room.roomId === roomId);

      if (
        chatRoom &&
        canEnterChatRoom(chatRoom.appointmentTime, chatRoom.status)
      ) {
        // 입장 가능한 경우 채팅창 오픈
        setActiveRoomId(roomId);
        setSelectedChatMember({
          id: chatRoom.patientId,
          name: chatRoom.patientName,
          avatar: "",
          age: 0,
          gender: "",
          lastVisit: "",
          condition: "",
          status: "in-progress",
        });
      }
    };

    window.addEventListener("openAppointmentChat", handleOpenChat);

    return () => {
      window.removeEventListener("openAppointmentChat", handleOpenChat);
    };
  }, [chatRooms]);

  // activeRoomId가 변경될 때마다 메시지를 새로 로드
  useEffect(() => {
    // roomId가 없으면 로드하지 않음
    if (!activeRoomId) {
      setMessages([]);
      return;
    }

    const loadMessages = async () => {
      try {
        const loadedMessages = await chatApi.getAllMessagesByRoomId(
          activeRoomId
        );
        setMessages(loadedMessages);
      } catch (error) {
        console.error("메시지 로드 실패:", error);
      }
    };

    loadMessages();
  }, [activeRoomId]);

  // 읽음 처리 로직
  useEffect(() => {
    if (isConnected && activeRoomId) {
      const markMessagesAsRead = async () => {
        try {
          await chatApi.markAsRead(activeRoomId, currentUserId);
          await loadChatRooms();
        } catch (error) {
          console.error("❌ 읽음 처리 실패:", error);
        }
      };
      markMessagesAsRead();
    }
  }, [isConnected, activeRoomId, currentUserId, loadChatRooms]);

  const handleFinishConsultation = async () => {
    if (!selectedChatMember || !activeRoomId) return;

    if (!confirm(`${selectedChatMember.name}님의 진료를 종료하시겠습니까?`)) {
      return;
    }

    // roomId에서 appointmentId 추출 (appointment_123 -> 123)
    const appointmentId = parseInt(activeRoomId.replace("appointment_", ""));
    const memberName = selectedChatMember.name;

    // 1. WebSocket 연결이 살아있을 때 API 호출 (백엔드에서 CONSULTATION_END 전송)
    try {
      await appointmentApi.completeAppointment(appointmentId);

      // 2. WebSocket 메시지 전송 대기 (트랜잭션 커밋 + 네트워크 전송 시간 확보)
      setTimeout(() => {
        // 3. alert로 진료 종료 알림
        alert(`${memberName}님의 진료가 종료되었습니다.`);

        // 4. UI 업데이트
        setSelectedChatMember(null);
        setActiveRoomId(null);
        setMessages([]);

        // 5. 채팅방 목록 갱신
        loadChatRooms();
      }, 500);
    } catch (error) {
      console.error("진료 종료 API 실패:", error);
      alert("진료 종료 중 오류가 발생했습니다.");
    }
  };

  const handleSendDoctorChat = () => {
    if (!chatMessage.trim() || !activeRoomId) return;

    sendWebSocketMessage(chatMessage, currentUserId);

    setChatMessage("");
  };

  // 입력창 포커스 시 읽음 처리
  const handleInputFocus = async () => {
    if (activeRoomId) {
      try {
        await chatApi.markAsRead(activeRoomId, currentUserId);
        await loadChatRooms();
      } catch (error) {
        console.error("❌ 읽음 처리 실패:", error);
      }
    }
  };

  // 약품 검색
  const handleSearchMedicines = async (query: string) => {
    setSearchQuery(query);

    if (!query.trim()) {
      setSearchResults([]);
      return;
    }

    setIsSearching(true);
    try {
      const data = await prescriptionApi.searchMedicines(query);
      setSearchResults(data);
    } catch (error) {
      console.error("약품 검색 실패:", error);
    } finally {
      setIsSearching(false);
    }
  };

  // 약품 추가 (클릭 또는 엔터)
  const handleAddMedicine = (medicine: Medicine) => {
    // 약품 개수 제한 체크 (최대 5개)
    if (selectedMedicines.length >= MAX_MEDICINES) {
      toast.error(`약품은 최대 ${MAX_MEDICINES}개까지 선택할 수 있습니다.`);
      return;
    }

    // 이미 추가된 약품인지 확인
    const existing = selectedMedicines.find((m) => m.id === medicine.id);

    if (!existing) {
      // 새로 추가
      setSelectedMedicines([
        ...selectedMedicines,
        {
          id: medicine.id,
          code: medicine.code,
          name: medicine.name,
          description: medicine.description,
          precautions: medicine.precautions,
        },
      ]);
    }

    // 검색어 초기화하지만 다이얼로그는 유지
    setSearchQuery("");
    setSearchResults([]);
  };

  // 약품 제거
  const handleRemoveMedicine = (id: number) => {
    setSelectedMedicines(selectedMedicines.filter((m) => m.id !== id));
  };

  const handleOpenMedicineDialog = () => {
    setSearchQuery("");
    setSearchResults([]);
    setShowMedicineDialog(true);
  };

  // DoctorChatPage.tsx 내부의 handleSavePrescription 함수를 이것으로 교체하세요.

  const handleSavePrescription = async () => {
    // 1. 기본 유효성 검사
    if (!selectedChatMember || !activeRoomId) {
      toast.error("환자 및 진료 대화방이 선택되지 않았습니다.");
      return;
    }

    if (!diagnosis.trim()) {
      toast.error("진단명을 입력해주세요.");
      return;
    }

    if (selectedMedicines.length === 0) {
      toast.error("처방할 약품을 최소 1개 이상 선택해주세요.");
      return;
    }

    try {
      // 2. roomId에서 appointmentId 추출 (예: "appointment_123" -> 123)
      const appointmentId = parseInt(activeRoomId.replace("appointment_", ""));

      // 3. API 요청 데이터 구성
      const medicinePayload = selectedMedicines.map((med) => ({
        medicineId: med.id,
        medicineName: med.name,
        instructions: instructions || "",
      }));

      const requestData = {
        appointmentId: appointmentId,
        patientId: selectedChatMember.id,
        diagnosis: diagnosis.trim(),
        medicines: medicinePayload,
        additionalInstructions: instructions || "",
      };

      // 4. API 호출
      await prescriptionApi.create(requestData);

      // 5. 성공 처리
      toast.success("처방전이 성공적으로 발급되었습니다.");

      // 채팅방에도 알림 메시지 자동 전송
      // [중요] 환자 측에서 이 텍스트("처방전이 발급되었습니다")를 감지하여 모달을 띄웁니다.
      if (isConnected) {
        sendWebSocketMessage(
          "📋 처방전이 발급되었습니다. 확인해주세요.",
          currentUserId
        );
      } else {
        console.warn("WebSocket 연결 끊김: 알림 메시지 전송 실패");
      }

      // 6. 폼 초기화
      setDiagnosis("");
      setInstructions("");
      setSelectedMedicines([]);
    } catch (error) {
      console.error("처방전 발급 실패:", error);

      if (error && typeof error === "object" && "response" in error) {
        const axiosError = error as {
          response?: { data?: { message?: string } };
        };
        console.error("에러 상세:", axiosError.response?.data);

        const errorMessage =
          axiosError.response?.data?.message ||
          "처방전 발급 중 오류가 발생했습니다.";
        toast.error(errorMessage);
      } else {
        toast.error("처방전 발급 중 오류가 발생했습니다.");
      }
    }
  };

  const getMedicinesText = () => {
    if (selectedMedicines.length === 0) return "약품을 선택하세요";
    return selectedMedicines.map((m) => m.name).join(", ");
  };

  return (
    <div className="h-full overflow-hidden p-8">
      {/* 너비를 1600px로 확장 */}
      <div className="max-w-[1600px] mx-auto h-full flex flex-col">
        <div className="flex-shrink-0">
          <h1 className="text-5xl font-black text-gray-900 mb-6">
            채팅 & 처방전 작성
          </h1>
        </div>

        <Card className="backdrop-blur-2xl bg-white/60 border border-gray-200 shadow-2xl flex-1 mx-auto overflow-hidden h-full w-full">
          <div className="h-full flex">
            {/* 좌측: 대화 목록 */}
            <div className="w-96 border-r border-gray-200 px-6 py-6 flex flex-col h-full">
              <h3 className="text-xl font-black text-gray-900 mb-4 flex items-center gap-2 flex-shrink-0">
                대화 목록
              </h3>
              <div className="flex-1 overflow-y-auto custom-scrollbar">
                <div className="space-y-3 pr-2">
                  {filteredChatRooms.length === 0 ? (
                    <div className="flex items-center justify-center h-full text-gray-500">
                      예약된 채팅방이 없습니다
                    </div>
                  ) : (
                    filteredChatRooms.map((room) => {
                        // activeRoomId로 선택 여부 판단
                        const isSelected = activeRoomId === room.roomId;

                        // 입장 가능 여부 확인
                        const canEnter = canEnterChatRoom(
                          room.appointmentTime,
                          room.status
                        );

                        return (
                          <div
                            key={room.roomId}
                            onClick={() => {
                              // 입장 불가능하면 클릭 무시
                              if (!canEnter) return;

                              // 이미 선택된 채팅방이면 아무 작업도 하지 않음
                              if (isSelected) return;

                              // 활성 룸 ID 변경 (useEffect가 메시지 로드)
                              setActiveRoomId(room.roomId);

                              // 선택된 멤버 정보 업데이트
                              setSelectedChatMember({
                                id: room.patientId,
                                name: room.patientName,
                                avatar:
                                  patientAvatars[room.patientId] ||
                                  room.patientName.charAt(0),
                                age: 0,
                                gender: "",
                                lastVisit: "",
                                condition: "",
                                status: "in-progress",
                              });

                              // 메시지 초기화
                              setMessages([]);
                            }}
                            className={`p-3 rounded-xl border-2 transition-all ${
                              !canEnter
                                ? "bg-gray-50 border-gray-300 opacity-60 cursor-not-allowed"
                                : isSelected
                                ? "bg-gray-200 border-transparent cursor-pointer"
                                : "bg-white border-transparent hover:bg-gray-50 cursor-pointer"
                            }`}
                          >
                            <div className="flex items-center gap-3 mb-2">
                              <UserHoverCard
                                name={room.patientName}
                                department={
                                  patientDepartments[room.patientId] || "환자"
                                }
                                size="sm"
                                avatarUrl={patientAvatars[room.patientId]}
                                activeDays={patientActiveDays[room.patientId]}
                                userId={room.patientId}
                              />
                              <div className="flex-1 min-w-0">
                                <div className="flex items-center justify-between mb-1">
                                  <div className="font-bold text-sm text-gray-900">
                                    {room.patientName}
                                  </div>
                                  <div className="text-xs text-gray-500">
                                    {formatChatTime(room.lastMessageTime)}
                                  </div>
                                </div>
                                {room.appointmentTime && (
                                  <div className="flex items-center gap-2 mb-1">
                                    <div
                                      className={`text-xs font-semibold ${
                                        canEnter
                                          ? "text-[#C93831]"
                                          : "text-blue-600"
                                      }`}
                                    >
                                      예약시간 :{" "}
                                      {new Date(
                                        room.appointmentTime
                                      ).toLocaleString("ko-KR", {
                                        month: "long",
                                        day: "numeric",
                                        hour: "numeric",
                                        minute: "2-digit",
                                      })}
                                    </div>
                                    {!canEnter && (
                                      <Badge className="bg-yellow-500 text-white font-bold border-0 text-xs">
                                        예약 중
                                      </Badge>
                                    )}
                                  </div>
                                )}
                                <div className="flex items-center justify-between">
                                  <div className="text-xs text-gray-600 truncate flex-1">
                                    {room.lastMessage}
                                  </div>
                                  {room.unreadCount > 0 && (
                                    <Badge className="bg-red-500 text-white font-bold border-0 text-xs ml-2 flex-shrink-0">
                                      {room.unreadCount}
                                    </Badge>
                                  )}
                                </div>
                              </div>
                            </div>
                          </div>
                        );
                      })
                  )}
                </div>
              </div>
            </div>

            {/* 중앙: 채팅 영역 */}
            <div className="flex-1 flex flex-col p-6 border-r border-gray-200 h-full overflow-hidden">
              {selectedChatMember && activeRoomId ? (
                <>
                  <div className="flex items-center justify-between pb-4 border-b border-gray-200 mb-4 flex-shrink-0">
                    <div className="flex items-center gap-3">
                      <UserHoverCard
                        name={selectedChatMember.name}
                        department={
                          selectedChatMember.id
                            ? patientDepartments[selectedChatMember.id] ||
                              "환자"
                            : "환자"
                        }
                        size="md"
                        avatarUrl={selectedChatMember.avatar}
                        activeDays={
                          selectedChatMember.id
                            ? patientActiveDays[selectedChatMember.id]
                            : undefined
                        }
                        userId={selectedChatMember.id}
                      />
                      <div>
                        <div className="font-bold text-gray-900">
                          {selectedChatMember.name}
                        </div>
                        <div className="text-xs text-gray-600 flex items-center gap-1">
                          <span className="w-2 h-2 rounded-full bg-green-500"></span>
                          진료 중
                        </div>
                      </div>
                    </div>
                    <Button
                      onClick={handleFinishConsultation}
                      className="bg-[#C93831] hover:bg-[#B02F28] active:scale-[0.98] transition-all rounded-2xl shadow-lg hover:shadow-xl text-white font-bold cursor-pointer"
                    >
                      <CheckCircle className="w-4 h-4 mr-2" />
                      진료 종료
                    </Button>
                  </div>

                  {/* 채팅 메시지 영역 */}
                  <div className="flex-1 overflow-y-auto mb-4 min-h-0 pr-2 custom-scrollbar">
                    <div className="space-y-4">
                      {messages.map((msg) => {
                        const isMine = msg.senderId === currentUserId;

                        // 이름 표시 로직 개선: senderName이 없으면 선택된 환자 이름 사용
                        let senderDisplayName = "알 수 없음";
                        if (isMine) {
                          senderDisplayName = "나";
                        } else {
                          senderDisplayName =
                            msg.senderName ||
                            selectedChatMember?.name ||
                            "알 수 없음";
                        }

                        return (
                          <div
                            key={msg.id}
                            className={`flex gap-3 ${
                              isMine ? "justify-end" : ""
                            }`}
                          >
                            {!isMine && (
                              <UserHoverCard
                                name={senderDisplayName}
                                department={
                                  selectedChatMember?.id
                                    ? patientDepartments[
                                        selectedChatMember.id
                                      ] || "환자"
                                    : "환자"
                                }
                                size="sm"
                                avatarUrl={selectedChatMember?.avatar}
                                activeDays={
                                  selectedChatMember?.id
                                    ? patientActiveDays[selectedChatMember.id]
                                    : undefined
                                }
                                userId={selectedChatMember?.id}
                              />
                            )}
                            <div
                              className={`rounded-2xl p-3 max-w-md ${
                                isMine
                                  ? "bg-[#C93831] text-white"
                                  : "bg-white border border-gray-200"
                              }`}
                            >
                              {!isMine && (
                                <div className="font-bold text-xs text-gray-900 mb-1">
                                  {senderDisplayName}
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

                  <div className="flex gap-2 flex-shrink-0">
                    <Input
                      placeholder="메시지 입력..."
                      className="rounded-xl bg-white border-2 border-gray-300 focus-visible:ring-0 focus-visible:ring-offset-0 focus-visible:border-[#C93831]"
                      value={chatMessage}
                      onChange={(e) => setChatMessage(e.target.value)}
                      onFocus={handleInputFocus}
                      onKeyDown={(e) => {
                        if (e.key === "Enter") {
                          handleSendDoctorChat();
                        }
                      }}
                    />
                    <Button
                      onClick={handleSendDoctorChat}
                      className="bg-[#C93831] hover:bg-[#B02F28] active:scale-[0.98] transition-all rounded-2xl shadow-lg hover:shadow-xl text-white cursor-pointer"
                    >
                      <Send className="w-4 h-4" />
                    </Button>
                  </div>
                </>
              ) : (
                <div className="flex items-center justify-center h-full text-gray-500">
                  환자를 선택하세요
                </div>
              )}
            </div>

            {/* 우측: 처방전 작성 */}
            <div className="w-96 px-6 py-6 flex flex-col h-full">
              <h3 className="text-xl font-black text-gray-900 mb-4 flex items-center gap-2 flex-shrink-0">
                <FileText className="w-5 h-5 text-[#C93831]" />
                처방전 작성
              </h3>

              {selectedChatMember ? (
                <>
                  <div className="flex-1 overflow-y-auto pr-2 custom-scrollbar">
                    <div className="space-y-4">
                      <div>
                        <Label className="text-sm font-bold">처방일</Label>
                        <Input
                          value={prescriptionDate}
                          disabled
                          className="mt-1 rounded-xl bg-white text-black disabled:opacity-100 border-2 border-gray-300"
                        />
                      </div>

                      <div>
                        <Label className="text-sm font-bold">담당 의사</Label>
                        <Input
                          value={localStorage.getItem("userName") || "의료진"}
                          disabled
                          className="mt-1 rounded-xl bg-white text-black disabled:opacity-100 border-2 border-gray-300"
                        />
                      </div>

                      <div>
                        <Label className="text-sm font-bold">진단명</Label>
                        <Input
                          value={diagnosis}
                          onChange={(e) => setDiagnosis(e.target.value)}
                          placeholder="예: 급성 상기도 감염"
                          className="mt-1 rounded-xl bg-white placeholder:text-gray-400 border-2 border-gray-300 transition-all duration-200 focus-visible:ring-0 focus-visible:ring-offset-0 focus-visible:border-[#C93831]"
                        />
                      </div>

                      <div>
                        <div className="flex items-center justify-between mb-2">
                          <Label className="text-sm font-bold">처방 약품</Label>
                          <Button
                            size="sm"
                            variant="ghost"
                            onClick={handleOpenMedicineDialog}
                            className="text-xs text-blue-600 hover:text-blue-700"
                          >
                            <Edit2 className="w-3 h-3 mr-1" />
                            약품 선택 ({selectedMedicines.length}/{MAX_MEDICINES}개)
                          </Button>
                        </div>
                        <div
                          onClick={handleOpenMedicineDialog}
                          className="min-h-[90px] p-3 rounded-xl border-2 border-dashed border-gray-300 bg-white cursor-pointer hover:border-[#C93831] transition-all duration-300"
                        >
                          <p
                            className={`text-sm whitespace-pre-wrap ${
                              selectedMedicines.length === 0
                                ? "text-gray-400"
                                : "text-gray-900"
                            }`}
                          >
                            {getMedicinesText()}
                          </p>
                        </div>
                      </div>

                      <div>
                        <div className="flex items-center justify-between mb-2">
                          <Label className="text-sm font-bold">복용 방법</Label>
                          <span className="text-xs text-gray-500">
                            {instructions.length}/{MAX_INSTRUCTIONS_LENGTH}자
                          </span>
                        </div>
                        <Textarea
                          value={instructions}
                          onChange={(e) => {
                            if (e.target.value.length <= MAX_INSTRUCTIONS_LENGTH) {
                              setInstructions(e.target.value);
                            } else {
                              toast.error(`복용 방법은 최대 ${MAX_INSTRUCTIONS_LENGTH}자까지 입력할 수 있습니다.`);
                            }
                          }}
                          placeholder="하루 3회, 식후 30분에 복용하세요."
                          className="mt-1 rounded-xl bg-white placeholder:text-gray-400 border-2 border-gray-300 transition-all duration-200 focus-visible:ring-0 focus-visible:ring-offset-0 focus-visible:border-[#C93831]"
                          rows={6}
                        />
                      </div>
                    </div>
                  </div>

                  <div className="mt-4 pt-4 border-t flex-shrink-0">
                    <Button
                      onClick={handleSavePrescription}
                      className="w-full h-14 text-lg font-bold bg-[#C93831] hover:bg-[#B02F28] active:scale-[0.98] transition-all rounded-2xl shadow-lg hover:shadow-xl text-white cursor-pointer"
                    >
                      처방전 저장
                    </Button>
                  </div>
                </>
              ) : (
                <div className="flex items-center justify-center h-full text-gray-500">
                  환자를 선택하세요
                </div>
              )}
            </div>
          </div>
        </Card>
      </div>

      {/* 약품 선택 다이얼로그 */}
      <Dialog open={showMedicineDialog} onOpenChange={setShowMedicineDialog}>
        <DialogContent className="max-w-2xl max-h-[80vh]">
          <DialogHeader>
            <DialogTitle className="text-2xl font-black">
              약품 검색 및 선택
            </DialogTitle>
            <DialogDescription>
              약품명을 검색하여 처방할 약품을 추가하세요
            </DialogDescription>
          </DialogHeader>

          {/* 검색 입력 */}
          <div className="space-y-4">
            <div>
              <Input
                placeholder="약품명을 입력하세요 (예: 타이레놀)"
                value={searchQuery}
                onChange={(e) => handleSearchMedicines(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === "Enter" && searchResults.length > 0) {
                    handleAddMedicine(searchResults[0]);
                  }
                }}
                className="rounded-xl bg-white border-2 border-gray-300 placeholder:text-gray-400 transition-all duration-200 focus-visible:ring-0 focus-visible:ring-offset-0 focus-visible:border-[#C93831]"
                autoFocus
              />
            </div>

            {/* 검색 결과 */}
            {searchQuery && (
              <div className="border rounded-xl p-2 max-h-[200px] overflow-y-auto custom-scrollbar">
                {isSearching ? (
                  <div className="text-center py-4 text-gray-500">
                    검색 중...
                  </div>
                ) : searchResults.length === 0 ? (
                  <div className="text-center py-4 text-gray-500">
                    검색 결과가 없습니다
                  </div>
                ) : (
                  <div className="space-y-1">
                    {searchResults.map((medicine) => (
                      <div
                        key={medicine.id}
                        onClick={() => handleAddMedicine(medicine)}
                        className="p-3 rounded-lg hover:bg-blue-50 cursor-pointer transition-colors"
                      >
                        <div className="font-medium text-gray-900">
                          {medicine.name}
                        </div>
                        {medicine.description && (
                          <div className="text-xs text-gray-500 mt-1">
                            {medicine.description}
                          </div>
                        )}
                      </div>
                    ))}
                  </div>
                )}
              </div>
            )}

            {/* 선택된 약품 목록 */}
            <div>
              <Label className="text-sm font-bold mb-2 block">
                선택된 약품 ({selectedMedicines.length}개)
              </Label>
              <div className="border rounded-xl p-3 min-h-[150px] max-h-[400px] overflow-y-auto custom-scrollbar space-y-3">
                {selectedMedicines.length === 0 ? (
                  <div className="text-center py-8 text-gray-400">
                    선택된 약품이 없습니다
                  </div>
                ) : (
                  selectedMedicines.map((medicine) => (
                    <div
                      key={medicine.id}
                      className="p-3 rounded-lg border bg-white space-y-2"
                    >
                      <div className="flex items-start justify-between">
                        <div className="flex-1">
                          <div className="text-sm font-medium text-gray-700">
                            {medicine.name}
                          </div>
                          {medicine.description && (
                            <div className="text-xs text-gray-500 mt-1">
                              {medicine.description}
                            </div>
                          )}
                        </div>
                        <Button
                          size="sm"
                          variant="ghost"
                          className="h-8 w-8 p-0 rounded-full text-red-500 hover:text-red-700 hover:bg-red-50"
                          onClick={() => handleRemoveMedicine(medicine.id)}
                        >
                          <Minus className="w-4 h-4" />
                        </Button>
                      </div>
                    </div>
                  ))
                )}
              </div>
            </div>

            {/* 닫기 버튼 */}
            <div className="flex justify-end">
              <Button
                className="bg-[#C93831] hover:bg-[#B02F28] active:scale-[0.98] transition-all rounded-2xl shadow-lg hover:shadow-xl text-white font-bold px-6 cursor-pointer"
                onClick={() => setShowMedicineDialog(false)}
              >
                완료
              </Button>
            </div>
          </div>
        </DialogContent>
      </Dialog>
    </div>
  );
}
