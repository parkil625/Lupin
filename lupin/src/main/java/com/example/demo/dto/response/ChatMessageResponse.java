package com.example.demo.dto.response;

import com.example.demo.domain.entity.ChatMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 채팅 메시지 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageResponse {

    private Long id;
    private String roomId;
    private String content;
    private Long senderId;
    private String senderName;
    private String senderProfileImage;
    private LocalDateTime sentAt;
    private Boolean isRead;

    /**
     * Entity -> Response DTO 변환
     */
    public static ChatMessageResponse from(ChatMessage chatMessage) {
        return ChatMessageResponse.builder()
                .id(chatMessage.getId())
                .roomId(chatMessage.getRoomId())
                .content(chatMessage.getContent())
                .senderId(chatMessage.getSender().getId())
                .senderName(chatMessage.getSender().getName())
                .senderProfileImage(chatMessage.getSender().getProfileImage())
                .sentAt(chatMessage.getSentAt())
                .isRead("Y".equals(chatMessage.getIsRead()))
                .build();
    }
}
