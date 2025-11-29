package com.example.demo.service;

import com.example.demo.domain.entity.Notification;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.Role;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService 테스트")
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .userId("user")
                .password("password")
                .name("사용자")
                .role(Role.MEMBER)
                .build();
    }

    @Test
    @DisplayName("사용자의 알림 목록을 조회한다")
    void getNotificationsTest() {
        // given
        Notification notification1 = Notification.builder()
                .user(user)
                .type("LIKE")
                .title("좋아요 알림")
                .content("누군가 좋아요를 눌렀습니다")
                .build();

        Notification notification2 = Notification.builder()
                .user(user)
                .type("COMMENT")
                .title("댓글 알림")
                .content("누군가 댓글을 달았습니다")
                .build();

        given(notificationRepository.findByUserOrderByCreatedAtDesc(user))
                .willReturn(List.of(notification1, notification2));

        // when
        List<Notification> result = notificationService.getNotifications(user);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getType()).isEqualTo("LIKE");
    }

    @Test
    @DisplayName("알림을 읽음 처리한다")
    void markAsReadTest() {
        // given
        Long notificationId = 1L;
        Notification notification = Notification.builder()
                .user(user)
                .type("LIKE")
                .title("좋아요 알림")
                .content("내용")
                .build();

        given(notificationRepository.findById(notificationId)).willReturn(Optional.of(notification));

        // when
        notificationService.markAsRead(notificationId);

        // then
        assertThat(notification.getIsRead()).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 알림을 읽음 처리하면 예외가 발생한다")
    void markAsReadNotFoundTest() {
        // given
        Long notificationId = 999L;
        given(notificationRepository.findById(notificationId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> notificationService.markAsRead(notificationId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOTIFICATION_NOT_FOUND);
    }

    @Test
    @DisplayName("읽지 않은 알림이 있는지 확인한다")
    void hasUnreadNotificationsTest() {
        // given
        given(notificationRepository.existsByUserAndIsReadFalse(user)).willReturn(true);

        // when
        boolean result = notificationService.hasUnreadNotifications(user);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("읽지 않은 알림이 없으면 false를 반환한다")
    void hasNoUnreadNotificationsTest() {
        // given
        given(notificationRepository.existsByUserAndIsReadFalse(user)).willReturn(false);

        // when
        boolean result = notificationService.hasUnreadNotifications(user);

        // then
        assertThat(result).isFalse();
    }
}
