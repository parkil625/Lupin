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
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService 테스트")
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationSseService notificationSseService;

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
        ReflectionTestUtils.setField(user, "id", 1L);
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

        given(notificationRepository.findByUserOrderByCreatedAtDescIdDesc(user))
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

    @Test
    @DisplayName("모든 알림을 읽음 처리한다")
    void markAllAsReadTest() {
        // given
        Notification notification1 = Notification.builder()
                .user(user)
                .type("LIKE")
                .title("좋아요 알림")
                .content("내용1")
                .build();

        Notification notification2 = Notification.builder()
                .user(user)
                .type("COMMENT")
                .title("댓글 알림")
                .content("내용2")
                .build();

        given(notificationRepository.findByUserAndIsReadFalse(user))
                .willReturn(List.of(notification1, notification2));

        // when
        notificationService.markAllAsRead(user);

        // then
        assertThat(notification1.getIsRead()).isTrue();
        assertThat(notification2.getIsRead()).isTrue();
    }

    @Test
    @DisplayName("알림을 삭제한다")
    void deleteNotificationTest() {
        // given
        Long notificationId = 1L;
        given(notificationRepository.existsById(notificationId)).willReturn(true);

        // when
        notificationService.deleteNotification(notificationId);

        // then
        verify(notificationRepository).deleteById(notificationId);
    }

    @Test
    @DisplayName("존재하지 않는 알림을 삭제하면 예외가 발생한다")
    void deleteNotificationNotFoundTest() {
        // given
        Long notificationId = 999L;
        given(notificationRepository.existsById(notificationId)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> notificationService.deleteNotification(notificationId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOTIFICATION_NOT_FOUND);
    }

    @Test
    @DisplayName("피드 좋아요 알림을 생성한다")
    void createFeedLikeNotificationTest() {
        // given
        User feedOwner = User.builder()
                .userId("owner")
                .password("password")
                .name("피드주인")
                .role(Role.MEMBER)
                .build();
        ReflectionTestUtils.setField(feedOwner, "id", 2L);

        User liker = User.builder()
                .userId("liker")
                .password("password")
                .name("좋아요누른사람")
                .role(Role.MEMBER)
                .build();
        ReflectionTestUtils.setField(liker, "id", 3L);

        Long feedId = 1L;

        given(notificationRepository.save(any(Notification.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        notificationService.createFeedLikeNotification(feedOwner, liker, feedId);

        // then
        verify(notificationRepository).save(org.mockito.ArgumentMatchers.argThat(notification ->
                notification.getUser().equals(feedOwner) &&
                notification.getType().equals("FEED_LIKE") &&
                notification.getRefId().equals(String.valueOf(feedId))
        ));
    }

    @Test
    @DisplayName("자기 자신의 피드에 좋아요 시 알림을 생성하지 않는다")
    void createFeedLikeNotificationSelfTest() {
        // given
        Long feedId = 1L;

        // when
        notificationService.createFeedLikeNotification(user, user, feedId);

        // then
        verify(notificationRepository, org.mockito.Mockito.never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("댓글 작성 알림을 생성한다")
    void createCommentNotificationTest() {
        // given
        User feedOwner = User.builder()
                .userId("owner")
                .password("password")
                .name("피드주인")
                .role(Role.MEMBER)
                .build();
        ReflectionTestUtils.setField(feedOwner, "id", 2L);

        User commenter = User.builder()
                .userId("commenter")
                .password("password")
                .name("댓글작성자")
                .role(Role.MEMBER)
                .build();
        ReflectionTestUtils.setField(commenter, "id", 3L);

        Long feedId = 1L;

        given(notificationRepository.save(any(Notification.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        notificationService.createCommentNotification(feedOwner, commenter, feedId);

        // then
        verify(notificationRepository).save(org.mockito.ArgumentMatchers.argThat(notification ->
                notification.getUser().equals(feedOwner) &&
                notification.getType().equals("COMMENT") &&
                notification.getRefId().equals(String.valueOf(feedId))
        ));
    }

    @Test
    @DisplayName("자기 자신의 피드에 댓글 시 알림을 생성하지 않는다")
    void createCommentNotificationSelfTest() {
        // given
        Long feedId = 1L;

        // when
        notificationService.createCommentNotification(user, user, feedId);

        // then
        verify(notificationRepository, org.mockito.Mockito.never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("댓글 좋아요 알림을 생성한다")
    void createCommentLikeNotificationTest() {
        // given
        User commentOwner = User.builder()
                .userId("owner")
                .password("password")
                .name("댓글주인")
                .role(Role.MEMBER)
                .build();
        ReflectionTestUtils.setField(commentOwner, "id", 2L);

        User liker = User.builder()
                .userId("liker")
                .password("password")
                .name("좋아요누른사람")
                .role(Role.MEMBER)
                .build();
        ReflectionTestUtils.setField(liker, "id", 3L);

        Long commentId = 1L;

        given(notificationRepository.save(any(Notification.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        notificationService.createCommentLikeNotification(commentOwner, liker, commentId);

        // then
        verify(notificationRepository).save(org.mockito.ArgumentMatchers.argThat(notification ->
                notification.getUser().equals(commentOwner) &&
                notification.getType().equals("COMMENT_LIKE") &&
                notification.getRefId().equals(String.valueOf(commentId))
        ));
    }

    @Test
    @DisplayName("자기 자신의 댓글에 좋아요 시 알림을 생성하지 않는다")
    void createCommentLikeNotificationSelfTest() {
        // given
        Long commentId = 1L;

        // when
        notificationService.createCommentLikeNotification(user, user, commentId);

        // then
        verify(notificationRepository, org.mockito.Mockito.never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("대댓글 알림을 생성한다")
    void createReplyNotificationTest() {
        // given
        User commentOwner = User.builder()
                .userId("owner")
                .password("password")
                .name("댓글주인")
                .role(Role.MEMBER)
                .build();
        ReflectionTestUtils.setField(commentOwner, "id", 2L);

        User replier = User.builder()
                .userId("replier")
                .password("password")
                .name("답글작성자")
                .role(Role.MEMBER)
                .build();
        ReflectionTestUtils.setField(replier, "id", 3L);

        Long commentId = 1L;

        given(notificationRepository.save(any(Notification.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        notificationService.createReplyNotification(commentOwner, replier, commentId);

        // then
        verify(notificationRepository).save(org.mockito.ArgumentMatchers.argThat(notification ->
                notification.getUser().equals(commentOwner) &&
                notification.getType().equals("REPLY") &&
                notification.getRefId().equals(String.valueOf(commentId))
        ));
    }

    @Test
    @DisplayName("자기 자신의 댓글에 답글 시 알림을 생성하지 않는다")
    void createReplyNotificationSelfTest() {
        // given
        Long commentId = 1L;

        // when
        notificationService.createReplyNotification(user, user, commentId);

        // then
        verify(notificationRepository, org.mockito.Mockito.never()).save(org.mockito.ArgumentMatchers.any());
    }
}
