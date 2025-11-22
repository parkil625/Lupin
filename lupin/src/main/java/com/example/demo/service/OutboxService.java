package com.example.demo.service;

import com.example.demo.domain.entity.Outbox;
import com.example.demo.repository.OutboxRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Outbox 서비스
 * 이벤트를 Outbox에 저장하고 처리하는 로직
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    private static final int MAX_RETRY = 3;

    /**
     * Outbox에 이벤트 저장
     */
    @Transactional
    public void saveEvent(String aggregateType, Long aggregateId, String eventType, Map<String, Object> payload) {
        try {
            String payloadJson = objectMapper.writeValueAsString(payload);

            Outbox outbox = Outbox.builder()
                    .aggregateType(aggregateType)
                    .aggregateId(aggregateId)
                    .eventType(eventType)
                    .payload(payloadJson)
                    .build();

            outboxRepository.save(outbox);

            log.debug("Outbox 이벤트 저장 - type: {}, id: {}, event: {}",
                    aggregateType, aggregateId, eventType);

        } catch (JsonProcessingException e) {
            log.error("Outbox payload 직렬화 실패", e);
            throw new RuntimeException("Outbox payload 직렬화 실패", e);
        }
    }

    /**
     * 대기 중인 이벤트 조회
     */
    @Transactional(readOnly = true)
    public List<Outbox> getPendingEvents() {
        return outboxRepository.findPendingEvents(MAX_RETRY);
    }

    /**
     * 재시도 가능한 실패 이벤트 조회
     */
    @Transactional(readOnly = true)
    public List<Outbox> getRetryableFailedEvents() {
        return outboxRepository.findRetryableFailedEvents(MAX_RETRY);
    }

    /**
     * 이벤트 처리 완료 표시
     */
    @Transactional
    public void markProcessed(Outbox outbox) {
        outbox.markProcessed();
        outboxRepository.save(outbox);
        log.debug("Outbox 이벤트 처리 완료 - id: {}", outbox.getId());
    }

    /**
     * 이벤트 처리 실패 표시
     */
    @Transactional
    public void markFailed(Outbox outbox, String errorMessage) {
        outbox.markFailed(errorMessage);
        outboxRepository.save(outbox);
        log.warn("Outbox 이벤트 처리 실패 - id: {}, retry: {}, error: {}",
                outbox.getId(), outbox.getRetryCount(), errorMessage);
    }

    /**
     * 실패 이벤트 재시도 대기로 변경
     */
    @Transactional
    public void markPendingForRetry(Outbox outbox) {
        outbox.markPending();
        outboxRepository.save(outbox);
    }

    /**
     * Payload를 Map으로 변환
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> parsePayload(String payload) {
        try {
            return objectMapper.readValue(payload, Map.class);
        } catch (JsonProcessingException e) {
            log.error("Outbox payload 역직렬화 실패", e);
            throw new RuntimeException("Outbox payload 역직렬화 실패", e);
        }
    }
}
