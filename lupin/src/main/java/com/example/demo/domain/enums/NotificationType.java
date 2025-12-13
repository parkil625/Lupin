package com.example.demo.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 알림 타입 Enum (Rich Enum Pattern)
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
@Getter
@RequiredArgsConstructor
public enum NotificationType {
    FEED_LIKE(true, false, true),      // refId=Feed.id, targetId=null, 취소 가능
    COMMENT(true, true, false),         // refId=Feed.id, targetId=Comment.id, 취소 불가
    COMMENT_LIKE(true, true, true),     // refId=Comment.id, targetId=Comment.id, 취소 가능
    REPLY(true, true, false),           // refId=부모Comment.id, targetId=Reply.id, 취소 불가
    FEED_DELETED(false, false, false),  // 시스템 알림
    COMMENT_DELETED(false, false, false); // 시스템 알림

    private final boolean hasRefId;      // 네비게이션용 참조 ID 존재 여부
    private final boolean hasTargetId;   // 하이라이트용 대상 ID 존재 여부
    private final boolean cancellable;   // 취소 가능 여부 (좋아요 취소 등)
}
