package com.example.demo.repository;

import com.example.demo.domain.entity.Outbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OutboxRepository extends JpaRepository<Outbox, Long> {

    // 처리 대기 중인 이벤트 조회 (재시도 횟수 제한)
    @Query("SELECT o FROM Outbox o WHERE o.status = 'PENDING' AND o.retryCount < :maxRetry ORDER BY o.createdAt ASC")
    List<Outbox> findPendingEvents(@Param("maxRetry") int maxRetry);

    // 실패한 이벤트 중 재시도 가능한 것 조회
    @Query("SELECT o FROM Outbox o WHERE o.status = 'FAILED' AND o.retryCount < :maxRetry ORDER BY o.createdAt ASC")
    List<Outbox> findRetryableFailedEvents(@Param("maxRetry") int maxRetry);

    // 오래된 처리 완료 이벤트 삭제 (정리용)
    @Modifying
    @Query("DELETE FROM Outbox o WHERE o.status = 'PROCESSED' AND o.processedAt < :before")
    int deleteProcessedEventsBefore(@Param("before") LocalDateTime before);

    // 특정 타입의 대기 중인 이벤트 수 조회
    Long countByAggregateTypeAndStatus(String aggregateType, String status);
}
