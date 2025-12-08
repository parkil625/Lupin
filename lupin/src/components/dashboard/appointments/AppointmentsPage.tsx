/**
 * AppointmentsPage.tsx
 *
 * 예약 관리 페이지 컴포넌트 (의사용)
 * - 예정된 예약 목록 표시
 * - 회원과의 채팅 시작
 * - 예약 상태 관리
 */

import { useState, useEffect } from "react";
import { Card } from "@/components/ui/card";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Calendar as CalendarIcon,
  FileText,
  MessageCircle,
  XCircle,
  User,
} from "lucide-react";
import { appointmentApi, AppointmentResponse } from "@/api/appointmentApi";
import ChatRoom from "@/components/dashboard/chat/ChatRoom";

interface AppointmentsPageProps {
  currentUser: { id: number; name: string; role: 'DOCTOR' | 'PATIENT' };
}

export default function AppointmentsPage({
  currentUser,
}: AppointmentsPageProps) {
  const [appointments, setAppointments] = useState<AppointmentResponse[]>([]);
  const [selectedAppointment, setSelectedAppointment] = useState<AppointmentResponse | null>(null);
  const [isChatOpen, setIsChatOpen] = useState(false);

  // 예약 목록 불러오기
  useEffect(() => {
    if (currentUser.role === 'DOCTOR') {
      appointmentApi
        .getDoctorAppointments(currentUser.id)
        .then(setAppointments)
        .catch((err) => console.error('예약 목록 로드 실패:', err));
    }
  }, [currentUser.id, currentUser.role]);

  // 채팅 시작 핸들러
  const handleChatClick = (appointment: AppointmentResponse) => {
    setSelectedAppointment(appointment);
    setIsChatOpen(true);
  };

  // 예약 취소 핸들러
  const handleCancelAppointment = async (appointmentId: number) => {
    if (!confirm('예약을 취소하시겠습니까?')) return;

    try {
      await appointmentApi.cancelAppointment(appointmentId);
      setAppointments((prev) =>
        prev.map((apt) =>
          apt.id === appointmentId ? { ...apt, status: 'CANCELLED' } : apt
        )
      );
      alert('예약이 취소되었습니다.');
    } catch (error) {
      console.error('예약 취소 실패:', error);
      alert('예약 취소에 실패했습니다.');
    }
  };
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
                      <AvatarFallback className="bg-white">
                        <User className="w-6 h-6 text-gray-400" />
                      </AvatarFallback>
                    </Avatar>
                    <div>
                      <h3 className="text-xl font-black text-gray-900">
                        환자 #{apt.patientId}
                      </h3>
                      <div className="text-sm text-gray-600 font-medium">
                        예약 번호: {apt.id}
                      </div>
                    </div>
                  </div>
                  <Badge
                    className={`${
                      apt.status === "SCHEDULED"
                        ? "bg-blue-500"
                        : apt.status === "COMPLETED"
                        ? "bg-green-500"
                        : apt.status === "CANCELLED"
                        ? "bg-gray-500"
                        : "bg-yellow-500"
                    } text-white font-bold border-0`}
                  >
                    {apt.status === "SCHEDULED"
                      ? "예정"
                      : apt.status === "COMPLETED"
                      ? "완료"
                      : apt.status === "CANCELLED"
                      ? "취소"
                      : "진행중"}
                  </Badge>
                </div>

                <div className="space-y-2 mb-4">
                  <div className="flex items-center gap-2 text-gray-700 font-medium">
                    <CalendarIcon className="w-4 h-4" />
                    {new Date(apt.date).toLocaleString('ko-KR', {
                      year: 'numeric',
                      month: 'long',
                      day: 'numeric',
                      hour: '2-digit',
                      minute: '2-digit',
                    })}
                  </div>
                  <div className="flex items-center gap-2 text-gray-700 font-medium">
                    <FileText className="w-4 h-4" />
                    의사: #{apt.doctorId} / 환자: #{apt.patientId}
                  </div>
                </div>

                {apt.status === "SCHEDULED" && (
                  <div className="flex gap-2">
                    <Button
                      variant="outline"
                      className="flex-1 rounded-xl border-blue-300 text-blue-600 hover:bg-blue-50"
                      onClick={() => handleChatClick(apt)}
                    >
                      <MessageCircle className="w-4 h-4 mr-2" />
                      채팅
                    </Button>
                    <Button
                      variant="outline"
                      className="flex-1 rounded-xl border-red-300 text-red-600 hover:bg-red-50"
                      onClick={() => handleCancelAppointment(apt.id)}
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

      {/* 채팅방 다이얼로그 */}
      {selectedAppointment && (
        <ChatRoom
          open={isChatOpen}
          onOpenChange={setIsChatOpen}
          appointmentId={selectedAppointment.id}
          currentUser={currentUser}
          targetUser={{
            id:
              currentUser.role === 'DOCTOR'
                ? selectedAppointment.patientId
                : selectedAppointment.doctorId,
            name:
              currentUser.role === 'DOCTOR'
                ? `환자 #${selectedAppointment.patientId}`
                : `의사 #${selectedAppointment.doctorId}`,
          }}
        />
      )}
    </div>
  );
}
