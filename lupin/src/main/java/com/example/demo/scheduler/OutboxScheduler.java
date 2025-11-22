package com.example.demo.scheduler;

import com.example.demo.domain.entity.Outbox;
import com.example.demo.service.OutboxService;
import com.example.demo.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Outbox 스케줄러
 * 주기적으로 Outbox 이벤트를 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxScheduler {

    private final OutboxService outboxService;
    private final NotificationService notificationService;

    /**
     * 대기 중인 이벤트 처리 (5초마다)
     */
    @Scheduled(fixedDelay = 5000)
    public void processPendingEvents() {
        List<Outbox> pendingEvents = outboxService.getPendingEvents();

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.debug("Outbox 처리 시작 - 대기 이벤트: {}건", pendingEvents.size());

        for (Outbox event : pendingEvents) {
            processEvent(event);
        }
    }

    /**
     * 실패 이벤트 재시도 (1분마다)
     */
    @Scheduled(fixedDelay = 60000)
    public void retryFailedEvents() {
        List<Outbox> failedEvents = outboxService.getRetryableFailedEvents();

        if (failedEvents.isEmpty()) {
            return;
        }

        log.info("Outbox 재시도 시작 - 실패 이벤트: {}건", failedEvents.size());

        for (Outbox event : failedEvents) {
            outboxService.markPendingForRetry(event);
        }
    }

    /**
     * 개별 이벤트 처리
     */
    private void processEvent(Outbox event) {
        try {
            Map<String, Object> payload = outboxService.parsePayload(event.getPayload());

            switch (event.getAggregateType()) {
                case "NOTIFICATION" -> processNotificationEvent(event, payload);
                case "EMAIL" -> processEmailEvent(event, payload);
                case "PUSH" -> processPushEvent(event, payload);
                default -> {
                    log.warn("알 수 없는 Outbox 타입: {}", event.getAggregateType());
                    outboxService.markFailed(event, "Unknown aggregate type");
                }
            }

        } catch (Exception e) {
            log.error("Outbox 이벤트 처리 실패 - id: {}", event.getId(), e);
            outboxService.markFailed(event, e.getMessage());
        }
    }

    /**
     * 알림 이벤트 처리
     */
    private void processNotificationEvent(Outbox event, Map<String, Object> payload) {
        // 현재는 DB에 알림이 이미 저장되어 있으므로 바로 완료 처리
        // 향후 푸시 알림이나 WebSocket 전송 로직 추가 가능

        Long notificationId = ((Number) payload.get("notificationId")).longValue();
        String eventType = event.getEventType();

        log.debug("알림 이벤트 처리 - notificationId: {}, type: {}", notificationId, eventType);

        // 여기에 푸시 알림 전송 로직 추가
        // pushNotificationService.send(notificationId);

        outboxService.markProcessed(event);
    }

    /**
     * 이메일 이벤트 처리 (향후 확장용)
     */
    private void processEmailEvent(Outbox event, Map<String, Object> payload) {
        // 이메일 발송 로직
        // emailService.send(payload);

        outboxService.markProcessed(event);
    }

    /**
     * 푸시 알림 이벤트 처리 (향후 확장용)
     */
    private void processPushEvent(Outbox event, Map<String, Object> payload) {
        // FCM/APNs 푸시 알림 발송 로직
        // pushService.send(payload);

        outboxService.markProcessed(event);
    }
}
