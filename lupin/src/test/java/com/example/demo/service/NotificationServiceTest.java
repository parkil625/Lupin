package com.example.demo.service;

import com.example.demo.domain.entity.Notification;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.Role;
import com.example.demo.dto.request.NotificationCreateRequest;
import com.example.demo.dto.response.NotificationResponse;
import com.example.demo.exception.BusinessException;
import com.example.demo.repository.NotificationRepository;
import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService 테스트")
class NotificationServiceTest {

    @InjectMocks
    private NotificationService notificationService;

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private OutboxService outboxService;

    private User receiver;
    private User sender;
    private Notification notification;

    @BeforeEach
    void setUp() {
        receiver = User.builder()
                .id(1L)
                .userId("receiver01")
                .realName("수신자")
                .role(Role.MEMBER)
                .build();

        sender = User.builder()
                .id(2L)
                .userId("sender01")
                .realName("발신자")
                .role(Role.MEMBER)
                .build();

        notification = Notification.builder()
                .id(1L)
                .type("like")
                .title("새로운 좋아요")
                .content("발신자님이 좋아요를 눌렀습니다.")
                .build();
        notification.setUser(receiver);
        ReflectionTestUtils.setField(notification, "createdAt", LocalDateTime.now());
    }

    @Nested
    @DisplayName("좋아요 알림 생성")
    class CreateLikeNotification {

        @Test
        @DisplayName("좋아요 알림 생성 성공")
        void createLikeNotification_Success() {
            // given
            given(userRepository.findById(1L)).willReturn(Optional.of(receiver));
            given(userRepository.findById(2L)).willReturn(Optional.of(sender));
            given(notificationRepository.save(any(Notification.class))).willAnswer(invocation -> {
                Notification saved = invocation.getArgument(0);
                ReflectionTestUtils.setField(saved, "id", 10L);
                return saved;
            });

            // when
            notificationService.createLikeNotification(1L, 2L, 1L);

            // then
            then(notificationRepository).should().save(any(Notification.class));
        }

        @Test
        @DisplayName("자기 자신 좋아요시 알림 생성 안함")
        void createLikeNotification_SameUser_NoNotification() {
            // given
            given(userRepository.findById(1L)).willReturn(Optional.of(receiver));

            // when
            notificationService.createLikeNotification(1L, 1L, 1L);

            // then
            then(notificationRepository).should(never()).save(any(Notification.class));
        }

