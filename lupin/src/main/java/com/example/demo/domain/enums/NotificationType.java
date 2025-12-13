package com.example.demo.domain.enums;

/**
 * 알림 타입 Enum
 *
 * | 타입            | refId (네비게이션)  | targetId (하이라이트) | 삭제 시점                       |
 * |-----------------|--------------------|-----------------------|--------------------------------|
 * | FEED_LIKE       | Feed.id            | null                  | 피드 삭제, 좋아요 취소 시         |
 * | COMMENT         | Feed.id            | Comment.id            | 피드 삭제, 댓글 삭제 시           |
 * | COMMENT_LIKE    | Comment.id         | Comment.id            | 댓글 삭제, 좋아요 취소 시         |
 * | REPLY           | 부모 Comment.id    | Reply.id              | 부모댓글 삭제, 대댓글 삭제 시     |
 * | FEED_DELETED    | null               | null                  | -                              |
 * | COMMENT_DELETED | null               | null                  | -                              |
 */
public enum NotificationType {
    FEED_LIKE,
    COMMENT,
    COMMENT_LIKE,
    REPLY,
    FEED_DELETED,
    COMMENT_DELETED
}
