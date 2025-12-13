package com.example.demo.service;

import com.example.demo.dto.response.NotificationResponse;
import com.example.demo.repository.NotificationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class NotificationSseServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private NotificationRepository notificationRepository;

    private ObjectMapper objectMapper;

    private NotificationSseService notificationSseService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        notificationSseService = new NotificationSseService(redisTemplate, objectMapper, notificationRepository);
    }

    @Test
    @DisplayName("SSE 구독 시 SseEmitter를 반환한다")
    void subscribeReturnsSseEmitter() {
        // given
        Long userId = 1L;

        // when
        SseEmitter emitter = notificationSseService.subscribe(userId);

        // then
        assertThat(emitter).isNotNull();
    }

    @Test
    @DisplayName("같은 유저가 다시 구독하면 기존 연결을 대체한다")
    void subscribeReplacesExistingConnection() {
        // given
        Long userId = 1L;
        SseEmitter firstEmitter = notificationSseService.subscribe(userId);

        // when
        SseEmitter secondEmitter = notificationSseService.subscribe(userId);

        // then
        assertThat(secondEmitter).isNotNull();
        assertThat(secondEmitter).isNotSameAs(firstEmitter);
    }

    @Test
    @DisplayName("연결 해제 후 알림 전송 시 에러가 발생하지 않는다")
    void sendNotificationAfterDisconnectDoesNotThrow() {
        // given
        Long userId = 1L;
        notificationSseService.subscribe(userId);
        notificationSseService.disconnect(userId);

        NotificationResponse notification = NotificationResponse.builder()
                .id(1L)
                .type("FEED_LIKE")
                .title("테스트 알림")
                .build();

        // when & then (예외 발생하지 않음)
        notificationSseService.sendNotification(userId, notification);
    }

    @Test
    @DisplayName("구독하지 않은 유저에게 알림 전송 시 에러가 발생하지 않는다")
    void sendNotificationToUnsubscribedUserDoesNotThrow() {
        // given
        Long userId = 999L;
        NotificationResponse notification = NotificationResponse.builder()
                .id(1L)
                .type("COMMENT")
                .title("테스트 알림")
                .build();

        // when & then (예외 발생하지 않음)
        notificationSseService.sendNotification(userId, notification);
    }

    @Test
    @DisplayName("연결 해제는 여러 번 호출해도 에러가 발생하지 않는다")
    void disconnectMultipleTimesDoesNotThrow() {
        // given
        Long userId = 1L;
        notificationSseService.subscribe(userId);

        // when & then (예외 발생하지 않음)
        notificationSseService.disconnect(userId);
        notificationSseService.disconnect(userId);
        notificationSseService.disconnect(userId);
    }
}
