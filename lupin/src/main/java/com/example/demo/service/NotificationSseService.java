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
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationSseService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final NotificationRepository notificationRepository;

    private static final String NOTIFICATION_CHANNEL = "notification-update";

    // userId -> SseEmitter 매핑
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    private static final Long SSE_TIMEOUT = 30 * 60 * 1000L; // 30분
    private static final long HEARTBEAT_INTERVAL = 25; // 25초마다 heartbeat

    private ScheduledExecutorService heartbeatScheduler;

    @PostConstruct
    public void init() {
        // Heartbeat 스케줄러 시작
        heartbeatScheduler = Executors.newSingleThreadScheduledExecutor();
        heartbeatScheduler.scheduleAtFixedRate(
                this::sendHeartbeatToAll,
                HEARTBEAT_INTERVAL,
                HEARTBEAT_INTERVAL,
                TimeUnit.SECONDS
        );
        log.info("SSE Heartbeat 스케줄러 시작 ({}초 간격)", HEARTBEAT_INTERVAL);
    }

    @PreDestroy
    public void destroy() {
        if (heartbeatScheduler != null) {
            heartbeatScheduler.shutdown();
            log.info("SSE Heartbeat 스케줄러 종료");
        }
    }

    /**
     * 모든 연결에 heartbeat 전송 (연결 유지용)
     * - 병렬 처리로 대량 연결 시 병목 방지
     * - ConcurrentLinkedQueue로 스레드 안전한 dead connection 수집
     */
    private void sendHeartbeatToAll() {
        if (emitters.isEmpty()) {
            return;
        }

        Queue<Long> deadConnections = new ConcurrentLinkedQueue<>();

        // 병렬 스트림으로 heartbeat 전송 (연결 수가 많을 때 성능 개선)
        emitters.entrySet().parallelStream().forEach(entry -> {
            Long userId = entry.getKey();
            SseEmitter emitter = entry.getValue();
            try {
                emitter.send(SseEmitter.event()
                        .name("heartbeat")
                        .data("ping"));
            } catch (IOException e) {
                log.debug("Heartbeat 전송 실패, 연결 제거: userId={}", userId);
                deadConnections.add(userId);
            }
        });

        // 죽은 연결 제거
        deadConnections.forEach(emitters::remove);
    }

    /**
     * 새 SSE 연결 생성
     */
    public SseEmitter subscribe(Long userId) {
        return subscribe(userId, null);
    }

    /**
     * SSE 연결 생성 (Last-Event-ID 지원)
     * - 재연결 시 lastEventId 이후의 알림들을 자동으로 전송
     */
    public SseEmitter subscribe(Long userId, Long lastEventId) {
        // 기존 연결이 있으면 제거
        if (emitters.containsKey(userId)) {
            emitters.get(userId).complete();
            emitters.remove(userId);
        }

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        emitter.onCompletion(() -> {
            log.info("SSE 연결 완료: userId={}", userId);
            emitters.remove(userId);
        });

        emitter.onTimeout(() -> {
            log.info("SSE 연결 타임아웃: userId={}", userId);
            emitter.complete();
            emitters.remove(userId);
        });

        emitter.onError((e) -> {
            log.error("SSE 연결 에러: userId={}", userId, e);
            emitters.remove(userId);
        });

        emitters.put(userId, emitter);

        // 연결 확인용 초기 이벤트 전송
        try {
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data("SSE 연결 성공"));
        } catch (IOException e) {
            log.error("SSE 초기 이벤트 전송 실패: userId={}", userId, e);
            emitters.remove(userId);
        }

        // Last-Event-ID가 있으면 그 이후의 알림들을 전송
        if (lastEventId != null) {
            sendMissedNotifications(emitter, userId, lastEventId);
        }

        log.info("SSE 연결 생성: userId={}, lastEventId={}", userId, lastEventId);
        return emitter;
    }

    /**
     * 놓친 알림들을 재전송 (Last-Event-ID 폴백)
     */
    private void sendMissedNotifications(SseEmitter emitter, Long userId, Long lastEventId) {
        try {
            List<Notification> missedNotifications =
                    notificationRepository.findByUserIdAndIdGreaterThan(userId, lastEventId);

            for (Notification notification : missedNotifications) {
                NotificationResponse response = NotificationResponse.from(notification);
                emitter.send(SseEmitter.event()
                        .id(String.valueOf(notification.getId()))
                        .name("notification")
                        .data(response));
            }

            if (!missedNotifications.isEmpty()) {
                log.info("놓친 알림 {} 개 재전송: userId={}, lastEventId={}",
                        missedNotifications.size(), userId, lastEventId);
            }
        } catch (IOException e) {
            log.error("놓친 알림 재전송 실패: userId={}", userId, e);
        }
    }

    /**
     * 특정 사용자에게 알림 전송 (Redis Pub/Sub 통해 모든 서버로 브로드캐스트)
     */
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

    /**
     * Redis Pub/Sub 메시지 수신 핸들러
     * - RedisConfig에서 MessageListenerAdapter가 이 메서드를 호출
     */
    public void handleMessage(String message) {
        try {
            NotificationMessage notificationMessage = objectMapper.readValue(message, NotificationMessage.class);
            deliverToLocalEmitter(notificationMessage.getUserId(), notificationMessage.getNotification());
        } catch (JsonProcessingException e) {
            log.error("알림 역직렬화 실패: {}", message, e);
        }
    }

    /**
     * 로컬 SSE Emitter로 알림 전달 (내부 메서드)
     * - id 필드에 알림 ID를 포함하여 Last-Event-ID 지원
     */
    private void deliverToLocalEmitter(Long userId, NotificationResponse notification) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter == null) {
            log.debug("SSE 연결 없음 (이 서버): userId={}", userId);
            return;
        }

        try {
            emitter.send(SseEmitter.event()
                    .id(String.valueOf(notification.getId()))
                    .name("notification")
                    .data(notification));
            log.info("SSE 알림 전송 성공: userId={}, type={}, eventId={}",
                    userId, notification.getType(), notification.getId());
        } catch (IOException e) {
            log.error("SSE 알림 전송 실패: userId={}", userId, e);
            emitters.remove(userId);
        }
    }

    /**
     * 연결 해제
     */
    public void disconnect(Long userId) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter != null) {
            emitter.complete();
            emitters.remove(userId);
            log.info("SSE 연결 해제: userId={}", userId);
        }
    }
}