        @Test
        @DisplayName("존재하지 않는 수신자 알림 생성 실패")
        void createLikeNotification_ReceiverNotFound_ThrowsException() {
            // given
            given(userRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> notificationService.createLikeNotification(999L, 2L, 1L))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("댓글 알림 생성")
    class CreateCommentNotification {

        @Test
        @DisplayName("댓글 알림 생성 성공")
        void createCommentNotification_Success() {
            // given
            given(userRepository.findById(1L)).willReturn(Optional.of(receiver));
            given(userRepository.findById(2L)).willReturn(Optional.of(sender));
            given(notificationRepository.save(any(Notification.class))).willAnswer(invocation -> {
                Notification saved = invocation.getArgument(0);
                ReflectionTestUtils.setField(saved, "id", 10L);
                return saved;
            });

            // when
            notificationService.createCommentNotification(1L, 2L, 1L, 1L);

            // then
            then(notificationRepository).should().save(any(Notification.class));
        }
    }

    @Nested
    @DisplayName("답글 알림 생성")
    class CreateReplyNotification {

        @Test
        @DisplayName("답글 알림 생성 성공")
        void createReplyNotification_Success() {
            // given
            given(userRepository.findById(1L)).willReturn(Optional.of(receiver));
            given(userRepository.findById(2L)).willReturn(Optional.of(sender));
            given(notificationRepository.save(any(Notification.class))).willAnswer(invocation -> {
                Notification saved = invocation.getArgument(0);
                ReflectionTestUtils.setField(saved, "id", 10L);
                return saved;
            });

            // when
            notificationService.createReplyNotification(1L, 2L, 1L, 1L);

            // then
            then(notificationRepository).should().save(any(Notification.class));
        }
    }

    @Nested
    @DisplayName("읽지 않은 알림 조회")
    class GetUnreadNotifications {

        @Test
        @DisplayName("읽지 않은 알림 조회 성공")
        void getUnreadNotifications_Success() {
            // given
            Notification notification2 = Notification.builder()
                    .id(2L)
                    .type("comment")
                    .title("새 댓글")
                    .content("테스트")
                    .build();
            notification2.setUser(receiver);
            ReflectionTestUtils.setField(notification2, "createdAt", LocalDateTime.now());

            given(notificationRepository.findUnreadNotificationsByUserId(1L))
                    .willReturn(Arrays.asList(notification, notification2));

            // when
            List<NotificationResponse> result = notificationService.getUnreadNotificationsByUserId(1L);

            // then
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("읽지 않은 알림이 없는 경우")
        void getUnreadNotifications_Empty() {
            // given
            given(notificationRepository.findUnreadNotificationsByUserId(1L))
                    .willReturn(Arrays.asList());

            // when
            List<NotificationResponse> result = notificationService.getUnreadNotificationsByUserId(1L);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("알림 읽음 처리")
    class MarkAsRead {

        @Test
        @DisplayName("알림 읽음 처리 성공")
        void markAsRead_Success() {
            // given
            given(notificationRepository.findById(1L)).willReturn(Optional.of(notification));

            // when
            notificationService.markAsRead(1L, 1L);

            // then
            assertThat(notification.getIsRead()).isEqualTo("Y");
        }

        @Test
        @DisplayName("다른 사용자 알림 읽음 처리 실패")
        void markAsRead_NotOwner_ThrowsException() {
            // given
            given(notificationRepository.findById(1L)).willReturn(Optional.of(notification));

            // when & then
            assertThatThrownBy(() -> notificationService.markAsRead(1L, 999L))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("존재하지 않는 알림 읽음 처리 실패")
        void markAsRead_NotFound_ThrowsException() {
            // given
            given(notificationRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> notificationService.markAsRead(999L, 1L))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("읽지 않은 알림 수 조회")
    class GetUnreadCount {

        @Test
        @DisplayName("읽지 않은 알림 수 조회 성공")
        void getUnreadCount_Success() {
            // given
            given(notificationRepository.countUnreadByUserId(1L)).willReturn(5L);

            // when
            Long result = notificationService.getUnreadCountByUserId(1L);

            // then
            assertThat(result).isEqualTo(5L);
        }

        @Test
        @DisplayName("읽지 않은 알림이 없는 경우")
        void getUnreadCount_Zero() {
            // given
            given(notificationRepository.countUnreadByUserId(1L)).willReturn(0L);

            // when
            Long result = notificationService.getUnreadCountByUserId(1L);

            // then
            assertThat(result).isEqualTo(0L);
        }
    }

    @Nested
    @DisplayName("모든 알림 읽음 처리")
    class MarkAllAsRead {

        @Test
        @DisplayName("모든 알림 읽음 처리 성공")
        void markAllAsRead_Success() {
            // when
            notificationService.markAllAsReadByUserId(1L);

            // then
            then(notificationRepository).should().markAllAsReadByUserId(1L);
        }
    }

    @Nested
    @DisplayName("시스템 알림 생성")
    class CreateSystemNotification {

        @Test
        @DisplayName("시스템 알림 생성 성공")
        void createSystemNotification_Success() {
            // given
            given(userRepository.findById(1L)).willReturn(Optional.of(receiver));
            given(notificationRepository.save(any(Notification.class))).willAnswer(invocation -> {
                Notification saved = invocation.getArgument(0);
                ReflectionTestUtils.setField(saved, "id", 10L);
                return saved;
            });

            // when
            notificationService.createSystemNotification(1L, "축하합니다! 추첨에 당첨되었습니다.");

            // then
            then(notificationRepository).should().save(any(Notification.class));
        }
    }
}
