/**
 * ChatDialog.tsx
 *
 * 채팅 다이얼로그 컴포넌트
 * - 회원-의사 간 실시간 채팅
 */
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
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
      <DialogContent className="max-w-2xl h-[600px] flex flex-col p-0">
        <DialogHeader className="px-6 pt-6 pb-4 border-b">
          <DialogTitle className="text-2xl font-black">진료 채팅</DialogTitle>
          <DialogDescription>
            의료진과 실시간으로 채팅할 수 있습니다.
          </DialogDescription>
        </DialogHeader>

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
