import { useState, useEffect, useRef } from "react";
import { useWebSocket } from "@/hooks/useWebSocket";
import { ChatMessageResponse, chatApi } from "@/api/chatApi";
import { Dialog, DialogContent, DialogHeader } from "@/components/ui/dialog";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Send, FileText, CheckCircle2 } from "lucide-react";
import PrescriptionDialog from "./PrescriptionDialog";
import PrescriptionModal from "../dialogs/PrescriptionModal"; // [수정] 경로 수정
import { prescriptionApi, PrescriptionResponse } from "@/api/prescriptionApi"; // [수정] 타입 Import 수정
import { toast } from "sonner"; // [추가] 알림용
import apiClient from "@/api/client";
import { userApi } from "@/api/userApi";
import UserHoverCard from "@/components/dashboard/shared/UserHoverCard";

interface ChatRoomProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  appointmentId: number;
  currentUser: { id: number; name: string; role: "DOCTOR" | "PATIENT" };
  targetUser: { id: number; name: string };
}

export default function ChatRoom({
  open,
  onOpenChange,
  appointmentId,
  currentUser,
  targetUser,
}: ChatRoomProps) {
  const [messages, setMessages] = useState<ChatMessageResponse[]>([]);
  const [input, setInput] = useState("");

  const [prescriptionDialogOpen, setPrescriptionDialogOpen] = useState(false);
  // [추가] 처방전 조회 모달 상태 관리
  const [viewPrescriptionOpen, setViewPrescriptionOpen] = useState(false);
  const [receivedPrescription, setReceivedPrescription] =
    useState<PrescriptionResponse | null>(null);

  const [isEndingConsultation, setIsEndingConsultation] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  // 프로필 이미지 state
  const [targetUserAvatar, setTargetUserAvatar] = useState<string>("");
  // 상대방 활동일 state
  const [targetUserActiveDays, setTargetUserActiveDays] = useState<number>();

  // roomId는 예약 건별로 생성 (appointment_ID 형식)
  const roomId = `appointment_${appointmentId}`;

  // 프로필 이미지 및 활동일 로드
  useEffect(() => {
    const loadProfiles = async () => {
      try {
        // 상대방 프로필
        const targetUserData = await userApi.getUserById(targetUser.id);
        if (targetUserData.avatar) {
          setTargetUserAvatar(targetUserData.avatar);
        }

        // 상대방 활동일 정보 가져오기
        try {
          const stats = await userApi.getUserStats(targetUser.id);
          if (stats.activeDays !== undefined) {
            setTargetUserActiveDays(stats.activeDays);
          }
        } catch (statsError) {
          console.error("상대방 통계 로드 실패:", statsError);
        }
      } catch (error) {
        console.error("프로필 이미지 로드 실패:", error);
      }
    };

    if (open) {
      loadProfiles();
    }
  }, [open, targetUser.id]);

  // 1. 이전 채팅 기록 불러오기
  useEffect(() => {
    if (!open) return;

    // 채팅창이 열릴 때 항상 최신 메시지 로드
    chatApi
      .getAllMessagesByRoomId(roomId)
      .then((data) => setMessages(data))
      .catch((err) => console.error("채팅 기록 로드 실패:", err));

    // 읽음 처리
    chatApi
      .markAsRead(roomId, currentUser.id)
      .catch((err) => console.error("읽음 처리 실패:", err));
  }, [open, roomId, currentUser.id]);

  // 2. 웹소켓 연결
  const { isConnected, sendMessage } = useWebSocket({
    roomId,
    userId: currentUser.id,
    onMessageReceived: async (
      msg: ChatMessageResponse & { type?: string; doctorName?: string }
    ) => {
      // [수정] 처방전 발급 메시지 감지 및 모달 자동 오픈 (환자용)
      if (
        currentUser.role === "PATIENT" &&
        msg.content &&
        (msg.content.includes("처방전이 발급되었습니다") ||
          msg.content.includes("처방전이 도착했습니다"))
      ) {
        try {
          // DB 트랜잭션 반영 대기 (안전장치)
          await new Promise((resolve) => setTimeout(resolve, 500));

          // 해당 예약의 처방전 정보 조회
          const response = await prescriptionApi.getByAppointmentId(
            appointmentId
          );

          if (response) {
            setReceivedPrescription(response);
            setViewPrescriptionOpen(true);
            toast.success("📋 처방전이 도착했습니다! 확인해보세요.");
          }

          // Medical 페이지 등의 목록 갱신 이벤트
          window.dispatchEvent(
            new CustomEvent("prescription-created", {
              detail: { patientId: currentUser.id },
            })
          );
        } catch (e) {
          console.error("처방전 데이터 자동 로드 실패", e);
        }
      }

      // 진료 종료 알림 처리
      if (msg.type === "CONSULTATION_END") {
        if (currentUser.role === "PATIENT") {
          // setTimeout을 사용해 다음 이벤트 루프에서 alert 표시 (즉시 표시)
          setTimeout(() => {
            // 1. alert를 가장 먼저 표시 (블로킹)
            alert("진료가 종료되었습니다.\n예약 목록으로 이동합니다.");

            // 2. alert를 닫은 후 채팅창 닫기
            onOpenChange(false);

            // 3. Medical 컴포넌트 상태 초기화를 위한 이벤트 발생 (의사 이름 포함)
            window.dispatchEvent(
              new CustomEvent("consultationEnded", {
                detail: { doctorName: msg.doctorName || targetUser.name },
              })
            );
          }, 0);
        }
        return;
      }

      // 일반 채팅 메시지 처리
      setMessages((prev) => [...prev, msg]);
      // 스크롤을 아래로 자동 이동
      setTimeout(() => {
        messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
      }, 100);
    },
  });

  // 3. 메시지 전송 핸들러
  const handleSend = () => {
    if (!input.trim()) return;
    sendMessage(input, currentUser.id);
    setInput("");
  };

  // 4. 진료 종료 핸들러 (의사만 사용)
  const handleEndConsultation = async () => {
    if (!confirm("진료를 종료하시겠습니까?")) return;

    setIsEndingConsultation(true);
    try {
      await apiClient.put(`/api/appointment/${appointmentId}/complete`);

      // 성공 시 채팅창 닫기
      onOpenChange(false);

      // 의사 측 페이지 업데이트를 위한 이벤트 발생
      window.dispatchEvent(
        new CustomEvent("doctorConsultationEnded", {
          detail: { appointmentId },
        })
      );
    } catch (error) {
      console.error("진료 종료 실패:", error);
      alert("진료 종료에 실패했습니다.");
    } finally {
      setIsEndingConsultation(false);
    }
  };

  // 스크롤 자동 이동
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  return (
    <>
      <Dialog open={open} onOpenChange={onOpenChange}>
        <DialogContent className="w-full !max-w-4xl h-[600px] flex flex-col p-0">
          {/* 헤더 */}
          <DialogHeader className="px-6 py-4 border-b">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-4">
                <UserHoverCard
                  name={targetUser.name}
                  department={currentUser.role === "DOCTOR" ? "환자" : "의사"}
                  size="md"
                  avatarUrl={targetUserAvatar}
                  activeDays={targetUserActiveDays}
                />

                <div className="flex flex-col">
                  <div className="flex items-center gap-6">
                    <span className="text-lg font-bold text-gray-900">
                      {targetUser.name}
                    </span>

                    {/* 연결 상태 배지 */}
                    <div className="flex items-center gap-1.5">
                      <div
                        className={`w-2 h-2 rounded-full ${
                          isConnected
                            ? "bg-green-500 animate-pulse"
                            : "bg-gray-400"
                        }`}
                      />
                      <span className="text-sm font-medium text-gray-600">
                        {isConnected ? "연결됨" : "연결 중..."}
                      </span>
                    </div>
                  </div>

                  <span className="text-xs text-gray-400 mt-0.5">
                    예약 #{appointmentId}
                  </span>
                </div>
              </div>

              {/* 의사인 경우에만 처방전 발급 및 진료 종료 버튼 표시 */}
              {currentUser.role === "DOCTOR" && (
                <div className="flex gap-2">
                  <Button
                    variant="outline"
                    className="rounded-xl border-[#C93831] text-[#C93831] hover:bg-red-50"
                    onClick={() => setPrescriptionDialogOpen(true)}
                  >
                    <FileText className="w-4 h-4 mr-2" />
                    처방전 발급
                  </Button>
                  <Button
                    variant="outline"
                    className="rounded-xl border-green-600 text-green-600 hover:bg-green-50"
                    onClick={handleEndConsultation}
                    disabled={isEndingConsultation}
                  >
                    <CheckCircle2 className="w-4 h-4 mr-2" />
                    {isEndingConsultation ? "종료 중..." : "진료 종료"}
                  </Button>
                </div>
              )}
            </div>
          </DialogHeader>

          {/* 메시지 목록 */}
          <ScrollArea className="flex-1 px-6 custom-scrollbar">
            <div className="space-y-4 py-4">
              {messages.map((msg, idx) => {
                const isMine = msg.senderId === currentUser.id;
                return (
                  <div
                    key={idx}
                    className={`flex gap-3 ${isMine ? "justify-end" : ""}`}
                  >
                    {!isMine && (
                      <UserHoverCard
                        name={msg.senderName || targetUser.name}
                        department={currentUser.role === "DOCTOR" ? "환자" : "의사"}
                        size="sm"
                        avatarUrl={targetUserAvatar}
                        activeDays={targetUserActiveDays}
                      />
                    )}
                    <div
                      className={`rounded-2xl p-3 max-w-md ${
                        isMine ? "bg-[#C93831] text-white" : "bg-gray-100 border border-white"
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
                        {new Date(msg.sentAt).toLocaleTimeString("ko-KR", {
                          hour: "2-digit",
                          minute: "2-digit",
                        })}
                      </div>
                    </div>
                  </div>
                );
              })}
              <div ref={messagesEndRef} />
            </div>
          </ScrollArea>

          {/* 입력 영역 */}
          <div className="p-6 border-t">
            <div className="flex gap-2">
              <Input
                placeholder="메시지 입력..."
                className="rounded-xl bg-white/40 backdrop-blur-xl border-2 border-gray-300 focus-visible:ring-0 focus-visible:border-[#C93831] transition-all duration-300"
                value={input}
                onChange={(e) => setInput(e.target.value)}
                onKeyPress={(e) => {
                  if (e.key === "Enter") {
                    handleSend();
                  }
                }}
                onFocus={(e) => {
                  // [기존 코드 유지] 포커스 시 그림자 효과
                  e.target.style.boxShadow = '0 0 20px 5px rgba(201, 56, 49, 0.35)';
                }}
                onBlur={(e) => {
                  // [기존 코드 유지] 포커스 해제 시 그림자 제거
                  e.target.style.boxShadow = '';
                }}
                disabled={!isConnected}
              />
              <Button
                onClick={handleSend}
                className="bg-gradient-to-r from-[#C93831] to-[#B02F28] text-white rounded-xl hover:from-[#B02F28] hover:to-[#C93831]"
                disabled={!isConnected || !input.trim()}
              >
                <Send className="w-4 h-4" />
              </Button>
            </div>
          </div>
        </DialogContent>
      </Dialog>

      {/* 의사용: 처방전 작성 다이얼로그 */}
      <PrescriptionDialog
        open={prescriptionDialogOpen}
        onOpenChange={setPrescriptionDialogOpen}
        appointmentId={appointmentId}
        patientId={targetUser.id}
        patientName={targetUser.name}
        onSuccess={() => {
          // [수정] 메시지 문구 통일 ("발급했습니다" -> "발급되었습니다")
          sendMessage("처방전이 발급되었습니다. 확인해주세요.", currentUser.id);

          window.dispatchEvent(
            new CustomEvent("prescription-created", {
              detail: { patientId: targetUser.id },
            })
          );
        }}
      />

      {/* [추가] 환자용: 처방전 조회 모달 렌더링 */}
      <PrescriptionModal
        open={viewPrescriptionOpen}
        onOpenChange={setViewPrescriptionOpen}
        prescription={receivedPrescription}
        onDownload={(p: PrescriptionResponse) => console.log("PDF 다운로드", p)}
      />
    </>
  );
}
