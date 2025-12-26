import { useEffect, useRef, useState, useCallback } from "react";
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import { ChatMessageResponse } from "@/api/chatApi";

interface UseWebSocketProps {
  roomId: string;
  userId: number;
  onMessageReceived: (message: ChatMessageResponse) => void;
}

export const useWebSocket = ({
  roomId,
  userId,
  onMessageReceived,
}: UseWebSocketProps) => {
  const [isConnected, setIsConnected] = useState(false);
  const clientRef = useRef<Client | null>(null);
  const onMessageReceivedRef = useRef(onMessageReceived);

  // 콜백이 변경되면 ref 업데이트
  useEffect(() => {
    onMessageReceivedRef.current = onMessageReceived;
  }, [onMessageReceived]);

  useEffect(() => {
    // roomId가 없으면 WebSocket 연결하지 않음
    if (!roomId) {
      // 기존 연결이 있다면 정리
      if (clientRef.current?.active) {
        clientRef.current.deactivate();
        clientRef.current = null;
      }
      return;
    }

    // ----------------------------------------------------
    // [업그레이드] 도메인 자동 감지 로직
    // ----------------------------------------------------
    const isLocal = window.location.hostname === "localhost";

    // 로컬이면 백엔드 포트(8081)로,
    // 배포 환경이면 '현재 접속한 도메인(lupin-care 등)' 뒤에 /ws를 붙여서 연결
    const socketUrl = isLocal
      ? "http://localhost:8081/ws"
      : "https://api.lupin-care.com/ws";

    const client = new Client({
      webSocketFactory: () =>
        new SockJS(socketUrl, null, {
          transports: ["websocket"],
          debug: false,
        }),
      debug: () => {
        // 프로덕션 환경에서는 debug 로그 비활성화
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      onConnect: () => {
        setIsConnected(true);

        // 🔧 수정: 백엔드와 일치하도록 /queue로 변경
        client.subscribe(`/queue/chat/${roomId}`, (message) => {
          const receivedMessage: ChatMessageResponse = JSON.parse(message.body);
          onMessageReceivedRef.current(receivedMessage);
        });
      },
      onStompError: (frame) => {
        console.error("STOMP 에러:", frame.headers["message"]);
        setIsConnected(false);
      },
      onDisconnect: () => {
        setIsConnected(false);
      },
      onWebSocketClose: () => {
        setIsConnected(false);
      },
    });

    clientRef.current = client;

    // 약간의 딜레이를 주어 이전 연결이 완전히 정리되도록 함
    const timeoutId = setTimeout(() => {
      client.activate();
    }, 100);

    return () => {
      clearTimeout(timeoutId);
      if (client.active) {
        client.deactivate();
      }
      setIsConnected(false);
    };
  }, [roomId, userId]);

  const sendMessage = useCallback(
    (content: string, senderId: number) => {
      if (!clientRef.current?.connected) {
        return;
      }

      // 🔧 수정: props로 받은 roomId를 그대로 사용하여 일관성 유지
      const messageRequest = {
        roomId: roomId, // Props로 받은 roomId 사용
        senderId,
        content,
      };

      clientRef.current.publish({
        destination: "/app/chat.send",
        body: JSON.stringify(messageRequest),
      });
    },
    [roomId]
  );

  // 🔧 제거: markAsRead는 REST API로만 처리 (WebSocket 미사용)
  // REST API: PUT /api/chat/rooms/{roomId}/read?userId={userId}

  return {
    isConnected,
    sendMessage,
  };
};
