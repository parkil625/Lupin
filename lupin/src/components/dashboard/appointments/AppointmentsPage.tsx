/**
 * AppointmentsPage.tsx
 *
 * 예약 관리 페이지 컴포넌트 (의사용)
 * - 예정된 예약 목록 표시
 * - 회원과의 채팅 시작
 * - 예약 상태 관리
 */

import { Card } from "@/components/ui/card";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Calendar as CalendarIcon, FileText, MessageCircle, XCircle } from "lucide-react";
import { Appointment } from "@/types/dashboard.types";
import { appointments } from "@/mockdata/members";

interface AppointmentsPageProps {
  onChatClick: () => void;
}

export default function AppointmentsPage({ onChatClick }: AppointmentsPageProps) {
  return (
    <div className="h-full overflow-auto p-8">
      <div className="max-w-7xl mx-auto space-y-6">
        <div>
          <h1 className="text-5xl font-black text-gray-900 mb-2">예약 관리</h1>
          <p className="text-gray-700 font-medium text-lg">회원 예약 현황</p>
        </div>

        <div className="grid lg:grid-cols-2 gap-6">
          {appointments.map((apt) => (
            <Card
              key={apt.id}
              className="backdrop-blur-2xl bg-white/60 border border-gray-200 shadow-xl"
            >
              <div className="p-6">
                <div className="flex items-start justify-between mb-4">
                  <div className="flex items-center gap-4">
                    <Avatar className="w-12 h-12 border-2 border-white shadow-lg">
                      <AvatarFallback className="bg-gradient-to-br from-gray-600 to-gray-800 text-white font-black">
                        {apt.memberAvatar}
                      </AvatarFallback>
                    </Avatar>
                    <div>
                      <h3 className="text-xl font-black text-gray-900">{apt.memberName}</h3>
                      <div className="text-sm text-gray-600 font-medium">{apt.department}</div>
                    </div>
                  </div>
                  <Badge className={`${
                    apt.status === "scheduled" ? "bg-blue-500" :
                    apt.status === "completed" ? "bg-green-500" :
                    "bg-gray-500"
                  } text-white font-bold border-0`}>
                    {apt.status === "scheduled" ? "예정" :
                     apt.status === "completed" ? "완료" :
                     "취소"}
                  </Badge>
                </div>

                <div className="space-y-2 mb-4">
                  <div className="flex items-center gap-2 text-gray-700 font-medium">
                    <CalendarIcon className="w-4 h-4" />
                    {apt.date} {apt.time}
                  </div>
                  <div className="flex items-center gap-2 text-gray-700 font-medium">
                    <FileText className="w-4 h-4" />
                    {apt.reason}
                  </div>
                </div>

                {apt.status === "scheduled" && (
                  <div className="flex gap-2">
                    <Button
                      variant="outline"
                      className="flex-1 rounded-xl border-blue-300 text-blue-600 hover:bg-blue-50"
                      onClick={onChatClick}
                    >
                      <MessageCircle className="w-4 h-4 mr-2" />
                      채팅
                    </Button>
                    <Button
                      variant="outline"
                      className="flex-1 rounded-xl border-red-300 text-red-600 hover:bg-red-50"
                    >
                      <XCircle className="w-4 h-4 mr-2" />
                      취소
                    </Button>
                  </div>
                )}
              </div>
            </Card>
          ))}
        </div>
      </div>
    </div>
  );
}
