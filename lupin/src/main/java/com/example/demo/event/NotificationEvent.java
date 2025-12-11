package com.example.demo.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 알림 이벤트 - 트랜잭션 커밋 후 비동기로 처리
 */
@Getter
@RequiredArgsConstructor
public class NotificationEvent {

    public enum Type {
        FEED_LIKE,
        COMMENT,
        COMMENT_LIKE,
        REPLY,
        FEED_DELETED,
        COMMENT_DELETED
    }

    private final Type type;
    private final Long targetUserId;      // 알림 받을 사용자
    private final Long actorUserId;       // 알림 발생시킨 사용자
    private final String actorName;       // 알림 발생시킨 사용자 이름
    private final Long refId;             // 참조 ID (피드ID, 댓글ID 등)

    // 피드 좋아요 이벤트
    public static NotificationEvent feedLike(Long targetUserId, Long actorUserId, String actorName, Long feedId) {
        return new NotificationEvent(Type.FEED_LIKE, targetUserId, actorUserId, actorName, feedId);
    }

    // 댓글 이벤트
    public static NotificationEvent comment(Long targetUserId, Long actorUserId, String actorName, Long feedId) {
        return new NotificationEvent(Type.COMMENT, targetUserId, actorUserId, actorName, feedId);
    }

    // 댓글 좋아요 이벤트
    public static NotificationEvent commentLike(Long targetUserId, Long actorUserId, String actorName, Long commentId) {
        return new NotificationEvent(Type.COMMENT_LIKE, targetUserId, actorUserId, actorName, commentId);
    }

    // 대댓글 이벤트
    public static NotificationEvent reply(Long targetUserId, Long actorUserId, String actorName, Long parentCommentId) {
        return new NotificationEvent(Type.REPLY, targetUserId, actorUserId, actorName, parentCommentId);
    }

    // 피드 삭제 알림
    public static NotificationEvent feedDeleted(Long targetUserId) {
        return new NotificationEvent(Type.FEED_DELETED, targetUserId, null, null, null);
    }

    // 댓글 삭제 알림
    public static NotificationEvent commentDeleted(Long targetUserId) {
        return new NotificationEvent(Type.COMMENT_DELETED, targetUserId, null, null, null);
    }
}
