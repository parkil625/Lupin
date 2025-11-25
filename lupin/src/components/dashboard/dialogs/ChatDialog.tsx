import {
  Dialog,
  DialogContent,
  DialogHeader,
  // DialogTitle, // 커스텀 헤더를 쓸 거라 기존 Title, Description은 지우거나 안 씁니다
  // DialogDescription,
} from "@/components/ui/dialog";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Send } from "lucide-react";
import { ChatMessage } from "@/types/dashboard.types";

interface ChatDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  messages: ChatMessage[];
  chatMessage: string;
  setChatMessage: (message: string) => void;
  onSend: () => void;
}

export default function ChatDialog({
  open,
  onOpenChange,
  messages,
  chatMessage,
  setChatMessage,
  onSend,
}: ChatDialogProps) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="bg-red-500 w-full !max-w-4xl h-[600px] flex flex-col p-0">
        {/* 헤더 부분 수정 시작 */}
        <DialogHeader className="px-6 py-4 border-b">
          <div className="flex items-center gap-4">
            {/* 1. 의사 프로필 아바타 */}
            <Avatar className="w-10 h-10 bg-blue-100">
              <AvatarFallback className="bg-blue-500 text-white font-bold">
                김
              </AvatarFallback>
            </Avatar>

            <div className="flex flex-col">
              {/* 2. 이름과 상태 표시줄 */}
              {/* 여기 gap-3을 gap-6 등으로 늘리면 사이 간격이 더 넓어집니다 */}
              <div className="flex items-center gap-6">
                <span className="text-lg font-bold text-gray-900">김의사</span>

                {/* 진료 중 상태 배지 */}
                <div className="flex items-center gap-1.5">
                  <div className="w-2 h-2 rounded-full bg-green-500 animate-pulse" />
                  <span className="text-sm font-medium text-gray-600">
                    진료 중
                  </span>
                </div>
              </div>

              {/* 3. 온라인 상태 텍스트 */}
              <span className="text-xs text-gray-400 mt-0.5">온라인</span>
            </div>
          </div>
        </DialogHeader>
        {/* 헤더 부분 수정 끝 */}

        <ScrollArea className="flex-1 px-6">
          <div className="space-y-4 py-4">
            {messages.map((msg) => (
              <div
                key={msg.id}
                className={`flex gap-3 ${msg.isMine ? "justify-end" : ""}`}
              >
                {!msg.isMine && (
                  <Avatar className="w-8 h-8">
                    <AvatarFallback className="bg-blue-500 text-white font-black text-xs">
                      {msg.avatar}
                    </AvatarFallback>
                  </Avatar>
                )}
                <div
                  className={`rounded-2xl p-3 max-w-md ${
                    msg.isMine ? "bg-[#C93831] text-white" : "bg-gray-100"
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

        <div className="p-6 border-t">
          <div className="flex gap-2">
            <Input
              placeholder="메시지 입력..."
              className="rounded-xl"
              value={chatMessage}
              onChange={(e) => setChatMessage(e.target.value)}
              onKeyPress={(e) => {
                if (e.key === "Enter") {
                  onSend();
                }
              }}
            />
            <Button
              onClick={onSend}
              className="bg-gradient-to-r from-[#C93831] to-[#B02F28] text-white rounded-xl"
            >
              <Send className="w-4 h-4" />
            </Button>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
}
