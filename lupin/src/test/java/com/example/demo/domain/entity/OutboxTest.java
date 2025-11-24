package com.example.demo.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.demo.domain.enums.OutboxStatus;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Outbox 엔티티 테스트")
class OutboxTest {

    @Test
    @DisplayName("Outbox 생성 - 기본값 확인")
    void create_DefaultValues() {
        // given & when
        Outbox outbox = Outbox.builder()
                .aggregateType("NOTIFICATION")
                .aggregateId(1L)
                .eventType("LIKE")
                .payload("{\"userId\": 1}")
                .build();

        // then
        assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(outbox.getRetryCount()).isEqualTo(0);
        assertThat(outbox.getCreatedAt()).isNotNull();
        assertThat(outbox.getProcessedAt()).isNull();
    }

    @Test
    @DisplayName("처리 완료 표시")
    void markProcessed_Success() {
        // given
        Outbox outbox = Outbox.builder()
                .aggregateType("NOTIFICATION")
                .aggregateId(1L)
                .eventType("COMMENT")
                .build();

        // when
        outbox.markProcessed();

        // then
        assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.PROCESSED);
        assertThat(outbox.getProcessedAt()).isNotNull();
    }

    @Test
    @DisplayName("처리 실패 표시")
    void markFailed_Success() {
        // given
        Outbox outbox = Outbox.builder()
                .aggregateType("EMAIL")
                .aggregateId(1L)
                .eventType("WELCOME")
                .build();

        // when
        outbox.markFailed("Connection timeout");

        // then
        assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(outbox.getRetryCount()).isEqualTo(1);
        assertThat(outbox.getErrorMessage()).isEqualTo("Connection timeout");
    }

    @Test
    @DisplayName("여러 번 실패 - 재시도 횟수 증가")
    void markFailed_MultipleRetries() {
        // given
        Outbox outbox = Outbox.builder()
                .aggregateType("SMS")
                .aggregateId(1L)
                .eventType("VERIFY")
                .build();

        // when
        outbox.markFailed("First error");
        outbox.markFailed("Second error");
        outbox.markFailed("Third error");

        // then
        assertThat(outbox.getRetryCount()).isEqualTo(3);
        assertThat(outbox.getErrorMessage()).isEqualTo("Third error");
    }

    @Test
    @DisplayName("재시도 대기 상태로 변경")
    void markPending_Success() {
        // given
        Outbox outbox = Outbox.builder()
                .aggregateType("NOTIFICATION")
                .aggregateId(1L)
                .eventType("REPLY")
                .build();
        outbox.markFailed("Temporary error");

        // when
        outbox.markPending();

        // then
        assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.PENDING);
    }

    @Test
    @DisplayName("상태 전환 - PENDING -> PROCESSED")
    void stateTransition_PendingToProcessed() {
        // given
        Outbox outbox = Outbox.builder()
                .aggregateType("NOTIFICATION")
                .aggregateId(1L)
                .eventType("LIKE")
                .build();

        assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.PENDING);

        // when
        outbox.markProcessed();

        // then
        assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.PROCESSED);
    }

    @Test
    @DisplayName("상태 전환 - FAILED -> PENDING -> PROCESSED")
    void stateTransition_FailedToPendingToProcessed() {
        // given
        Outbox outbox = Outbox.builder()
                .aggregateType("NOTIFICATION")
                .aggregateId(1L)
                .eventType("COMMENT")
                .build();

        // when & then
        outbox.markFailed("Network error");
        assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.FAILED);

        outbox.markPending();
        assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.PENDING);

        outbox.markProcessed();
        assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.PROCESSED);
    }
}
