package com.example.demo.service;

import com.example.demo.domain.entity.Notification;
import com.example.demo.domain.entity.User;
import com.example.demo.repository.NotificationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @InjectMocks
    private NotificationReadService notificationReadService;

    @InjectMocks
    private NotificationCommandService notificationCommandService;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private User user;

    @Test
    @DisplayName("알림 목록 조회")
    void getNotificationsTest() {
        // given
        Notification notification = Notification.builder().build();
        given(notificationRepository.findByUserIdOrderByCreatedAtDescIdDesc(any())).willReturn(List.of(notification));

        // when
        List<Notification> result = notificationReadService.getNotifications(user);

        // then
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("알림 읽음 처리")
    void markAsReadTest() {
        // given
        Long notificationId = 1L;
        Notification notification = Notification.builder().build();
        given(notificationRepository.findById(notificationId)).willReturn(Optional.of(notification));

        // when
        notificationCommandService.markAsRead(notificationId);

        // then
        assertThat(notification.getIsRead()).isTrue();
    }
}