package com.example.demo.service;

import com.example.demo.dto.response.NotificationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class NotificationSseService {

    // userId -> SseEmitter 매핑
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    private static final Long SSE_TIMEOUT = 30 * 60 * 1000L; // 30분

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
