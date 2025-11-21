import { useEffect, useRef, useState, useCallback } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { ChatMessageResponse } from '@/api/chatApi';

interface UseWebSocketProps {
    roomId: string;
    userId: number;
    onMessageReceived: (message: ChatMessageResponse) => void;
    onReadNotification?: (notification: { userId: number; roomId: string }) => void;
}

export const useWebSocket = ({
                                 roomId,
                                 userId,
                                 onMessageReceived,
                                 onReadNotification,
                             }: UseWebSocketProps) => {
    const [isConnected, setIsConnected] = useState(false);
    const clientRef = useRef<Client | null>(null);

    useEffect(() => {
        // ----------------------------------------------------
        // [업그레이드] 도메인 자동 감지 로직
        // ----------------------------------------------------
        const isLocal = window.location.hostname === 'localhost';

        // 로컬이면 백엔드 포트(8081)로,
        // 배포 환경이면 '현재 접속한 도메인(lupin-care 등)' 뒤에 /ws를 붙여서 연결
        const socketUrl = isLocal
            ? 'http://localhost:8081/ws'
            : `${window.location.origin}/ws`;

        console.log(`[WebSocket] 연결 URL: ${socketUrl}`);

        const client = new Client({
            webSocketFactory: () => new SockJS(socketUrl),
            debug: (str) => {
                if (isLocal) console.log('[STOMP Debug]', str);
            },
            reconnectDelay: 5000,
            heartbeatIncoming: 4000,
            heartbeatOutgoing: 4000,
            onConnect: () => {
                console.log('✅ WebSocket 연결 성공');
                setIsConnected(true);

                client.subscribe(`/topic/chat/${roomId}`, (message) => {
                    const receivedMessage: ChatMessageResponse = JSON.parse(message.body);
                    console.log('📩 메시지 수신:', receivedMessage);
                    onMessageReceived(receivedMessage);
                });

                if (onReadNotification) {
                    client.subscribe(`/topic/chat/${roomId}/read`, (message) => {
                        const notification = JSON.parse(message.body);
                        console.log('👀 읽음 알림 수신:', notification);
                        onReadNotification(notification);
                    });
                }
            },
            onStompError: (frame) => {
                console.error('❌ STOMP 에러:', frame.headers['message']);
                console.error('상세:', frame.body);
            },
            onDisconnect: () => {
                console.log('❌ WebSocket 연결 해제');
                setIsConnected(false);
            },
        });

        clientRef.current = client;
        client.activate();

        return () => {
            if (client.active) {
                client.deactivate();
            }
        };
    }, [roomId, userId, onMessageReceived, onReadNotification]);

    const sendMessage = useCallback((content: string, senderId: number, patientId: number, doctorId: number) => {
        if (!clientRef.current?.connected) {
            console.error('WebSocket이 연결되지 않았습니다.');
            return;
        }

        const messageRequest = {
            senderId,
            patientId,
            doctorId,
            content,
        };

        clientRef.current.publish({
            destination: '/app/chat.sendMessage',
            body: JSON.stringify(messageRequest),
        });
    }, []);

    const markAsRead = useCallback(() => {
        if (!clientRef.current?.connected) {
            console.error('WebSocket이 연결되지 않았습니다.');
            return;
        }

        clientRef.current.publish({
            destination: '/app/chat.markAsRead',
            body: JSON.stringify({ roomId, userId }),
        });
    }, [roomId, userId]);

    return {
        isConnected,
        sendMessage,
        markAsRead,
    };
};