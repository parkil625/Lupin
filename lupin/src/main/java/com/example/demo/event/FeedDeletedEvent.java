package com.example.demo.event;

import java.util.List;

/**
 * 피드 삭제 이벤트 - 트랜잭션 커밋 후 비동기 정리 작업에 사용
 * 각 도메인별 리스너가 자신의 데이터를 정리하도록 필요한 ID들을 포함
 */
public record FeedDeletedEvent(
        Long feedId,
        Long writerId,
        List<Long> parentCommentIds,
        List<Long> allCommentIds
) {
    public static FeedDeletedEvent of(Long feedId, Long writerId,
                                       List<Long> parentCommentIds, List<Long> allCommentIds) {
        return new FeedDeletedEvent(feedId, writerId, parentCommentIds, allCommentIds);
    }
}
