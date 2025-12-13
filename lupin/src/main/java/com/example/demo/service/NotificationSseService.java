package com.example.demo.service;

import com.example.demo.dto.response.NotificationResponse;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class NotificationSseService {

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
     */
    private void sendHeartbeatToAll() {
        if (emitters.isEmpty()) {
            return;
        }

        List<Long> deadConnections = new ArrayList<>();

        emitters.forEach((userId, emitter) -> {
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

        log.info("SSE 연결 생성: userId={}", userId);
        return emitter;
    }

    /**
     * 특정 사용자에게 알림 전송
     */
    public void sendNotification(Long userId, NotificationResponse notification) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter == null) {
            log.debug("SSE 연결 없음: userId={}", userId);
            return;
        }

        try {
            emitter.send(SseEmitter.event()
                    .name("notification")
                    .data(notification));
            log.info("SSE 알림 전송 성공: userId={}, type={}", userId, notification.getType());
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
