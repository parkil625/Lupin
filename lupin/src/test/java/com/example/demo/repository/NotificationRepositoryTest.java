package com.example.demo.repository;

import com.example.demo.config.QueryDslConfig;
import com.example.demo.domain.entity.Notification;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.NotificationType;
import com.example.demo.domain.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(QueryDslConfig.class)
class NotificationRepositoryTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .userId("test@test.com")
                .password("password")
                .name("testUser")
                .role(Role.MEMBER)
                .build();
        userRepository.save(user);

        Notification notification1 = Notification.builder()
                .user(user)
                .type(NotificationType.COMMENT)
                .title("title1")
                .content("content1")
                .build();
        Notification notification2 = Notification.builder()
                .user(user)
                .type(NotificationType.FEED_LIKE)
                .title("title2")
                .content("content2")
                .build();
        notificationRepository.save(notification1);
        notificationRepository.save(notification2);
    }

    @Test
    @DisplayName("사용자별 알림 목록 조회 (최신순)")
    void findByUserIdOrderByCreatedAtDescIdDescTest() {
        // when
        List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDescIdDesc(user.getId());

        // then
        assertThat(notifications).hasSize(2);
        assertThat(notifications.get(0).getTitle()).isEqualTo("title2"); // 최신순
        assertThat(notifications.get(1).getTitle()).isEqualTo("title1");
    }

    @Test
    @DisplayName("읽지 않은 알림 존재 여부 확인")
    void existsByUserIdAndIsReadFalseTest() {
        // when
        boolean exists = notificationRepository.existsByUserIdAndIsReadFalse(user.getId());

        // then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("전체 읽음 처리")
    void markAllAsReadByUserIdTest() {
        // when
        int updatedCount = notificationRepository.markAllAsReadByUserId(user.getId());

        // then
        assertThat(updatedCount).isEqualTo(2);
        boolean exists = notificationRepository.existsByUserIdAndIsReadFalse(user.getId());
        assertThat(exists).isFalse();
    }
}