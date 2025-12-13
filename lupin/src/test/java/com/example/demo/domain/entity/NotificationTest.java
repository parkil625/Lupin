package com.example.demo.domain.entity;

import com.example.demo.domain.enums.NotificationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationTest {

    @Test
    @DisplayName("알림을 읽음 처리한다")
    void markAsReadTest() {
        // given
        Notification notification = Notification.builder()
                .type(NotificationType.COMMENT)
                .title("새 댓글")
                .content("테스트 내용")
                .build();

        // when
        notification.markAsRead();

        // then
        assertThat(notification.getIsRead()).isTrue();
    }
}
