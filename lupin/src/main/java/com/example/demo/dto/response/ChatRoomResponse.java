package com.example.demo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 채팅방 정보 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoomResponse {

    private String roomId;
    private Long patientId;
    private String patientName;
    private String patientProfileImage;
    private Long doctorId;
    private String doctorName;
    private String doctorProfileImage;
    private String lastMessage;
    private LocalDateTime lastMessageTime;
    private Long unreadCount;
}
