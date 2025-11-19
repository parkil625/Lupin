/**
 * DoctorChatPage.tsx
 *
 * 의사 채팅 & 처방전 작성 페이지
 * - 대화 목록 + 채팅창 + 처방전 작성 (3단 레이아웃)
 */

import { useState } from "react";
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
import {
  Send,
  CheckCircle,
  Calendar as CalendarIcon,
  FileText,
  Plus,
  Minus,
  Edit2,
} from "lucide-react";
import { toast } from "sonner";
import { Member, ChatMessage } from "@/types/dashboard.types";
import { members, initialDoctorChats, appointments } from "@/mockdata/members";

interface MedicineQuantity {
  name: string;
  quantity: number;
}

export default function DoctorChatPage() {
  const [selectedChatMember, setSelectedChatMember] = useState<Member | null>(
    null
  );
  const [chatMessages, setChatMessages] = useState<{
    [key: number]: ChatMessage[];
  }>(initialDoctorChats);
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

  const handleSendDoctorChat = () => {
    if (!chatMessage.trim() || !selectedChatMember) return;

    const newMsg: ChatMessage = {
      id: Date.now(),
      author: "김의사",
      avatar: "의",
      content: chatMessage,
      time: new Date().toLocaleTimeString("ko-KR", {
        hour: "2-digit",
        minute: "2-digit",
      }),
      isMine: true,
    };

    const memberChats = chatMessages[selectedChatMember.id] || [];
    setChatMessages({
      ...chatMessages,
      [selectedChatMember.id]: [...memberChats, newMsg],
    });
    setChatMessage("");
  };

  const updateTempMedicineQuantity = (index: number, change: number) => {
    const newMedicines = [...tempMedicines];
    const newQuantity = Math.max(0, newMedicines[index].quantity + change);
    newMedicines[index].quantity = newQuantity;
    setTempMedicines(newMedicines);
  };

  const handleOpenMedicineDialog = () => {
    // 현재 선택된 약을 tempMedicines에 반영
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

    // 여기서 처방전 저장 로직
    toast.success("처방전이 저장되었습니다");

    // 폼 초기화
    setPrescriptionName("");
    setDiagnosis("");
    setInstructions("");
    setSelectedMedicines([]);
    setTempMedicines(tempMedicines.map((m) => ({ ...m, quantity: 0 })));
  };

  // 회원의 예약 정보 찾기
  const getMemberAppointment = (memberName: string) => {
    return appointments.find((apt) => apt.memberName === memberName);
  };

  // 약품 텍스트 생성
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
            {/* 좌측: 대화 목록 (예약 정보 포함) */}
            <div className="w-96 border-r border-gray-200 p-4">
              <h3 className="text-xl font-black text-gray-900 mb-4">
                대화 목록
              </h3>
              <ScrollArea className="h-[calc(100vh-280px)]">
                <div className="space-y-3">
                  {members.slice(0, 4).map((member) => {
                    const appointment = getMemberAppointment(member.name);
                    return (
                      <div
                        key={member.id}
                        onClick={() => setSelectedChatMember(member)}
                        className={`p-3 rounded-xl border cursor-pointer hover:shadow-lg transition-all ${
                          selectedChatMember?.id === member.id
                            ? "bg-blue-50 border-blue-300"
                            : "bg-white/80 border-gray-200"
                        }`}
                      >
                        <div className="flex items-center gap-3 mb-2">
                          <Avatar className="w-10 h-10">
                            <AvatarFallback className="bg-gradient-to-br from-gray-600 to-gray-800 text-white font-black text-sm">
                              {member.avatar}
                            </AvatarFallback>
                          </Avatar>
                          <div className="flex-1 min-w-0">
                            <div className="font-bold text-sm text-gray-900">
                              {member.name}
                            </div>
                            <div className="text-xs text-gray-600 truncate">
                              {chatMessages[member.id]?.[
                                chatMessages[member.id].length - 1
                              ]?.content || "메시지 없음"}
                            </div>
                          </div>
                          {chatMessages[member.id] &&
                            chatMessages[member.id].length > 0 && (
                              <div className="w-2 h-2 bg-red-500 rounded-full flex-shrink-0"></div>
                            )}
                        </div>

                        {/* 예약 정보 표시 */}
                        {appointment && (
                          <div className="mt-2 pt-2 border-t border-gray-200">
                            <div className="flex items-center justify-between mb-1">
                              <div className="flex items-center gap-2">
                                <CalendarIcon className="w-3 h-3 text-gray-600" />
                                <span className="text-xs text-gray-700">
                                  {appointment.date} {appointment.time}
                                </span>
                              </div>
                              <div className="flex items-center gap-2">
                                <span className="text-xs text-gray-600">
                                  {appointment.department}
                                </span>
                                <Badge
                                  className={`${
                                    appointment.status === "scheduled"
                                      ? "bg-blue-500"
                                      : "bg-green-500"
                                  } text-white font-bold border-0 text-xs`}
                                >
                                  {appointment.status === "scheduled"
                                    ? "예약"
                                    : "완료"}
                                </Badge>
                              </div>
                            </div>
                            <div className="text-xs text-gray-600 mt-1">
                              {appointment.reason}
                            </div>
                          </div>
                        )}
                      </div>
                    );
                  })}
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
                        <div className="text-xs text-gray-600">온라인</div>
                      </div>
                    </div>
                    <Button
                      onClick={() => {
                        toast.success("진료가 종료되었습니다.");
                      }}
                      variant="outline"
                      className="rounded-xl border-red-300 text-red-600 hover:bg-red-50"
                    >
                      <CheckCircle className="w-4 h-4 mr-2" />
                      진료 종료
                    </Button>
                  </div>

                  <ScrollArea className="flex-1 mb-4">
                    <div className="space-y-4">
                      {(chatMessages[selectedChatMember.id] || []).map(
                        (msg) => (
                          <div
                            key={msg.id}
                            className={`flex gap-3 ${
                              msg.isMine ? "justify-end" : ""
                            }`}
                          >
                            {!msg.isMine && (
                              <Avatar className="w-8 h-8">
                                <AvatarFallback className="bg-gradient-to-br from-gray-600 to-gray-800 text-white font-black text-xs">
                                  {msg.avatar}
                                </AvatarFallback>
                              </Avatar>
                            )}
                            <div
                              className={`rounded-2xl p-3 max-w-md ${
                                msg.isMine
                                  ? "bg-[#C93831] text-white"
                                  : "bg-gray-100"
                              }`}
                            >
                              {!msg.isMine && (
                                <div className="font-bold text-xs text-gray-900 mb-1">
                                  {msg.author}
                                </div>
                              )}
                              <div className="text-sm">{msg.content}</div>
                              <div
                                className={`text-xs mt-1 ${
                                  msg.isMine ? "text-white/80" : "text-gray-500"
                                }`}
                              >
                                {msg.time}
                              </div>
                            </div>
                          </div>
                        )
                      )}
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
