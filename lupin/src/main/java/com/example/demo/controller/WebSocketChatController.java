package com.example.demo.controller;

import com.example.demo.domain.entity.ChatMessage;
import com.example.demo.dto.request.ChatMessageRequest;
import com.example.demo.dto.response.ChatMessageResponse;
import com.example.demo.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

/**
 * WebSocket 채팅 컨트롤러
 * STOMP 프로토콜을 사용한 실시간 채팅
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class WebSocketChatController {

    private final ChatService chatService;
    private final SimpMessageSendingOperations messagingTemplate;

    /**
     * 메시지 전송
     * 클라이언트: /app/chat.send
     * 구독: /queue/chat/{roomId}
     */
    @Transactional
    @MessageMapping("/chat.send")
    public void sendMessage(@Valid @Payload ChatMessageRequest request) {
        log.info("메시지 전송 요청 - roomId: {}, senderId: {}", request.getRoomId(), request.getSenderId());

        try {
            ChatMessage savedMessage = chatService.saveMessage(
                    request.getRoomId(),
                    request.getSenderId(),
                    request.getContent()
            );

            // Lazy loading 방지를 위해 sender name 명시적 로드
            String senderName = savedMessage.getSender().getName();

            ChatMessageResponse response = ChatMessageResponse.from(savedMessage);

            messagingTemplate.convertAndSend(
                    "/queue/chat/" + request.getRoomId(),
                    response
            );

            log.info("메시지 전송 완료 - messageId: {}", savedMessage.getId());

        } catch (IllegalArgumentException e) {
            log.error("메시지 전송 실패: {}", e.getMessage());

            messagingTemplate.convertAndSendToUser(
                    request.getSenderId().toString(),
                    "/queue/errors",
                    "메시지 전송 실패: " + e.getMessage()
            );
        }
    }
}
