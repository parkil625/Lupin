/**
 * DoctorChatPage.tsx
 *
 * [수정 내용]
 * 1. 상태 관리 간소화: 'waiting' 제거 -> 'in-progress'(진료 중) 와 'completed'(완료됨) 만 사용
 * 2. [진료 종료] 버튼 클릭 시: '완료됨' 처리 후 채팅창 닫기 로직 추가
 */

import { useState, useEffect } from "react";
import { Card } from "@/components/ui/card";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { ScrollArea } from "@/components/ui/scroll-area";
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

export default function DoctorChatPage() {
  // 현재 로그인한 의사 정보
  const currentDoctorId = 2;
  const currentUserId = 2;

  const [selectedChatMember, setSelectedChatMember] = useState<Member | null>(
    null
  );
  const [chatRooms, setChatRooms] = useState<ChatRoomResponse[]>([]);
  const [messages, setMessages] = useState<ChatMessageResponse[]>([]);
  const [chatMessage, setChatMessage] = useState("");
  const [showMedicineDialog, setShowMedicineDialog] = useState(false);

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

  // WebSocket 연결
  const roomId = selectedChatMember
    ? `${selectedChatMember.id}:${currentDoctorId}`
    : "";

  const {
    isConnected,
    sendMessage: sendWebSocketMessage,
    markAsRead,
  } = useWebSocket({
    roomId: roomId || "placeholder",
    userId: currentUserId,
    onMessageReceived: (message: ChatMessageResponse) => {
      setMessages((prev) => [...prev, message]);
      if (message.senderId !== currentUserId) {
        toast.success("새 메시지가 도착했습니다");
      }
    },
    onReadNotification: (notification) => {
      console.log("상대방이 메시지를 읽었습니다:", notification);
    },
  });

  // 채팅방 목록 로드
  useEffect(() => {
    const loadChatRooms = async () => {
      try {
        const rooms = await chatApi.getChatRoomsByUserId(currentUserId);
        setChatRooms(rooms);
      } catch (error) {
        console.error("채팅방 목록 로드 실패:", error);
      }
    };

    loadChatRooms();
  }, [currentUserId]);

  // 메시지 로드 (HTTP 요청)
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

  // 읽음 처리 전용 useEffect
  useEffect(() => {
    if (isConnected && selectedChatMember && roomId) {
      const timer = setTimeout(() => {
        markAsRead();
      }, 100);
      return () => clearTimeout(timer);
    }
  }, [isConnected, roomId, markAsRead, selectedChatMember]);

  // ✅ [진료 종료] 버튼 핸들러
  const handleFinishConsultation = () => {
    if (!selectedChatMember) return;

    // 1. 실제로는 여기서 API를 호출하여 DB 상태를 'completed'로 변경해야 함
    // await chatApi.completeConsultation(selectedChatMember.id);

    // 2. UI 처리: 완료 메시지 출력 및 채팅창 닫기
    toast.success(`${selectedChatMember.name}님의 진료가 완료되었습니다.`);
    setSelectedChatMember(null); // 현재 선택된 환자 해제 (채팅창 닫힘)

    // 필요하다면 목록 갱신 로직 추가
    // loadChatRooms();
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

    if (!prescriptionName || !diagnosis || selectedMedicines.length === 0) {
      toast.error("필수 항목을 입력해주세요");
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
    <div className="h-full overflow-auto p-8">
      <div className="max-w-[1200px] mx-auto">
        <div>
          <h1 className="text-5xl font-black text-gray-900 mb-6">
            채팅 & 처방전 작성
          </h1>
        </div>

        <Card className="backdrop-blur-2xl bg-white/60 border border-gray-200 shadow-2xl h-[calc(100vh-200px)] mx-auto">
          <div className="h-full flex">
            {/* 좌측: 대화 목록 */}
            <div className="w-96 border-r border-gray-200 p-4">
              <h3 className="text-xl font-black text-gray-900 mb-4">
                대화 목록
              </h3>
              <ScrollArea className="h-[calc(100vh-280px)]">
                <div className="space-y-3">
                  {chatRooms.length === 0 ? (
                    <div className="text-center text-gray-500 py-8">
                      채팅방이 없습니다
                    </div>
                  ) : (
                    chatRooms.map((room) => {
                      const patientName = room.patientName;
                      const isSelected =
                        selectedChatMember &&
                        `${selectedChatMember.id}:${currentDoctorId}` ===
                          room.roomId;

                      return (
                        <div
                          key={room.roomId}
                          onClick={() =>
                            // ✅ 목록 클릭 시 무조건 "진료 중(in-progress)" 상태로 설정
                            setSelectedChatMember({
                              id: room.patientId,
                              name: patientName,
                              avatar: patientName.charAt(0),
                              age: 0,
                              gender: "",
                              lastVisit: "정보 없음",
                              condition: "양호",
                              status: "in-progress", // 대기 상태 없이 바로 진료 중으로 시작
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
                                {patientName.charAt(0)}
                              </AvatarFallback>
                            </Avatar>
                            <div className="flex-1 min-w-0">
                              <div className="font-bold text-sm text-gray-900">
                                {patientName}
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
              </ScrollArea>
            </div>

            {/* 중앙: 채팅 영역 */}
            <div className="flex-1 flex flex-col p-6 border-r border-gray-200">
              {selectedChatMember ? (
                <>
                  <div className="flex items-center justify-between pb-4 border-b border-gray-200 mb-4">
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
                        {/* 상태 표시도 단순화 */}
                        <div className="text-xs text-gray-600 flex items-center gap-1">
                          <span className="w-2 h-2 rounded-full bg-green-500"></span>
                          진료 중
                        </div>
                      </div>
                    </div>
                    {/* ✅ 진료 종료 버튼 연결 */}
                    <Button
                      onClick={handleFinishConsultation}
                      variant="outline"
                      className="rounded-xl border-red-300 text-red-600 hover:bg-red-50"
                    >
                      <CheckCircle className="w-4 h-4 mr-2" />
                      진료 종료
                    </Button>
                  </div>

                  <ScrollArea className="flex-1 mb-4">
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
                    </div>
                  </ScrollArea>

                  <div className="flex gap-2">
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
            <div className="w-96 px-6 py-6 flex flex-col">
              <h3 className="text-xl font-black text-gray-900 mb-4 flex items-center gap-2">
                <FileText className="w-5 h-5 text-[#C93831]" />
                처방전 작성
              </h3>

              {selectedChatMember ? (
                <>
                  <ScrollArea className="flex-1 pr-2">
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
                          value="김의사"
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
                  </ScrollArea>

                  <div className="mt-4 pt-4 border-t">
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

          <ScrollArea className="max-h-[400px] pr-4">
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
          </ScrollArea>

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
