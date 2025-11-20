package com.example.demo.controller;

import com.example.demo.dto.request.ChatMessageCreateRequest;
import com.example.demo.dto.response.ChatMessageResponse;
import com.example.demo.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

/**
 * WebSocket을 통한 실시간 채팅 컨트롤러
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketController {

    private final ChatMessageService chatMessageService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 클라이언트가 /app/chat.sendMessage로 메시지를 보내면 호출됨
     *
     * @param request 채팅 메시지 요청
     */
    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload ChatMessageCreateRequest request) {
        try {
            log.info("WebSocket 메시지 수신: roomId={}, senderId={}",
                    request.getPatientId() + ":" + request.getDoctorId(),
                    request.getSenderId());

            // 메시지 저장
            ChatMessageResponse savedMessage = chatMessageService.sendMessage(request);

            // roomId 생성
            String roomId = request.getPatientId() + ":" + request.getDoctorId();

            // 해당 채팅방을 구독하는 모든 클라이언트에게 메시지 전송
            // 구독 경로: /topic/chat/{roomId}
            messagingTemplate.convertAndSend(
                    "/topic/chat/" + roomId,
                    savedMessage
            );

            log.info("메시지 전송 완료: messageId={}, roomId={}", savedMessage.getId(), roomId);

        } catch (Exception e) {
            log.error("메시지 전송 실패", e);
            throw new RuntimeException("메시지 전송에 실패했습니다.", e);
        }
    }

    /**
     * 클라이언트가 /app/chat.markAsRead로 읽음 표시 요청을 보내면 호출됨
     *
     * @param request roomId와 userId를 포함하는 요청
     */
    @MessageMapping("/chat.markAsRead")
    public void markAsRead(@Payload MarkAsReadRequest request) {
        try {
            log.info("읽음 처리 요청: roomId={}, userId={}", request.getRoomId(), request.getUserId());

            // 메시지 읽음 처리
            chatMessageService.markAllAsRead(request.getRoomId(), request.getUserId());

            // 읽음 처리 완료 알림 전송 (상대방에게도 알림)
            messagingTemplate.convertAndSend(
                    "/topic/chat/" + request.getRoomId() + "/read",
                    new ReadNotification(request.getUserId(), request.getRoomId())
            );

            log.info("읽음 처리 완료: roomId={}", request.getRoomId());

        } catch (Exception e) {
            log.error("읽음 처리 실패", e);
        }
    }

    /**
     * 읽음 표시 요청 DTO
     */
    public static class MarkAsReadRequest {
        private String roomId;
        private Long userId;

        public String getRoomId() {
            return roomId;
        }

        public void setRoomId(String roomId) {
            this.roomId = roomId;
        }

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }
    }

    /**
     * 읽음 알림 DTO
     */
    public static class ReadNotification {
        private Long userId;
        private String roomId;

        public ReadNotification(Long userId, String roomId) {
            this.userId = userId;
            this.roomId = roomId;
        }

        public Long getUserId() {
            return userId;
        }

        public String getRoomId() {
            return roomId;
        }
    }
}
