import { Card } from "../ui/card";
import { Button } from "../ui/button";
import { Input } from "../ui/input";
import { Avatar, AvatarFallback } from "../ui/avatar";
import { ScrollArea } from "../ui/scroll-area";
import { CheckCircle, Send } from "lucide-react";
import { Patient, ChatMessage } from "@/types/dashboard.types";
import { toast } from "sonner@2.0.3";

interface ChatProps {
  patients: Patient[];
  selectedChatPatient: Patient | null;
  setSelectedChatPatient: (patient: Patient | null) => void;
  chatMessages: { [key: number]: ChatMessage[] };
  chatMessage: string;
  setChatMessage: (message: string) => void;
  handleSendDoctorChat: () => void;
}

export default function Chat({
  patients,
  selectedChatPatient,
  setSelectedChatPatient,
  chatMessages,
  chatMessage,
  setChatMessage,
  handleSendDoctorChat,
}: ChatProps) {
  return (
    <div className="h-full overflow-auto p-8">
      <div className="max-w-5xl mx-auto">
        <div>
          <h1 className="text-5xl font-black text-gray-900 mb-6">채팅</h1>
        </div>

        <Card className="backdrop-blur-2xl bg-white/60 border border-gray-200 shadow-2xl h-[calc(100vh-200px)]">
          <div className="h-full flex">
            {/* Chat List */}
            <div className="w-80 border-r border-gray-200 p-4">
              <h3 className="text-xl font-black text-gray-900 mb-4">
                대화 목록
              </h3>
              <ScrollArea className="h-[calc(100%-60px)]">
                <div className="space-y-2">
                  {patients.slice(0, 4).map((patient) => (
                    <div
                      key={patient.id}
                      onClick={() => setSelectedChatPatient(patient)}
                      className={`p-3 rounded-xl border cursor-pointer hover:shadow-lg transition-all ${
                        selectedChatPatient?.id === patient.id
                          ? "bg-blue-50 border-blue-300"
                          : "bg-white/80 border-gray-200"
                      }`}
                    >
                      <div className="flex items-center gap-3">
                        <Avatar className="w-10 h-10">
                          <AvatarFallback className="bg-gradient-to-br from-gray-600 to-gray-800 text-white font-black text-sm">
                            {patient.avatar}
                          </AvatarFallback>
                        </Avatar>
                        <div className="flex-1 min-w-0">
                          <div className="font-bold text-sm text-gray-900">
                            {patient.name}
                          </div>
                          <div className="text-xs text-gray-600 truncate">
                            {chatMessages[patient.id]?.[
                              chatMessages[patient.id].length - 1
                            ]?.content || "메시지 없음"}
                          </div>
                        </div>
                        {chatMessages[patient.id] &&
                          chatMessages[patient.id].length > 0 && (
                            <div className="w-2 h-2 bg-red-500 rounded-full flex-shrink-0"></div>
                          )}
                      </div>
                    </div>
                  ))}
                </div>
              </ScrollArea>
            </div>

            {/* Chat Area */}
            <div className="flex-1 flex flex-col p-6">
              {selectedChatPatient ? (
                <>
                  <div className="flex items-center justify-between pb-4 border-b border-gray-200 mb-4">
                    <div className="flex items-center gap-3">
                      <Avatar className="w-10 h-10">
                        <AvatarFallback className="bg-gradient-to-br from-gray-600 to-gray-800 text-white font-black">
                          {selectedChatPatient.avatar}
                        </AvatarFallback>
                      </Avatar>
                      <div>
                        <div className="font-bold text-gray-900">
                          {selectedChatPatient.name}
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
                      {(chatMessages[selectedChatPatient.id] || []).map(
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
                              className={`rounded-2xl p-3 max-w-xs ${
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
          </div>
        </Card>
      </div>
    </div>
  );
}
