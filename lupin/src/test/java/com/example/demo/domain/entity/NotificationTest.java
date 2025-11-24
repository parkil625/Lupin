package com.example.demo.domain.entity;

import com.example.demo.domain.enums.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Notification 엔티티 테스트")
class NotificationTest {

    @Test
    @DisplayName("알림 읽음 처리")
    void markAsRead_Success() {
        // given
        Notification notification = Notification.builder()
                .id(1L)
                .type("like")
                .title("새로운 좋아요")
                .content("회원님의 피드에 좋아요가 등록되었습니다.")
                .build();

        // when
        notification.markAsRead();

        // then
        assertThat(notification.getIsRead()).isEqualTo("Y");
    }

    @Test
    @DisplayName("알림에 사용자 설정")
    void setUser_Success() {
        // given
        User user = User.builder()
                .id(1L)
                .userId("user01")
                .email("user01@test.com")
                .password("password")
                .realName("테스트")
                .role(Role.MEMBER)
                .build();

        Notification notification = Notification.builder()
                .id(1L)
                .type("like")
                .title("새로운 좋아요")
                .build();

        // when
        notification.setUser(user);

        // then
        assertThat(notification.getUser()).isEqualTo(user);
        assertThat(user.getNotifications()).contains(notification);
    }

    @Test
    @DisplayName("알림 생성 - 기본값 확인")
    void create_DefaultValues() {
        // given & when
        Notification notification = Notification.builder()
                .id(1L)
                .type("comment")
                .title("새로운 댓글")
                .build();

        // then
        assertThat(notification.getIsRead()).isEqualTo("N");
        assertThat(notification.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("참조 ID 설정")
    void refId_Success() {
        // given & when
        Notification notification = Notification.builder()
                .id(1L)
                .type("like")
                .title("새로운 좋아요")
                .refId("123")
                .build();

        // then
        assertThat(notification.getRefId()).isEqualTo("123");
    }
}
