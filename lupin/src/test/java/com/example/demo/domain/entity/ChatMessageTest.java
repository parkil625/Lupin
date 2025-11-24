package com.example.demo.domain.entity;

import com.example.demo.domain.enums.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@DisplayName("ChatMessage 엔티티 테스트")
class ChatMessageTest {

    @Test
    @DisplayName("채팅 메시지 생성 - 기본값 확인")
    void create_DefaultValues() {
        // given & when
        ChatMessage message = ChatMessage.builder()
                .id(1L)
                .roomId("1:2")
                .content("안녕하세요")
                .build();

        // then
        assertThat(message.getId()).isEqualTo(1L);
        assertThat(message.getRoomId()).isEqualTo("1:2");
        assertThat(message.getContent()).isEqualTo("안녕하세요");
        assertThat(message.getIsRead()).isFalse();
        assertThat(message.getSentAt()).isNotNull();
    }

    @Test
    @DisplayName("메시지 읽음 처리")
    void markAsRead_Success() {
        // given
        ChatMessage message = ChatMessage.builder()
                .id(1L)
                .roomId("1:2")
                .content("테스트 메시지")
                .build();

        // when
        message.markAsRead();

        // then
        assertThat(message.getIsRead()).isTrue();
    }

    @Test
    @DisplayName("방 ID 생성")
    void generateRoomId_Success() {
        // given
        Long patientId = 1L;
        Long doctorId = 2L;

        // when
        String roomId = ChatMessage.generateRoomId(patientId, doctorId);

        // then
        assertThat(roomId).isEqualTo("1:2");
    }

    @Test
    @DisplayName("발신자 설정")
    void setSender_Success() {
        // given
        User sender = User.builder()
                .id(1L)
                .userId("patient01")
                .email("patient@test.com")
                .password("password")
                .realName("환자")
                .role(Role.MEMBER)
                .build();

        ChatMessage message = ChatMessage.builder()
                .id(1L)
                .roomId("1:2")
                .content("진료 예약 문의합니다")
                .sender(sender)
                .build();

        // then
        assertThat(message.getSender()).isEqualTo(sender);
    }

    @Test
    @DisplayName("다양한 방 ID 생성")
    void generateRoomId_DifferentIds() {
        // given & when & then
        assertThat(ChatMessage.generateRoomId(100L, 200L)).isEqualTo("100:200");
        assertThat(ChatMessage.generateRoomId(1L, 1000L)).isEqualTo("1:1000");
        assertThat(ChatMessage.generateRoomId(999L, 1L)).isEqualTo("999:1");
    }

    @Test
    @DisplayName("특정 시간에 메시지 생성")
    void createWithSpecificTime_Success() {
        // given
        LocalDateTime sentAt = LocalDateTime.of(2024, 1, 1, 10, 30);

        // when
        ChatMessage message = ChatMessage.builder()
                .id(1L)
                .roomId("1:2")
                .content("새해 인사")
                .sentAt(sentAt)
                .build();

        // then
        assertThat(message.getSentAt()).isEqualTo(sentAt);
    }
}
