package com.example.demo.repository;

import com.example.demo.domain.entity.Notification;
import com.example.demo.domain.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @Test
    @DisplayName("사용자의 알림을 최신순으로 조회한다")
    void findByUserOrderByCreatedAtDescTest() {
        // given
        User user = createAndSaveUser("user1");
        User otherUser = createAndSaveUser("user2");

        createAndSaveNotification(user, "LIKE", "좋아요 알림 1");
        createAndSaveNotification(user, "COMMENT", "댓글 알림");
        createAndSaveNotification(user, "LIKE", "좋아요 알림 2");
        createAndSaveNotification(otherUser, "LIKE", "다른 사용자 알림");

        // when
        List<Notification> notifications = notificationRepository.findByUserOrderByCreatedAtDesc(user);

        // then
        assertThat(notifications).hasSize(3);
        assertThat(notifications.get(0).getTitle()).isEqualTo("좋아요 알림 2");
        assertThat(notifications.get(1).getTitle()).isEqualTo("댓글 알림");
        assertThat(notifications.get(2).getTitle()).isEqualTo("좋아요 알림 1");
    }

    @Test
    @DisplayName("읽지 않은 알림이 있는지 확인한다")
    void existsByUserAndIsReadFalseTest() {
        // given
        User user = createAndSaveUser("user1");
        User allReadUser = createAndSaveUser("user2");

        createAndSaveNotification(user, "LIKE", "읽지 않은 알림");
        createAndSaveNotification(allReadUser, "LIKE", "읽은 알림", true);

        // when
        boolean hasUnread = notificationRepository.existsByUserAndIsReadFalse(user);
        boolean noUnread = notificationRepository.existsByUserAndIsReadFalse(allReadUser);

        // then
        assertThat(hasUnread).isTrue();
        assertThat(noUnread).isFalse();
    }

    private Notification createAndSaveNotification(User user, String type, String title) {
        return createAndSaveNotification(user, type, title, false);
    }

    private Notification createAndSaveNotification(User user, String type, String title, boolean isRead) {
        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .isRead(isRead)
                .build();
        return notificationRepository.save(notification);
    }
}
