package com.example.demo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Redisson 기반 분산 락 서비스
 * - 멀티 인스턴스 환경에서 동시성 제어
 * - 자동 락 해제 (try-with-resources 패턴)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DistributedLockService {

    private final RedissonClient redissonClient;

    private static final long DEFAULT_WAIT_TIME = 5L;
    private static final long DEFAULT_LEASE_TIME = 10L;

    // ==================== 락 키 프리픽스 ====================

    private static final String DRAW_LOCK = "lock:draw:";
    private static final String PRIZE_STOCK_LOCK = "lock:prize:stock:";
    private static final String CHALLENGE_JOIN_LOCK = "lock:challenge:join:";
    private static final String TICKET_ISSUE_LOCK = "lock:ticket:issue:";
    private static final String POINTS_LOCK = "lock:points:";
    private static final String FEED_LIKE_LOCK = "lock:feed:like:";

    // ==================== 기본 락 연산 ====================

    /**
     * 락 획득 후 작업 실행 (자동 해제)
     */
    public <T> T executeWithLock(String lockKey, Supplier<T> task) {
        return executeWithLock(lockKey, DEFAULT_WAIT_TIME, DEFAULT_LEASE_TIME, task);
    }

    /**
     * 락 획득 후 작업 실행 (커스텀 타임아웃)
     */
    public <T> T executeWithLock(String lockKey, long waitTime, long leaseTime, Supplier<T> task) {
        RLock lock = redissonClient.getLock(lockKey);
        boolean acquired = false;

        try {
            acquired = lock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS);

            if (!acquired) {
                log.warn("락 획득 실패: key={}", lockKey);
                throw new LockAcquisitionException("락 획득 실패: " + lockKey);
            }

            log.debug("락 획득 성공: key={}", lockKey);
            return task.get();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LockAcquisitionException("락 대기 중 인터럽트: " + lockKey, e);
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("락 해제: key={}", lockKey);
            }
        }
    }

    /**
     * void 반환 작업용 락 실행
     */
    public void executeWithLock(String lockKey, Runnable task) {
        executeWithLock(lockKey, () -> {
            task.run();
            return null;
        });
    }

    // ==================== 도메인별 락 메서드 ====================

    /**
     * 추첨 실행 락
     */
    public <T> T withDrawLock(Long drawId, Supplier<T> task) {
        return executeWithLock(DRAW_LOCK + drawId, task);
    }

    /**
     * 상품 재고 차감 락
     */
    public <T> T withPrizeStockLock(Long prizeId, Supplier<T> task) {
        return executeWithLock(PRIZE_STOCK_LOCK + prizeId, task);
    }

    /**
     * 챌린지 참가 락 (사용자별)
     */
    public <T> T withChallengeJoinLock(Long challengeId, Long userId, Supplier<T> task) {
        String key = CHALLENGE_JOIN_LOCK + challengeId + ":" + userId;
        return executeWithLock(key, task);
    }

    /**
     * 추첨권 발급 락 (사용자별)
     */
    public <T> T withTicketIssueLock(Long userId, Supplier<T> task) {
        return executeWithLock(TICKET_ISSUE_LOCK + userId, task);
    }

    /**
     * 포인트 변경 락 (사용자별)
     */
    public <T> T withPointsLock(Long userId, Supplier<T> task) {
        return executeWithLock(POINTS_LOCK + userId, task);
    }

    /**
     * 피드 좋아요 락 (피드+사용자별)
     */
    public <T> T withFeedLikeLock(Long feedId, Long userId, Supplier<T> task) {
        String key = FEED_LIKE_LOCK + feedId + ":" + userId;
        return executeWithLock(key, task);
    }

    // ==================== 페어 락 (다중 리소스) ====================

    /**
     * 멀티 락 (여러 리소스 동시 락)
     */
    public <T> T executeWithMultiLock(Supplier<T> task, String... lockKeys) {
        RLock[] locks = new RLock[lockKeys.length];
        for (int i = 0; i < lockKeys.length; i++) {
            locks[i] = redissonClient.getLock(lockKeys[i]);
        }

        RLock multiLock = redissonClient.getMultiLock(locks);
        boolean acquired = false;

        try {
            acquired = multiLock.tryLock(DEFAULT_WAIT_TIME, DEFAULT_LEASE_TIME, TimeUnit.SECONDS);

            if (!acquired) {
                log.warn("멀티 락 획득 실패: keys={}", (Object) lockKeys);
                throw new LockAcquisitionException("멀티 락 획득 실패");
            }

            return task.get();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LockAcquisitionException("멀티 락 대기 중 인터럽트", e);
        } finally {
            if (acquired) {
                multiLock.unlock();
            }
        }
    }

    // ==================== 예외 클래스 ====================

    public static class LockAcquisitionException extends RuntimeException {
        public LockAcquisitionException(String message) {
            super(message);
        }

        public LockAcquisitionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
