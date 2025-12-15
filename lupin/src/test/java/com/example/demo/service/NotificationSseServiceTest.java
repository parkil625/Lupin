package com.example.demo.service;

import com.example.demo.domain.entity.Notification;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.NotificationType;
import com.example.demo.repository.NotificationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class NotificationSseServiceTest {

    @InjectMocks
    private NotificationSseService notificationSseService;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private NotificationRepository notificationRepository;

    @Test
    @DisplayName("SSE 구독 성공")
    void subscribeTest() {
        // given
        Long userId = 1L;

        // when
        SseEmitter emitter = notificationSseService.subscribe(userId, null);

        // then
        assertThat(emitter).isNotNull();
    }

    @Test
    @DisplayName("기존 연결 종료 후 재구독")
    void resubscribeTest() {
        // given
        Long userId = 1L;
        SseEmitter firstEmitter = notificationSseService.subscribe(userId, null);

        // when
        SseEmitter secondEmitter = notificationSseService.subscribe(userId, null);

        // then
        assertThat(secondEmitter).isNotNull();
        assertThat(secondEmitter).isNotEqualTo(firstEmitter);
    }

    @Test
    @DisplayName("놓친 알림 재전송")
    void sendMissedNotificationsTest() {
        // given
        Long userId = 1L;
        Long lastEventId = 10L;
        User user = User.builder().id(userId).build();

        // NPE 방지를 위해 모든 필드 세팅
        Notification notification = Notification.builder()
                .id(11L)
                .user(user)
                .type(NotificationType.COMMENT)
                .title("title")
                .content("content")
                .isRead(false)
                .build();

        given(notificationRepository.findByUserIdAndIdGreaterThan(userId, lastEventId))
                .willReturn(List.of(notification));

        // when
        SseEmitter emitter = notificationSseService.subscribe(userId, lastEventId);

        // then
        assertThat(emitter).isNotNull();
    }
}