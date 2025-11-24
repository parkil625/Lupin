package com.example.demo.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Notification 엔티티 테스트")
class NotificationTest {

    @Test
    @DisplayName("알림 읽음 처리")
    void markAsRead_Success() {
        // given
        Notification notification = Notification.builder()
                .id(1L)
                .userId(1L)
                .type("like")
                .title("새로운 좋아요")
                .content("회원님의 피드에 좋아요가 등록되었습니다.")
                .build();

        // when
        notification.markAsRead();

        // then
        assertThat(notification.getIsRead()).isTrue();
    }

    @Test
    @DisplayName("알림 생성 - 기본값 확인")
    void create_DefaultValues() {
        // given & when
        Notification notification = Notification.builder()
                .id(1L)
                .userId(1L)
                .type("comment")
                .title("새로운 댓글")
                .build();

        // then
        assertThat(notification.getIsRead()).isFalse();
        assertThat(notification.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("참조 ID 설정")
    void refId_Success() {
        // given & when
        Notification notification = Notification.builder()
                .id(1L)
                .userId(1L)
                .type("like")
                .title("새로운 좋아요")
                .refId("123")
                .build();

        // then
        assertThat(notification.getRefId()).isEqualTo("123");
    }

    @Test
    @DisplayName("알림 타입 확인")
    void types_Success() {
        // given & when
        Notification likeNotification = Notification.builder()
                .id(1L)
                .userId(1L)
                .type("like")
                .title("좋아요")
                .build();

        Notification commentNotification = Notification.builder()
                .id(2L)
                .userId(1L)
                .type("comment")
                .title("댓글")
                .build();

        Notification systemNotification = Notification.builder()
                .id(3L)
                .userId(1L)
                .type("system")
                .title("시스템")
                .build();

        // then
        assertThat(likeNotification.getType()).isEqualTo("like");
        assertThat(commentNotification.getType()).isEqualTo("comment");
        assertThat(systemNotification.getType()).isEqualTo("system");
    }
}
