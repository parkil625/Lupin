package com.example.demo.service;

import com.example.demo.domain.entity.Notification;
import com.example.demo.dto.NotificationMessage;
import com.example.demo.dto.response.NotificationResponse;
import com.example.demo.repository.NotificationRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledFuture;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationSseService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final NotificationRepository notificationRepository;

    private static final String NOTIFICATION_CHANNEL = "notification-update";
    private static final long SSE_TIMEOUT_MS = Duration.ofMinutes(30).toMillis();
    private static final long HEARTBEAT_INTERVAL_SECONDS = 25;

    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();
    private ThreadPoolTaskScheduler heartbeatScheduler;
    private ScheduledFuture<?> heartbeatTask;

    @PostConstruct
    public void init() {
        heartbeatScheduler = new ThreadPoolTaskScheduler();
        heartbeatScheduler.setPoolSize(1);
        heartbeatScheduler.setThreadNamePrefix("sse-heartbeat-");
        heartbeatScheduler.setWaitForTasksToCompleteOnShutdown(true);
        heartbeatScheduler.setAwaitTerminationSeconds(10);
        heartbeatScheduler.initialize();

        heartbeatTask = heartbeatScheduler.scheduleAtFixedRate(
                this::sendHeartbeatToAll,
                Duration.ofSeconds(HEARTBEAT_INTERVAL_SECONDS)
        );
        log.info("SSE Heartbeat 스케줄러 시작 ({}초 간격)", HEARTBEAT_INTERVAL_SECONDS);
    }

    @PreDestroy
    public void destroy() {
        if (heartbeatTask != null) heartbeatTask.cancel(false);
        if (heartbeatScheduler != null) heartbeatScheduler.shutdown();
        log.info("SSE Heartbeat 스케줄러 종료");
    }

    public SseEmitter subscribe(Long userId, Long lastEventId) {
        SseEmitter emitter = createEmitter(userId);
        sendInitialEvent(emitter, userId);
        if (lastEventId != null) {
            sendMissedNotifications(emitter, userId, lastEventId);
        }
        log.info("SSE 연결 생성: userId={}, lastEventId={}", userId, lastEventId);
        return emitter;
    }

    public void disconnect(Long userId) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter != null) {
            emitter.complete();
        }
    }

    public void sendNotification(Long userId, NotificationResponse notification) {
        try {
            NotificationMessage message = NotificationMessage.builder()
                    .userId(userId)
                    .notification(notification)
                    .build();
            String json = objectMapper.writeValueAsString(message);
            redisTemplate.convertAndSend(NOTIFICATION_CHANNEL, json);
            log.info("Redis Pub/Sub 알림 발행: userId={}, type={}", userId, notification.getType());
        } catch (JsonProcessingException e) {
            log.error("알림 직렬화 실패: userId={}", userId, e);
        }
    }

    public void handleMessage(String message) {
        try {
            NotificationMessage notificationMessage = objectMapper.readValue(message, NotificationMessage.class);
            deliverToLocalEmitter(notificationMessage.getUserId(), notificationMessage.getNotification());
        } catch (JsonProcessingException e) {
            log.error("알림 역직렬화 실패: {}", message, e);
        }
    }

    private SseEmitter createEmitter(Long userId) {
        emitters.computeIfPresent(userId, (key, oldEmitter) -> {
            oldEmitter.complete();
            return null;
        });

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        registerEventHandlers(emitter, userId);
        emitters.put(userId, emitter);
        return emitter;
    }

    private void registerEventHandlers(SseEmitter emitter, Long userId) {
        emitter.onCompletion(() -> {
            log.info("SSE 연결 완료: userId={}", userId);
            emitters.remove(userId);
        });
        emitter.onTimeout(() -> {
            log.info("SSE 연결 타임아웃: userId={}", userId);
            emitters.remove(userId);
            emitter.complete();
        });
        emitter.onError((e) -> {
            log.error("SSE 연결 에러: userId={}", userId, e);
            emitters.remove(userId);
        });
    }

    private void sendInitialEvent(SseEmitter emitter, Long userId) {
        try {
            emitter.send(SseEmitter.event().name("connect").data("SSE 연결 성공"));
        } catch (IOException e) {
            log.error("SSE 초기 이벤트 전송 실패: userId={}", userId, e);
            emitters.remove(userId);
        }
    }

    private void sendMissedNotifications(SseEmitter emitter, Long userId, Long lastEventId) {
        List<Notification> missedNotifications = notificationRepository.findByUserIdAndIdGreaterThan(userId, lastEventId);
        for (Notification notification : missedNotifications) {
            NotificationResponse response = NotificationResponse.from(notification);
            sendSseEvent(emitter, String.valueOf(notification.getId()), "notification", response);
        }
        if (!missedNotifications.isEmpty()) {
            log.info("놓친 알림 {} 개 재전송: userId={}, lastEventId={}", missedNotifications.size(), userId, lastEventId);
        }
    }

    private void deliverToLocalEmitter(Long userId, NotificationResponse notification) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter != null) {
            sendSseEvent(emitter, String.valueOf(notification.getId()), "notification", notification);
        }
    }

    private void sendHeartbeatToAll() {
        emitters.forEach((userId, emitter) -> {
            try {
                emitter.send(SseEmitter.event().name("heartbeat").data("ping"));
            } catch (IOException e) {
                log.debug("Heartbeat 전송 실패, 연결 제거: userId={}", userId);
                emitters.remove(userId);
            }
        });
    }

    private void sendSseEvent(SseEmitter emitter, String id, String name, Object data) {
        try {
            synchronized (emitter) {
                emitter.send(SseEmitter.event().id(id).name(name).data(data));
            }
        } catch (IOException e) {
            log.error("SSE 이벤트 전송 실패: id={}, name={}", id, name, e);
            // 에러 발생 시 emitter 제거는 onError 콜백에서 처리
        }
    }
}
