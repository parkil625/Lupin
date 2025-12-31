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
    private static final String NOTIFICATION_DELETE_CHANNEL = "notification-delete"; // [추가] 채널명

    // userId -> SseEmitter 매핑
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    private static final Long SSE_TIMEOUT = 30 * 60 * 1000L; // 30분
    
    // [수정] 연결 유지 및 버퍼 플러시를 위해 10초 주기로 단축 (더 자주 뚫어줌)
    private static final long HEARTBEAT_INTERVAL = 10 * 1000L;

    // [최종 수정] Cloudflare의 Brotli 압축을 무력화하기 위한 '고엔트로피' 무작위 데이터
    // 단순 반복 문자열은 압축되면 100바이트 미만으로 줄어들어 버퍼링을 뚫지 못합니다.
    // 매번 다른 UUID를 조합하여 약 10KB 이상의 압축 불가능한 데이터를 만듭니다.
    private static final String DUMMY_DATA;
    
    static {
        StringBuilder sb = new StringBuilder();
        // UUID(36자) * 300개 = 약 10,800 바이트 (10KB 이상)
        for (int i = 0; i < 300; i++) {
            sb.append(java.util.UUID.randomUUID().toString());
        }
        DUMMY_DATA = sb.toString();
    }

    // 전용 스케줄러 (내부 관리 - Bean 충돌 방지)
    private ThreadPoolTaskScheduler heartbeatScheduler;
    private ScheduledFuture<?> heartbeatTask;

    @PostConstruct
    public void init() {
        // 전용 스케줄러 초기화 (Spring Bean이 아닌 내부 관리)
        heartbeatScheduler = new ThreadPoolTaskScheduler();
        heartbeatScheduler.setPoolSize(1);
        heartbeatScheduler.setThreadNamePrefix("sse-heartbeat-");
        heartbeatScheduler.setWaitForTasksToCompleteOnShutdown(true);
        heartbeatScheduler.setAwaitTerminationSeconds(10);
        heartbeatScheduler.initialize();

        // Heartbeat 작업 등록
        heartbeatTask = heartbeatScheduler.scheduleAtFixedRate(
                this::sendHeartbeatToAll,
                Duration.ofMillis(HEARTBEAT_INTERVAL)
        );
        log.info("SSE Heartbeat 스케줄러 시작 ({}ms 간격)", HEARTBEAT_INTERVAL);
    }

    @PreDestroy
    public void destroy() {
        if (heartbeatTask != null) {
            heartbeatTask.cancel(false);
        }
        if (heartbeatScheduler != null) {
            heartbeatScheduler.shutdown();
            log.info("SSE Heartbeat 스케줄러 종료");
        }
    }

    /**
     * 모든 연결에 heartbeat 전송 (연결 유지용)
     * - 병렬 처리로 대량 연결 시 병목 방지
     * - ConcurrentLinkedQueue로 스레드 안전한 dead connection 수집
     * - SseEmitter는 Thread-Safe 하지 않으므로 synchronized 처리
     */
    private void sendHeartbeatToAll() {
        if (emitters.isEmpty()) return;

        Queue<Long> deadConnections = new ConcurrentLinkedQueue<>();

        // 병렬 스트림으로 heartbeat 전송 (연결 수가 많을 때 성능 개선)
        emitters.entrySet().parallelStream().forEach(entry -> {
            Long userId = entry.getKey();
            SseEmitter emitter = entry.getValue();
            // SseEmitter Thread-Safety: 동시 send 방지
            synchronized (emitter) {
                try {
                    // [수정] 하트비트에도 5KB 패딩을 붙여 연결이 살아있음을 즉시 전송 (버퍼링 방지)
                    emitter.send(SseEmitter.event()
                            .name("heartbeat")
                            .data("ping")
                            .comment(DUMMY_DATA));
                } catch (IOException e) {
                    log.debug("Heartbeat 전송 실패, 연결 제거: userId={}", userId);
                    deadConnections.add(userId);
                }
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

        // [수정] 연결 즉시 버퍼를 뚫어주어야 함 (매우 중요: 브라우저가 연결 성공을 바로 인지하도록)
        try {
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data("connected")
                    .comment(DUMMY_DATA)); // [핵심] 5KB 패딩으로 초기 연결 즉시 전송
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
     * [추가] 알림 삭제 이벤트 발행 (Redis -> SSE)
     */
    public void sendNotificationDelete(Long userId, List<Long> notificationIds) {
        if (notificationIds == null || notificationIds.isEmpty()) return;
        try {
            NotificationDeleteMessage message = new NotificationDeleteMessage(userId, notificationIds);
            String json = objectMapper.writeValueAsString(message);

            // Redis로 발행 (다중 서버 환경 대응)
            redisTemplate.convertAndSend(NOTIFICATION_DELETE_CHANNEL, json);
            log.info("Redis Pub/Sub 알림 삭제 발행: userId={}, count={}", userId, notificationIds.size());
        } catch (JsonProcessingException e) {
            log.error("알림 삭제 메시지 발행 실패", e);
        }
    }

    /**
     * [추가] Redis 알림 삭제 메시지 수신 핸들러
     * (RedisConfig에서 notificationDeleteListenerAdapter가 이 메서드를 호출)
     */
    public void handleDeleteMessage(String message) {
        try {
            NotificationDeleteMessage deleteMsg = objectMapper.readValue(message, NotificationDeleteMessage.class);
            deliverDeleteToLocalEmitter(deleteMsg.getUserId(), deleteMsg.getNotificationIds());
        } catch (Exception e) {
            log.error("알림 삭제 메시지 처리 실패", e);
        }
    }

    /**
     * [추가] 로컬 Emitter로 삭제 이벤트 전송
     */
    private void deliverDeleteToLocalEmitter(Long userId, List<Long> notificationIds) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter == null) return;

        synchronized (emitter) {
            try {
                // 이벤트명: "notification-delete", 데이터: ID 리스트
                emitter.send(SseEmitter.event()
                        .name("notification-delete")
                        .data(notificationIds)
                        .comment(DUMMY_DATA)); // [수정] 5KB 패딩 적용 (확실한 전송 보장)

                log.info("SSE 알림 삭제 전송 성공: userId={}, ids={}", userId, notificationIds);
            } catch (IOException e) {
                log.error("SSE 알림 삭제 전송 실패: userId={}", userId, e);
                emitters.remove(userId);
            }
        }
    }

    // [추가] 삭제 메시지 DTO
    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    private static class NotificationDeleteMessage {
        private Long userId;
        private List<Long> notificationIds;
    }

    /**
     * 로컬 SSE Emitter로 알림 전달 (내부 메서드)
     * - id 필드에 알림 ID를 포함하여 Last-Event-ID 지원
     * - SseEmitter는 Thread-Safe 하지 않으므로 synchronized 처리
     */
    private void deliverToLocalEmitter(Long userId, NotificationResponse notification) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter == null) {
            log.debug("SSE 연결 없음 (이 서버): userId={}", userId);
            return;
        }

        // SseEmitter Thread-Safety: heartbeat와 동시 send 방지
        synchronized (emitter) {
            try {
                emitter.send(SseEmitter.event()
                        .id(String.valueOf(notification.getId()))
                        .name("notification")
                        .data(notification)
                        .comment(DUMMY_DATA)); // [수정] 5KB 패딩 적용 (즉시 전송 보장)
                
                log.info("SSE 알림 전송 성공: userId={}, type={}, eventId={}",
                        userId, notification.getType(), notification.getId());
            } catch (IOException e) {
                log.error("SSE 알림 전송 실패: userId={}", userId, e);
                emitters.remove(userId);
            }
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
