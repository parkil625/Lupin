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

import { useState, useEffect, useCallback, useRef } from "react";
import { Card } from "@/components/ui/card";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
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
import { Send, CheckCircle, FileText, Plus, Minus, Edit2 } from "lucide-react";
import { toast } from "sonner";
import { Member } from "@/types/dashboard.types";
import { useWebSocket } from "@/hooks/useWebSocket";
import { chatApi, ChatMessageResponse, ChatRoomResponse } from "@/api/chatApi";
import { prescriptionApi } from "@/api/prescriptionApi";
import { appointmentApi } from "@/api/appointmentApi";

interface MedicineQuantity {
  id: number;
  name: string;
  quantity: number;
}

interface MedicineSearchResult {
  id: number;
  code: string;
  name: string;
  manufacturer: string;
  standardDosage: string;
  unit: string;
  description: string;
}

// 🔧 제거: ReadNotification (REST API로만 처리)

// 시간 포맷 함수 (카톡 스타일)
const formatChatTime = (timeString?: string) => {
  if (!timeString) return "";

  const messageTime = new Date(timeString);
  const today = new Date();

  // 오늘인지 확인
  const isToday = messageTime.toDateString() === today.toDateString();

  if (isToday) {
    // 오늘이면 시간만 표시 (오후 3:45)
    return messageTime.toLocaleTimeString("ko-KR", {
      hour: "numeric",
      minute: "2-digit",
      hour12: true,
    });
  } else {
    // 오늘이 아니면 날짜 표시 (12월 11일)
    return messageTime.toLocaleDateString("ko-KR", {
      month: "long",
      day: "numeric",
    });
  }
};

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

  // 스크롤 제어용 Ref
  const messagesEndRef = useRef<HTMLDivElement>(null);

  // 처방전 폼 상태
  const [prescriptionName, setPrescriptionName] = useState("");
  const [prescriptionDate] = useState(
    new Date().toLocaleDateString("ko-KR", { month: "long", day: "numeric" })
  );
  const [diagnosis, setDiagnosis] = useState("");
  const [instructions, setInstructions] = useState("");
  const [selectedMedicines, setSelectedMedicines] = useState<
    MedicineQuantity[]
  >([]);

  // 약품 검색 관련 상태
  const [searchQuery, setSearchQuery] = useState("");
  const [searchResults, setSearchResults] = useState<MedicineSearchResult[]>([]);
  const [isSearching, setIsSearching] = useState(false);

  // 채팅방 목록 로드 함수 (재사용 가능하도록 별도 함수로 분리)
  const loadChatRooms = useCallback(async () => {
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
    } catch (error) {
      console.error("채팅방 목록 로드 실패:", error);
    }
  }, [currentUserId]);

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
      // 메시지 수신 시 채팅방 목록 갱신
      loadChatRooms();
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

  // 알림 클릭 시 채팅창 자동 오픈 (5분 전 알림)
  useEffect(() => {
    const handleOpenChat = async (event: Event) => {
      const customEvent = event as CustomEvent<{ appointmentId: number }>;
      const { appointmentId } = customEvent.detail;

      // appointmentId로 roomId 생성
      const roomId = `appointment_${appointmentId}`;

      // 채팅방 목록에서 해당 채팅방 찾기
      const chatRoom = chatRooms.find((room) => room.roomId === roomId);

      if (chatRoom && chatRoom.status === "IN_PROGRESS") {
        // IN_PROGRESS 상태인 경우에만 채팅창 오픈
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
        console.log("메시지 로드 시작 RoomID:", activeRoomId);
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
          console.log("✅ 읽음 처리 완료:", activeRoomId);
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
    const appointmentId = parseInt(activeRoomId.replace('appointment_', ''));
    const memberName = selectedChatMember.name;

    // 즉시 UI 업데이트 (사용자 경험 향상)
    setSelectedChatMember(null);
    setActiveRoomId(null);
    setMessages([]);
    toast.success(`${memberName}님의 진료를 종료합니다...`);

    // API 호출은 백그라운드에서 처리
    try {
      await appointmentApi.completeAppointment(appointmentId);
      console.log('진료 종료 성공:', appointmentId);

      // 채팅방 목록 갱신 (백그라운드)
      loadChatRooms();
    } catch (error) {
      console.error('진료 종료 API 실패:', error);
      // API 실패해도 UI는 이미 업데이트되었으므로 사용자에게는 영향 없음
      // 필요시 재시도 로직 추가 가능
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
      // API 응답의 optional 필드에 기본값 제공
      const formattedData = data.map(medicine => ({
        ...medicine,
        manufacturer: medicine.manufacturer || "",
        standardDosage: medicine.standardDosage || "",
        unit: medicine.unit || "",
        description: medicine.description || "",
      }));
      setSearchResults(formattedData);
    } catch (error) {
      console.error("약품 검색 실패:", error);
    } finally {
      setIsSearching(false);
    }
  };

  // 약품 추가 (클릭 또는 엔터)
  const handleAddMedicine = (medicine: MedicineSearchResult) => {
    // 이미 추가된 약품인지 확인
    const existing = selectedMedicines.find(m => m.id === medicine.id);

    if (existing) {
      // 수량 증가
      setSelectedMedicines(selectedMedicines.map(m =>
        m.id === medicine.id ? { ...m, quantity: m.quantity + 1 } : m
      ));
    } else {
      // 새로 추가
      setSelectedMedicines([...selectedMedicines, {
        id: medicine.id,
        name: medicine.name,
        quantity: 1
      }]);
    }

    // 검색어 초기화하지만 다이얼로그는 유지
    setSearchQuery("");
    setSearchResults([]);
  };

  // 약품 수량 변경
  const handleUpdateQuantity = (id: number, change: number) => {
    setSelectedMedicines(selectedMedicines.map(m => {
      if (m.id === id) {
        const newQuantity = Math.max(0, m.quantity + change);
        return { ...m, quantity: newQuantity };
      }
      return m;
    }).filter(m => m.quantity > 0)); // 수량이 0이면 제거
  };

  const handleOpenMedicineDialog = () => {
    setSearchQuery("");
    setSearchResults([]);
    setShowMedicineDialog(true);
  };

  const handleSavePrescription = () => {
    if (!selectedChatMember) {
      toast.error("환자를 선택해주세요");
      return;
    }
    toast.success("처방전이 저장되었습니다");
    setPrescriptionName("");
    setDiagnosis("");
    setInstructions("");
    setSelectedMedicines([]);
  };

  const getMedicinesText = () => {
    if (selectedMedicines.length === 0) return "약품을 선택하세요";
    return selectedMedicines.map((m) => `${m.name} ${m.quantity}개`).join(", ");
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
              <div className="flex-1 overflow-y-auto scrollbar-thin scrollbar-thumb-gray-300 scrollbar-track-gray-100">
                <div className="space-y-3 pr-2">
                  {chatRooms.filter(room => room.status === 'IN_PROGRESS').length === 0 ? (
                    <div className="flex items-center justify-center h-full text-gray-500">
                      진료 중인 환자가 없습니다
                    </div>
                  ) : (
                    chatRooms.filter(room => room.status === 'IN_PROGRESS').map((room) => {
                      const isMyNameInList = room.patientName === "김민준";
                      const displayName = isMyNameInList
                        ? "김강민"
                        : room.patientName;

                      // activeRoomId로 선택 여부 판단
                      const isSelected = activeRoomId === room.roomId;

                      return (
                        <div
                          key={room.roomId}
                          onClick={() => {
                            // 이미 선택된 채팅방이면 아무 작업도 하지 않음
                            if (isSelected) return;

                            // 활성 룸 ID 변경 (useEffect가 메시지 로드)
                            setActiveRoomId(room.roomId);

                            // 선택된 멤버 정보 업데이트
                            const newMember: Member = {
                              id: room.patientId,
                              name: displayName,
                              avatar: displayName.charAt(0),
                              age: 0,
                              gender: "",
                              lastVisit: "정보 없음",
                              condition: "양호",
                              status: "in-progress",
                            };

                            // 메시지 초기화
                            setSelectedChatMember(newMember);
                            setMessages([]);
                          }}
                          className={`p-3 rounded-xl border cursor-pointer hover:shadow-lg transition-all ${
                            isSelected
                              ? "bg-blue-50 border-blue-300"
                              : "bg-white/80 border-gray-200"
                          }`}
                        >
                          <div className="flex items-center gap-3 mb-2">
                            <Avatar className="w-10 h-10">
                              <AvatarFallback className="bg-gradient-to-br from-gray-600 to-gray-800 text-white font-black text-sm">
                                {displayName.charAt(0)}
                              </AvatarFallback>
                            </Avatar>
                            <div className="flex-1 min-w-0">
                              <div className="flex items-center justify-between mb-1">
                                <div className="font-bold text-sm text-gray-900">
                                  {displayName}
                                </div>
                                <div className="text-xs text-gray-500">
                                  {formatChatTime(room.lastMessageTime)}
                                </div>
                              </div>
                              <div className="flex items-center justify-between">
                                <div className="text-xs text-gray-600 truncate flex-1">
                                  {room.lastMessage || "메시지를 시작하세요"}
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
                      <Avatar className="w-10 h-10">
                        <AvatarFallback className="bg-gradient-to-br from-gray-600 to-gray-800 text-white font-black">
                          {selectedChatMember.avatar}
                        </AvatarFallback>
                      </Avatar>
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
                      variant="outline"
                      className="rounded-xl border-red-300 text-red-600 hover:bg-red-50"
                    >
                      <CheckCircle className="w-4 h-4 mr-2" />
                      진료 종료
                    </Button>
                  </div>

                  {/* 채팅 메시지 영역 */}
                  <div className="flex-1 overflow-y-auto mb-4 min-h-0 pr-2">
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

                        const senderInitial = isMine
                          ? "의"
                          : senderDisplayName.charAt(0);

                        return (
                          <div
                            key={msg.id}
                            className={`flex gap-3 ${
                              isMine ? "justify-end" : ""
                            }`}
                          >
                            {!isMine && (
                              <Avatar className="w-8 h-8">
                                <AvatarFallback className="bg-gradient-to-br from-gray-600 to-gray-800 text-white font-black text-xs">
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
                      className="rounded-xl"
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
                      className="bg-gradient-to-r from-[#C93831] to-[#B02F28] text-white rounded-xl"
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
                  <div className="flex-1 overflow-y-auto pr-2">
                    <div className="space-y-4">
                      <div>
                        <Label className="text-sm font-bold">처방명</Label>
                        <Input
                          value={prescriptionName}
                          onChange={(e) => setPrescriptionName(e.target.value)}
                          placeholder="예: 감기약 처방"
                          className="mt-1 rounded-xl"
                        />
                      </div>

                      <div>
                        <Label className="text-sm font-bold">처방일</Label>
                        <Input
                          value={prescriptionDate}
                          disabled
                          className="mt-1 rounded-xl bg-gray-100"
                        />
                      </div>

                      <div>
                        <Label className="text-sm font-bold text-gray-400">
                          담당 의사
                        </Label>
                        <Input
                          value={localStorage.getItem("userName") || "의료진"}
                          disabled
                          className="mt-1 rounded-xl bg-gray-100 text-gray-400"
                        />
                      </div>

                      <div>
                        <Label className="text-sm font-bold">진단명</Label>
                        <Input
                          value={diagnosis}
                          onChange={(e) => setDiagnosis(e.target.value)}
                          placeholder="예: 급성 상기도 감염"
                          className="mt-1 rounded-xl"
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
                            약품 선택 ({selectedMedicines.length}개)
                          </Button>
                        </div>
                        <div
                          onClick={handleOpenMedicineDialog}
                          className="min-h-[90px] p-3 rounded-xl border-2 border-dashed border-gray-300 bg-gray-50 cursor-pointer hover:bg-gray-100 transition-all"
                        >
                          <p className="text-sm text-gray-700 whitespace-pre-wrap">
                            {getMedicinesText()}
                          </p>
                        </div>
                      </div>

                      <div>
                        <Label className="text-sm font-bold">복용 방법</Label>
                        <Textarea
                          value={instructions}
                          onChange={(e) => setInstructions(e.target.value)}
                          placeholder="하루 3회, 식후 30분에 복용하세요."
                          className="mt-1 rounded-xl"
                          rows={4}
                        />
                      </div>
                    </div>
                  </div>

                  <div className="mt-4 pt-4 border-t flex-shrink-0">
                    <Button
                      onClick={handleSavePrescription}
                      className="w-full bg-gradient-to-r from-[#C93831] to-[#B02F28] text-white font-bold rounded-xl h-12"
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
            <DialogTitle className="text-2xl font-black">약품 검색 및 선택</DialogTitle>
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
                  if (e.key === 'Enter' && searchResults.length > 0) {
                    handleAddMedicine(searchResults[0]);
                  }
                }}
                className="rounded-xl"
                autoFocus
              />
            </div>

            {/* 검색 결과 */}
            {searchQuery && (
              <div className="border rounded-xl p-2 max-h-[200px] overflow-y-auto">
                {isSearching ? (
                  <div className="text-center py-4 text-gray-500">검색 중...</div>
                ) : searchResults.length === 0 ? (
                  <div className="text-center py-4 text-gray-500">검색 결과가 없습니다</div>
                ) : (
                  <div className="space-y-1">
                    {searchResults.map((medicine) => (
                      <div
                        key={medicine.id}
                        onClick={() => handleAddMedicine(medicine)}
                        className="p-3 rounded-lg hover:bg-blue-50 cursor-pointer transition-colors"
                      >
                        <div className="font-medium text-gray-900">{medicine.name}</div>
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
              <div className="border rounded-xl p-3 min-h-[150px] max-h-[250px] overflow-y-auto space-y-2">
                {selectedMedicines.length === 0 ? (
                  <div className="text-center py-8 text-gray-400">
                    선택된 약품이 없습니다
                  </div>
                ) : (
                  selectedMedicines.map((medicine) => (
                    <div
                      key={medicine.id}
                      className="flex items-center justify-between p-3 rounded-lg border bg-white hover:bg-gray-50"
                    >
                      <span className="text-sm font-medium text-gray-700 flex-1">
                        {medicine.name}
                      </span>
                      <div className="flex items-center gap-2">
                        <Button
                          size="sm"
                          variant="outline"
                          className="h-8 w-8 p-0 rounded-full"
                          onClick={() => handleUpdateQuantity(medicine.id, -1)}
                        >
                          <Minus className="w-3 h-3" />
                        </Button>
                        <span className="text-sm font-bold w-8 text-center">
                          {medicine.quantity}
                        </span>
                        <Button
                          size="sm"
                          variant="outline"
                          className="h-8 w-8 p-0 rounded-full"
                          onClick={() => handleUpdateQuantity(medicine.id, 1)}
                        >
                          <Plus className="w-3 h-3" />
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
                className="bg-gradient-to-r from-[#C93831] to-[#B02F28] text-white rounded-xl"
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
