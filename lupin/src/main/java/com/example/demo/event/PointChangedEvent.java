package com.example.demo.event;

/**
 * 포인트 변경 이벤트 - 트랜잭션 커밋 후 User.totalPoints 업데이트
 */
public record PointChangedEvent(
        Long userId,
        long amount
) {
    public static PointChangedEvent add(Long userId, long amount) {
        return new PointChangedEvent(userId, amount);
    }

    public static PointChangedEvent deduct(Long userId, long amount) {
        return new PointChangedEvent(userId, -amount);
    }
}
