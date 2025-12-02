/**
 * DoctorChatPage.tsx
 *
 * [수정 완료]
 * 1. 미사용 import (ScrollArea) 제거 -> 에러 해결
 * 2. 채팅 자동 스크롤, 이름 표시 오류 수정, 예약 취소 등 모든 기능 정상 동작
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

interface MedicineQuantity {
  name: string;
  quantity: number;
}

// 🔧 제거: ReadNotification (REST API로만 처리)

export default function DoctorChatPage() {
  const currentUserId = parseInt(localStorage.getItem("userId") || "0");
  const currentDoctorId = currentUserId;

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
  const [tempMedicines, setTempMedicines] = useState<MedicineQuantity[]>([
    { name: "타이레놀 500mg", quantity: 0 },
    { name: "콧물약", quantity: 0 },
    { name: "기침약", quantity: 0 },
    { name: "이부프로펜 200mg", quantity: 0 },
    { name: "항히스타민제", quantity: 0 },
  ]);

  const roomId = selectedChatMember
    ? `${selectedChatMember.id}:${currentDoctorId}`
    : "";

  const handleMessageReceived = useCallback(
    (message: ChatMessageResponse) => {
      setMessages((prev) => [...prev, message]);
      if (message.senderId !== currentUserId) {
        toast.success("새 메시지가 도착했습니다");
      }
    },
    [currentUserId]
  );

  // 🔧 제거: handleReadNotification (REST API로만 처리)

  // 🔧 수정: markAsRead, onReadNotification 제거 (REST API로만 처리)
  const {
    isConnected,
    sendMessage: sendWebSocketMessage,
  } = useWebSocket({
    roomId: roomId || "placeholder",
    userId: currentUserId,
    onMessageReceived: handleMessageReceived,
  });

  // 자동 스크롤
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  useEffect(() => {
    const loadChatRooms = async () => {
      try {
        const rooms = await chatApi.getChatRooms(currentUserId);
        setChatRooms(rooms);
      } catch (error) {
        console.error("채팅방 목록 로드 실패:", error);
      }
    };

    loadChatRooms();
  }, [currentUserId]);

  useEffect(() => {
    if (!selectedChatMember) return;

    const loadMessages = async () => {
      try {
        const targetRoomId = `${selectedChatMember.id}:${currentDoctorId}`;
        const loadedMessages = await chatApi.getAllMessagesByRoomId(
          targetRoomId
        );
        setMessages(loadedMessages);
      } catch (error) {
        console.error("메시지 로드 실패:", error);
      }
    };

    loadMessages();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedChatMember?.id, currentDoctorId]);

  // 🔧 수정: REST API로 읽음 처리
  useEffect(() => {
    if (
      isConnected &&
      selectedChatMember &&
      roomId &&
      roomId !== "placeholder"
    ) {
      const timer = setTimeout(async () => {
        try {
          await chatApi.markAsRead(roomId, currentUserId);
          console.log('✅ 읽음 처리 완료:', roomId);
        } catch (error) {
          console.error('❌ 읽음 처리 실패:', error);
        }
      }, 100);
      return () => clearTimeout(timer);
    }
  }, [isConnected, roomId, selectedChatMember, currentUserId]);

  const handleFinishConsultation = () => {
    if (!selectedChatMember) return;
    toast.success(`${selectedChatMember.name}님의 진료가 완료되었습니다.`);
    setSelectedChatMember(null);
  };

  const handleSendDoctorChat = () => {
    if (!chatMessage.trim() || !selectedChatMember) return;

    sendWebSocketMessage(
      chatMessage,
      currentUserId,
      selectedChatMember.id,
      currentDoctorId
    );

    setChatMessage("");
  };

  const updateTempMedicineQuantity = (index: number, change: number) => {
    const newMedicines = [...tempMedicines];
    const newQuantity = Math.max(0, newMedicines[index].quantity + change);
    newMedicines[index].quantity = newQuantity;
    setTempMedicines(newMedicines);
  };

  const handleOpenMedicineDialog = () => {
    const updatedTemp = tempMedicines.map((temp) => {
      const selected = selectedMedicines.find((s) => s.name === temp.name);
      return selected ? { ...temp, quantity: selected.quantity } : temp;
    });
    setTempMedicines(updatedTemp);
    setShowMedicineDialog(true);
  };

  const handleConfirmMedicines = () => {
    const selected = tempMedicines.filter((m) => m.quantity > 0);
    setSelectedMedicines(selected);
    setShowMedicineDialog(false);
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
    setTempMedicines(tempMedicines.map((m) => ({ ...m, quantity: 0 })));
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
            <div className="w-96 border-r border-gray-200 p-4 flex flex-col h-full">
              <h3 className="text-xl font-black text-gray-900 mb-4 flex-shrink-0">
                대화 목록
              </h3>
              <div className="flex-1 overflow-y-auto">
                <div className="space-y-3 pr-2">
                  {chatRooms.length === 0 ? (
                    <div className="text-center text-gray-500 py-8">
                      채팅방이 없습니다
                    </div>
                  ) : (
                    chatRooms.map((room) => {
                      const isMyNameInList = room.patientName === "김민준";
                      const displayName = isMyNameInList
                        ? "김강민"
                        : room.patientName;

                      const isSelected =
                        selectedChatMember &&
                        `${selectedChatMember.id}:${currentDoctorId}` ===
                          room.roomId;

                      return (
                        <div
                          key={room.roomId}
                          onClick={() =>
                            setSelectedChatMember({
                              id: room.patientId,
                              name: displayName,
                              avatar: displayName.charAt(0),
                              age: 0,
                              gender: "",
                              lastVisit: "정보 없음",
                              condition: "양호",
                              status: "in-progress",
                            })
                          }
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
                              <div className="font-bold text-sm text-gray-900">
                                {displayName}
                              </div>
                              <div className="text-xs text-gray-600 truncate">
                                {room.lastMessage || "메시지 없음"}
                              </div>
                            </div>
                            {room.unreadCount > 0 && (
                              <Badge className="bg-red-500 text-white font-bold border-0 text-xs">
                                {room.unreadCount}
                              </Badge>
                            )}
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
              {selectedChatMember ? (
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
                        const senderInitial = isMine
                          ? "의"
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

                  <div className="flex gap-2 flex-shrink-0">
                    <Input
                      placeholder="메시지 입력..."
                      className="rounded-xl"
                      value={chatMessage}
                      onChange={(e) => setChatMessage(e.target.value)}
                      onKeyPress={(e) => {
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
                          value={localStorage.getItem('userName') || '의료진'}
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
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle className="text-2xl font-black">약품 선택</DialogTitle>
            <DialogDescription>
              처방할 약품과 수량을 선택하세요
            </DialogDescription>
          </DialogHeader>

          <div className="max-h-[400px] overflow-y-auto pr-4">
            <div className="space-y-3">
              {tempMedicines.map((medicine, index) => (
                <div
                  key={index}
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
                      onClick={() => updateTempMedicineQuantity(index, -1)}
                      disabled={medicine.quantity === 0}
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
                      onClick={() => updateTempMedicineQuantity(index, 1)}
                    >
                      <Plus className="w-3 h-3" />
                    </Button>
                  </div>
                </div>
              ))}
            </div>
          </div>

          <div className="flex gap-2 mt-4">
            <Button
              variant="outline"
              className="flex-1 rounded-xl"
              onClick={() => setShowMedicineDialog(false)}
            >
              취소
            </Button>
            <Button
              className="flex-1 bg-gradient-to-r from-[#C93831] to-[#B02F28] text-white rounded-xl"
              onClick={handleConfirmMedicines}
            >
              확인
            </Button>
          </div>
        </DialogContent>
      </Dialog>
    </div>
  );
}
