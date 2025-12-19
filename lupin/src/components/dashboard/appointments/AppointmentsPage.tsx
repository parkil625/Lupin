import { useState, useEffect, useCallback } from "react";
import { Card } from "@/components/ui/card";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
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
  currentUser: { id: number; name: string; role: "DOCTOR" | "PATIENT" };
}

export default function AppointmentsPage({
  currentUser,
}: AppointmentsPageProps) {
  const [appointments, setAppointments] = useState<AppointmentResponse[]>([]);
  const [selectedAppointment, setSelectedAppointment] =
    useState<AppointmentResponse | null>(null);
  const [isChatOpen, setIsChatOpen] = useState(false);
  const [statusFilter, setStatusFilter] = useState<string>("ALL");

  // 예약 목록 불러오기
  useEffect(() => {
    const fetchAppointments = async () => {
      try {
        let data: AppointmentResponse[] = [];

        if (currentUser.role === "DOCTOR") {
          data = await appointmentApi.getDoctorAppointments(currentUser.id);
        } else if (currentUser.role === "PATIENT") {
          // 환자인 경우 내 예약 목록 조회 (필요 시 주석 해제)
          // data = await appointmentApi.getPatientAppointments(currentUser.id);
        }

        setAppointments(data);
      } catch (err) {
        console.error("예약 목록 로드 실패:", err);
      }
    };

    fetchAppointments();
  }, [currentUser.id, currentUser.role]);

  // 채팅 시작 핸들러
  const handleChatClick = useCallback(
    async (appointment: AppointmentResponse) => {
      // 채팅 가능 여부 확인
      try {
        const available = await appointmentApi.isChatAvailable(appointment.id);

        if (!available) {
          const lockMessage = await appointmentApi.getChatLockMessage(
            appointment.id
          );
          alert(lockMessage);
          return;
        }

        setSelectedAppointment(appointment);
        setIsChatOpen(true);
      } catch (error) {
        console.error("채팅 가능 여부 확인 실패:", error);
        alert("채팅 시작 중 오류가 발생했습니다.");
      }
    },
    []
  );

  // 알림 클릭으로 채팅창 자동 오픈 이벤트 수신
  useEffect(() => {
    const handleOpenChat = (event: Event) => {
      const customEvent = event as CustomEvent<{ appointmentId: number }>;
      const { appointmentId } = customEvent.detail;
      const appointment = appointments.find((apt) => apt.id === appointmentId);

      if (appointment) {
        handleChatClick(appointment);
      }
    };

    window.addEventListener("openAppointmentChat", handleOpenChat);

    return () => {
      window.removeEventListener("openAppointmentChat", handleOpenChat);
    };
  }, [appointments, handleChatClick]);

  // 예약 취소 핸들러
  const handleCancelAppointment = async (appointmentId: number) => {
    if (!confirm("정말로 예약을 취소하시겠습니까?")) return;

    try {
      await appointmentApi.cancelAppointment(appointmentId);

      // 상태 업데이트: 서버에서 최신 목록 다시 가져오기
      let data: AppointmentResponse[] = [];
      if (currentUser.role === "DOCTOR") {
        data = await appointmentApi.getDoctorAppointments(currentUser.id);
      } else if (currentUser.role === "PATIENT") {
        data = await appointmentApi.getPatientAppointments(currentUser.id);
      }
      setAppointments(data);

      // alert보다는 toast 사용을 권장하지만 여기선 기존 로직 유지
      alert("예약이 성공적으로 취소되었습니다.");
    } catch (error) {
      console.error("예약 취소 실패:", error);
      alert("예약 취소 중 오류가 발생했습니다.");
    }
  };

  // 상태에 따른 뱃지 스타일/텍스트 반환
  const getStatusBadge = (status: string) => {
    switch (status) {
      case "SCHEDULED":
        return { className: "bg-blue-500", text: "예정됨" };
      case "COMPLETED":
        return { className: "bg-green-500", text: "진료 완료" };
      case "CANCELLED":
        return { className: "bg-gray-400", text: "취소됨" };
      case "IN_PROGRESS":
        return { className: "bg-yellow-500", text: "진료 중" };
      default:
        return { className: "bg-gray-500", text: status };
    }
  };

  // 상태 필터에 따라 예약 목록 필터링
  const filteredAppointments = appointments.filter((apt) => {
    if (statusFilter === "ALL") return true;
    if (statusFilter === "SCHEDULED") return apt.status === "SCHEDULED";
    if (statusFilter === "IN_PROGRESS_OR_COMPLETED")
      return apt.status === "IN_PROGRESS" || apt.status === "COMPLETED";
    if (statusFilter === "CANCELLED") return apt.status === "CANCELLED";
    return true;
  });

  return (
    <div className="h-full overflow-auto p-8">
      <div className="max-w-7xl mx-auto space-y-6">
        <div className="flex items-start justify-between">
          <div>
            <h1 className="text-5xl font-black text-gray-900 mb-2">
              예약 관리
            </h1>
            <p className="text-gray-700 font-medium text-lg">
              {currentUser.role === "DOCTOR"
                ? "담당 환자 예약 현황"
                : "내 진료 예약 현황"}
            </p>
          </div>
          <div className="min-w-[200px]">
            <Select value={statusFilter} onValueChange={setStatusFilter}>
              <SelectTrigger className="rounded-xl h-11 bg-white border-gray-200 shadow-sm">
                <SelectValue placeholder="전체 예약" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">전체 예약</SelectItem>
                <SelectItem value="SCHEDULED">예약된 진료</SelectItem>
                <SelectItem value="IN_PROGRESS_OR_COMPLETED">
                  진행 중/완료된 진료
                </SelectItem>
                <SelectItem value="CANCELLED">취소된 진료</SelectItem>
              </SelectContent>
            </Select>
          </div>
        </div>

        {filteredAppointments.length === 0 ? (
          <div className="text-center py-20 text-gray-500 bg-white/50 rounded-2xl border border-dashed border-gray-300">
            {statusFilter === "ALL"
              ? "예정된 예약이 없습니다."
              : "해당 조건의 예약이 없습니다."}
          </div>
        ) : (
          <div className="grid lg:grid-cols-2 gap-6">
            {filteredAppointments.map((apt) => {
              const badge = getStatusBadge(apt.status);

              return (
                <Card
                  key={apt.id}
                  className="backdrop-blur-2xl bg-white/80 border border-gray-200 shadow-lg hover:shadow-xl transition-all duration-300"
                >
                  <div className="p-6">
                    <div className="flex items-start justify-between mb-4">
                      <div className="flex items-center gap-4">
                        <Avatar className="w-14 h-14 border-2 border-white shadow-md">
                          <AvatarFallback className="bg-slate-100">
                            <User className="w-6 h-6 text-gray-400" />
                          </AvatarFallback>
                        </Avatar>
                        <div>
                          <h3 className="text-xl font-bold text-gray-900 flex items-center gap-2">
                            {currentUser.role === "DOCTOR"
                              ? apt.patientName
                              : apt.doctorName}
                            <span className="text-xs font-normal text-gray-500 px-2 py-0.5 bg-gray-100 rounded-full">
                              {currentUser.role === "DOCTOR"
                                ? "환자"
                                : "담당의"}
                            </span>
                          </h3>
                          <div className="text-sm text-gray-500 mt-1">
                            예약 번호: #{apt.id}
                          </div>
                        </div>
                      </div>
                      <Badge
                        className={`${badge.className} text-white font-bold border-0 px-3 py-1`}
                      >
                        {badge.text}
                      </Badge>
                    </div>

                    <div className="bg-gray-50 p-4 rounded-xl space-y-3 mb-5 border border-gray-100">
                      <div className="flex items-center gap-3 text-gray-700">
                        <div className="p-2 bg-white rounded-lg shadow-sm">
                          <CalendarIcon className="w-4 h-4 text-blue-500" />
                        </div>
                        <span className="font-medium">
                          {new Date(apt.date).toLocaleString("ko-KR", {
                            year: "numeric",
                            month: "long",
                            day: "numeric",
                            weekday: "short",
                            hour: "2-digit",
                            minute: "2-digit",
                          })}
                        </span>
                      </div>
                      <div className="flex items-center gap-3 text-gray-700">
                        <div className="p-2 bg-white rounded-lg shadow-sm">
                          <FileText className="w-4 h-4 text-purple-500" />
                        </div>
                        <span className="font-medium">
                          {apt.departmentName || "진료 예약"}
                        </span>
                      </div>
                    </div>

                    {apt.status === "SCHEDULED" && (
                      <div className="flex gap-3">
                        <Button
                          className="flex-1 rounded-xl bg-blue-50 text-blue-600 hover:bg-blue-100 border-blue-200 border shadow-sm font-bold h-11"
                          onClick={() => handleChatClick(apt)}
                        >
                          <MessageCircle className="w-4 h-4 mr-2" />
                          1:1 채팅
                        </Button>
                        <Button
                          variant="outline"
                          className="flex-1 rounded-xl border-red-200 text-red-600 hover:bg-red-50 hover:text-red-700 hover:border-red-300 font-bold h-11"
                          onClick={() => handleCancelAppointment(apt.id)}
                        >
                          <XCircle className="w-4 h-4 mr-2" />
                          예약 취소
                        </Button>
                      </div>
                    )}
                  </div>
                </Card>
              );
            })}
          </div>
        )}
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
              currentUser.role === "DOCTOR"
                ? selectedAppointment.patientId
                : selectedAppointment.doctorId,
            name:
              currentUser.role === "DOCTOR"
                ? selectedAppointment.patientName ||
                  `환자 #${selectedAppointment.patientId}`
                : selectedAppointment.doctorName ||
                  `의사 #${selectedAppointment.doctorId}`,
          }}
        />
      )}
    </div>
  );
}
