/**
 * Medical.tsx
 *
 * 진료 관리 페이지 컴포넌트 (2단 레이아웃)
 * - 좌측: 예약 내역 및 처방전 조회
 * - 우측: 실시간 채팅
 */

import { useState } from "react";
import { Card } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Input } from "@/components/ui/input";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import {
  Calendar as CalendarIcon,
  Clock,
  FileText,
  XCircle,
  Send,
} from "lucide-react";
import { Prescription, ChatMessage } from "@/types/dashboard.types";

interface MedicalProps {
  setShowAppointment: (show: boolean) => void;
  setShowChat: (show: boolean) => void;
  setSelectedPrescription: (prescription: Prescription | null) => void;
}

export default function Medical({
  setShowAppointment,
  setSelectedPrescription,
}: MedicalProps) {
  const [chatMessage, setChatMessage] = useState("");
  const [chatMessages, setChatMessages] = useState<ChatMessage[]>([
    {
      id: 1,
      author: "김의사",
      avatar: "김",
      content: "안녕하세요! 무엇을 도와드릴까요?",
      time: "오전 10:00",
      isMine: false,
    },
  ]);

  const handleSendMessage = () => {
    if (!chatMessage.trim()) return;

    const newMsg: ChatMessage = {
      id: Date.now(),
      author: "김루핀",
      avatar: "김",
      content: chatMessage,
      time: new Date().toLocaleTimeString("ko-KR", {
        hour: "2-digit",
        minute: "2-digit",
      }),
      isMine: true,
    };

    setChatMessages([...chatMessages, newMsg]);
    setChatMessage("");
  };

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

  // 예정된 예약이 있는지 확인
  const hasActiveAppointment = appointments.some(
    (apt) => apt.status === "예정"
  );

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
            {/* 예약 내역 */}
            <Card className="backdrop-blur-2xl bg-white/60 border border-gray-200 shadow-xl h-[calc((100vh-216px)/2)] flex flex-col overflow-hidden">
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

            {/* 처방전 */}
            <Card className="backdrop-blur-2xl bg-white/60 border border-gray-200 shadow-xl h-[calc((100vh-216px)/2)] flex flex-col overflow-hidden">
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

          {/* 우측: 채팅 */}
          <Card className="flex-1 backdrop-blur-2xl bg-white/60 border border-gray-200 shadow-2xl">
            <div className="h-full flex flex-col p-6">
              {hasActiveAppointment ? (
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

                  <ScrollArea className="flex-1 mb-4">
                    <div className="space-y-4">
                      {chatMessages.map((msg) => (
                        <div
                          key={msg.id}
                          className={`flex gap-3 ${
                            msg.isMine ? "justify-end" : ""
                          }`}
                        >
                          {!msg.isMine && (
                            <Avatar className="w-8 h-8">
                              <AvatarFallback className="bg-gradient-to-br from-blue-600 to-blue-800 text-white font-black text-xs">
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
                      ))}
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
                <div className="flex items-center justify-center h-full">
                  <Card className="backdrop-blur-2xl bg-white/60 border border-gray-200 shadow-xl hover:shadow-2xl transition-all max-w-md">
                    <div className="p-8 text-center">
                      <div className="w-20 h-20 bg-gradient-to-br from-[#C93831] to-[#B02F28] rounded-3xl flex items-center justify-center shadow-xl mx-auto mb-6">
                        <CalendarIcon className="w-10 h-10 text-white" />
                      </div>
                      <h3 className="text-2xl font-black text-gray-900 mb-3">
                        새 진료 예약
                      </h3>
                      <p className="text-gray-600 font-medium mb-6">
                        의료진과 비대면 상담을 시작하세요
                      </p>
                      <Button
                        onClick={() => setShowAppointment(true)}
                        className="bg-gradient-to-r from-[#C93831] to-[#B02F28] hover:from-[#B02F28] hover:to-[#C93831] text-white font-bold px-8 py-3 rounded-2xl w-full"
                      >
                        예약하기
                      </Button>
                    </div>
                  </Card>
                </div>
              )}
            </div>
          </Card>
        </div>
      </div>
    </div>
  );
}
