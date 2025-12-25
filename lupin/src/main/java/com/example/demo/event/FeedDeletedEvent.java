package com.example.demo.event;

import java.util.List;

/**
 * 피드 삭제 이벤트 - 트랜잭션 커밋 후 비동기 정리 작업에 사용
 * 각 도메인별 리스너가 자신의 데이터를 정리하도록 필요한 ID들과 이미지 경로를 포함
 */
public record FeedDeletedEvent(
        Long feedId,
        Long writerId,
        List<Long> parentCommentIds,
        List<Long> allCommentIds,
        List<String> imageUrls // [추가] S3 이미지 삭제를 위해 필요
) {
    public static FeedDeletedEvent of(Long feedId, Long writerId,
                                       List<Long> parentCommentIds, List<Long> allCommentIds,
                                       List<String> imageUrls) {
        return new FeedDeletedEvent(feedId, writerId, parentCommentIds, allCommentIds, imageUrls);
    }
}