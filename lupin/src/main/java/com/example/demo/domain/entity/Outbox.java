package com.example.demo.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Outbox 엔티티
 * 외부 시스템(푸시 알림, 이메일 등)과의 통신을 트랜잭션으로 보장
 */
@Entity
@Table(name = "outbox")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Outbox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "aggregate_type", nullable = false, length = 50)
    private String aggregateType; // NOTIFICATION, EMAIL, SMS 등

    @Column(name = "aggregate_id", nullable = false)
    private Long aggregateId; // 관련 엔티티 ID

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType; // LIKE, COMMENT, REPLY 등

    @Column(columnDefinition = "TEXT")
    private String payload; // JSON 형태의 데이터

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING"; // PENDING, PROCESSED, FAILED

    @Column(name = "retry_count")
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "error_message")
    private String errorMessage;

    /**
     * 처리 완료
     */
    public void markProcessed() {
        this.status = "PROCESSED";
        this.processedAt = LocalDateTime.now();
    }

    /**
     * 처리 실패
     */
    public void markFailed(String errorMessage) {
        this.status = "FAILED";
        this.retryCount++;
        this.errorMessage = errorMessage;
    }

    /**
     * 재시도 대기
     */
    public void markPending() {
        this.status = "PENDING";
    }
}
