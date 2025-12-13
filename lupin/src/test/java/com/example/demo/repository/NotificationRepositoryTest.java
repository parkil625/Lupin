package com.example.demo.repository;

import com.example.demo.domain.entity.Notification;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.NotificationType;
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

        createAndSaveNotification(user, NotificationType.FEED_LIKE, "좋아요 알림 1");
        createAndSaveNotification(user, NotificationType.COMMENT, "댓글 알림");
        createAndSaveNotification(user, NotificationType.FEED_LIKE, "좋아요 알림 2");
        createAndSaveNotification(otherUser, NotificationType.FEED_LIKE, "다른 사용자 알림");

        // when
        List<Notification> notifications = notificationRepository.findByUserOrderByCreatedAtDescIdDesc(user);

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

        createAndSaveNotification(user, NotificationType.FEED_LIKE, "읽지 않은 알림");
        createAndSaveNotification(allReadUser, NotificationType.FEED_LIKE, "읽은 알림", true);

        // when
        boolean hasUnread = notificationRepository.existsByUserAndIsReadFalse(user);
        boolean noUnread = notificationRepository.existsByUserAndIsReadFalse(allReadUser);

        // then
        assertThat(hasUnread).isTrue();
        assertThat(noUnread).isFalse();
    }

    @Test
    @DisplayName("refId와 타입 목록으로 알림을 삭제한다")
    void deleteByRefIdAndTypeInTest() {
        // given
        User user = createAndSaveUser("user1");
        String feedId = "100";
        String commentId = "200";

        // 피드 관련 알림
        createAndSaveNotification(user, NotificationType.FEED_LIKE, "피드 좋아요", feedId);
        createAndSaveNotification(user, NotificationType.COMMENT, "댓글 알림", feedId);

        // 댓글 관련 알림
        createAndSaveNotification(user, NotificationType.COMMENT_LIKE, "댓글 좋아요", commentId);
        createAndSaveNotification(user, NotificationType.REPLY, "답글 알림", commentId);

        // 삭제되면 안 되는 알림
        createAndSaveNotification(user, NotificationType.FEED_LIKE, "다른 피드 좋아요", "999");

        // when - 피드 관련 알림 삭제
        notificationRepository.deleteByRefIdAndTypeIn(feedId, List.of(NotificationType.FEED_LIKE, NotificationType.COMMENT));

        // then
        List<Notification> remaining = notificationRepository.findByUserOrderByCreatedAtDescIdDesc(user);
        assertThat(remaining).hasSize(3);
        assertThat(remaining).extracting("type")
                .containsExactlyInAnyOrder(NotificationType.COMMENT_LIKE, NotificationType.REPLY, NotificationType.FEED_LIKE);
    }

    private Notification createAndSaveNotification(User user, NotificationType type, String title) {
        return createAndSaveNotification(user, type, title, null, false);
    }

    private Notification createAndSaveNotification(User user, NotificationType type, String title, boolean isRead) {
        return createAndSaveNotification(user, type, title, null, isRead);
    }

    private Notification createAndSaveNotification(User user, NotificationType type, String title, String refId) {
        return createAndSaveNotification(user, type, title, refId, false);
    }

    private Notification createAndSaveNotification(User user, NotificationType type, String title, String refId, boolean isRead) {
        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .refId(refId)
                .isRead(isRead)
                .build();
        return notificationRepository.save(notification);
    }
}
