package com.example.demo.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ChatMessage 엔티티 테스트")
class ChatMessageTest {

    @Test
    @DisplayName("읽음 처리 시 isRead가 true로 변경된다")
    void markAsReadTest() {
        // given
        ChatMessage message = ChatMessage.builder()
                .roomId("1:21")
                .sender(User.builder().id(1L).build())
                .content("테스트 메시지")
                .isRead(false)
                .build();

        // when
        message.markAsRead();

        // then
        assertThat(message.getIsRead()).isTrue();
    }

    @Test
    @DisplayName("roomId 생성 시 patientId:doctorId 형식이다")
    void generateRoomIdFormatTest() {
        // given
        Long patientId = 1L;
        Long doctorId = 21L;

        // when
        String roomId = ChatMessage.generateRoomId(patientId, doctorId);

        // then
        assertThat(roomId).isEqualTo("1:21");
    }

    @Test
    @DisplayName("roomId 생성 시 ID 순서가 유지된다")
    void generateRoomIdOrderTest() {
        // given & when
        String roomId1 = ChatMessage.generateRoomId(5L, 10L);
        String roomId2 = ChatMessage.generateRoomId(100L, 200L);

        // then
        assertThat(roomId1).isEqualTo("5:10");
        assertThat(roomId2).isEqualTo("100:200");
    }

    @Test
    @DisplayName("기본값으로 isRead는 false다")
    void defaultIsReadIsFalseTest() {
        // given & when
        ChatMessage message = ChatMessage.builder()
                .roomId("1:21")
                .sender(User.builder().id(1L).build())
                .content("테스트 메시지")
                .build();

        // then
        assertThat(message.getIsRead()).isFalse();
    }

    @Test
    @DisplayName("기본값으로 time은 현재 시간이다")
    void defaultTimeIsNowTest() {
        // given
        LocalDateTime before = LocalDateTime.now();

        // when
        ChatMessage message = ChatMessage.builder()
                .roomId("1:21")
                .sender(User.builder().id(1L).build())
                .content("테스트 메시지")
                .build();

        LocalDateTime after = LocalDateTime.now();

        // then
        assertThat(message.getTime()).isNotNull();
        assertThat(message.getTime()).isAfterOrEqualTo(before);
        assertThat(message.getTime()).isBeforeOrEqualTo(after);
    }
}
