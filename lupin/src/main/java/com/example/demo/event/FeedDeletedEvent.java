package com.example.demo.event;

/**
 * 피드 삭제 이벤트 - 트랜잭션 커밋 후 비동기 정리 작업에 사용
 */
public record FeedDeletedEvent(
        Long feedId,
        Long writerId
) {
    public static FeedDeletedEvent of(Long feedId, Long writerId) {
        return new FeedDeletedEvent(feedId, writerId);
    }
}
