package com.example.demo.service;

import com.example.demo.dto.response.ChatMessageResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatWebSocketService {

    private final SimpMessageSendingOperations messagingTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Redis에서 메시지가 발행(Publish)되면 이 메서드가 실행됩니다.
     * 역할: Redis 메시지 수신 -> WebSocket(STOMP)으로 사용자에게 전송
     */
    public void handleMessage(String messageJson) {
        try {
            // 1. 들어온 JSON 문자열을 DTO 객체로 변환
            ChatMessageResponse message = objectMapper.readValue(messageJson, ChatMessageResponse.class);

            // 2. 해당 채팅방("/queue/chat/{roomId}")을 구독 중인 사용자들에게 메시지 전송
            messagingTemplate.convertAndSend("/queue/chat/" + message.getRoomId(), message);
            
            log.info("Redis -> WebSocket 전송 완료. RoomId: {}, MsgId: {}", message.getRoomId(), message.getId());

        } catch (Exception e) {
            log.error("Redis 메시지 수신 중 오류 발생: {}", e.getMessage());
        }
    }
}