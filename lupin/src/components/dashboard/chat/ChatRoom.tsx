import { useState, useEffect, useRef } from 'react';
import { useWebSocket } from '@/hooks/useWebSocket';
import { ChatMessageResponse, chatApi } from '@/api/chatApi';
import { Dialog, DialogContent, DialogHeader } from '@/components/ui/dialog';
import { ScrollArea } from '@/components/ui/scroll-area';
import { Avatar, AvatarFallback } from '@/components/ui/avatar';
import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';
import { Send, FileText, CheckCircle2 } from 'lucide-react';
import PrescriptionDialog from './PrescriptionDialog';
import axiosInstance from '@/api/axiosInstance';

interface ChatRoomProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  appointmentId: number;
  currentUser: { id: number; name: string; role: 'DOCTOR' | 'PATIENT' };
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
  const [input, setInput] = useState('');
  const [prescriptionDialogOpen, setPrescriptionDialogOpen] = useState(false);
  const [isEndingConsultation, setIsEndingConsultation] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  // roomId는 예약 건별로 생성 (appointment_ID 형식)
  const roomId = `appointment_${appointmentId}`;

  // 1. 이전 채팅 기록 불러오기
  useEffect(() => {
    if (!open) return;

    // 채팅창이 열릴 때 항상 최신 메시지 로드
    chatApi
      .getAllMessagesByRoomId(roomId)
      .then((data) => setMessages(data))
      .catch((err) => console.error('채팅 기록 로드 실패:', err));

    // 읽음 처리
    chatApi
      .markAsRead(roomId, currentUser.id)
      .catch((err) => console.error('읽음 처리 실패:', err));
  }, [roomId, currentUser.id]); // open 제거 - appointmentId가 바뀔 때만 로드

  // 2. 웹소켓 연결
  const { isConnected, sendMessage } = useWebSocket({
    roomId,
    userId: currentUser.id,
    onMessageReceived: (msg: any) => {
      // 진료 종료 알림 처리
      if (msg.type === 'CONSULTATION_END') {
        if (currentUser.role === 'PATIENT') {
          // 환자는 채팅창 닫고 초기 화면으로 돌아가기
          onOpenChange(false);
          // Medical 컴포넌트 상태 초기화를 위한 이벤트 발생 (의사 이름 포함)
          window.dispatchEvent(new CustomEvent('consultationEnded', {
            detail: { doctorName: msg.doctorName || targetUser.name }
          }));
        }
        return;
      }

      // 일반 채팅 메시지 처리
      setMessages((prev) => [...prev, msg]);
      // 스크롤을 아래로 자동 이동
      setTimeout(() => {
        messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
      }, 100);
    },
  });

  // 3. 메시지 전송 핸들러
  const handleSend = () => {
    if (!input.trim()) return;
    sendMessage(input, currentUser.id);
    setInput('');
  };

  // 4. 진료 종료 핸들러 (의사만 사용)
  const handleEndConsultation = async () => {
    if (!confirm('진료를 종료하시겠습니까?')) return;

    setIsEndingConsultation(true);
    try {
      await axiosInstance.put(`/api/appointment/${appointmentId}/complete`);
      // 성공 시 채팅창 닫기
      onOpenChange(false);
      // 의사 측 페이지 업데이트를 위한 이벤트 발생
      window.dispatchEvent(new CustomEvent('doctorConsultationEnded', {
        detail: { appointmentId }
      }));
    } catch (error) {
      console.error('진료 종료 실패:', error);
      alert('진료 종료에 실패했습니다.');
    } finally {
      setIsEndingConsultation(false);
    }
  };

  // 스크롤 자동 이동
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  return (
    <>
      <Dialog open={open} onOpenChange={onOpenChange}>
        <DialogContent className="w-full !max-w-4xl h-[600px] flex flex-col p-0">
          {/* 헤더 */}
          <DialogHeader className="px-6 py-4 border-b">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-4">
                <Avatar className="w-10 h-10 bg-blue-100">
                  <AvatarFallback className="bg-blue-500 text-white font-bold">
                    {targetUser.name.charAt(0)}
                  </AvatarFallback>
                </Avatar>

                <div className="flex flex-col">
                  <div className="flex items-center gap-6">
                    <span className="text-lg font-bold text-gray-900">
                      {targetUser.name}
                    </span>

                    {/* 연결 상태 배지 */}
                    <div className="flex items-center gap-1.5">
                      <div
                        className={`w-2 h-2 rounded-full ${
                          isConnected ? 'bg-green-500 animate-pulse' : 'bg-gray-400'
                        }`}
                      />
                      <span className="text-sm font-medium text-gray-600">
                        {isConnected ? '연결됨' : '연결 중...'}
                      </span>
                    </div>
                  </div>

                  <span className="text-xs text-gray-400 mt-0.5">
                    예약 #{appointmentId}
                  </span>
                </div>
              </div>

              {/* 의사인 경우에만 처방전 발급 및 진료 종료 버튼 표시 */}
              {currentUser.role === 'DOCTOR' && (
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
                    {isEndingConsultation ? '종료 중...' : '진료 종료'}
                  </Button>
                </div>
              )}
            </div>
          </DialogHeader>

          {/* 메시지 목록 */}
          <ScrollArea className="flex-1 px-6">
            <div className="space-y-4 py-4">
              {messages.map((msg, idx) => {
                const isMine = msg.senderId === currentUser.id;
                return (
                  <div
                    key={idx}
                    className={`flex gap-3 ${isMine ? 'justify-end' : ''}`}
                  >
                    {!isMine && (
                      <Avatar className="w-8 h-8">
                        <AvatarFallback className="bg-blue-500 text-white font-black text-xs">
                          {msg.senderName.charAt(0)}
                        </AvatarFallback>
                      </Avatar>
                    )}
                    <div
                      className={`rounded-2xl p-3 max-w-md ${
                        isMine ? 'bg-[#C93831] text-white' : 'bg-gray-100'
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
                          isMine ? 'text-white/80' : 'text-gray-500'
                        }`}
                      >
                        {new Date(msg.sentAt).toLocaleTimeString('ko-KR', {
                          hour: '2-digit',
                          minute: '2-digit',
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
                className="rounded-xl"
                value={input}
                onChange={(e) => setInput(e.target.value)}
                onKeyPress={(e) => {
                  if (e.key === 'Enter') {
                    handleSend();
                  }
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

      {/* 처방전 발급 다이얼로그 */}
      <PrescriptionDialog
        open={prescriptionDialogOpen}
        onOpenChange={setPrescriptionDialogOpen}
        appointmentId={appointmentId}
        patientId={targetUser.id}
        patientName={targetUser.name}
        onSuccess={() => {
          // 처방전 발급 성공 시 채팅으로 알림 메시지 전송
          sendMessage("처방전을 발급했습니다. 확인해주세요.", currentUser.id);
        }}
      />
    </>
  );
}
