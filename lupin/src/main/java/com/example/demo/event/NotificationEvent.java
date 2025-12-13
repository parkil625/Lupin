package com.example.demo.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 알림 이벤트 - 트랜잭션 커밋 후 비동기로 처리
 */
@Getter
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
    private final String actorProfileImage; // 알림 발생시킨 사용자 프로필 이미지
    private final Long refId;             // 참조 ID (네비게이션용: 피드ID, 부모댓글ID 등)
    private final Long targetId;          // 하이라이트 대상 ID (댓글ID, 답글ID)
    private final String contentPreview;  // 콘텐츠 미리보기 (유튜브 스타일)

    private NotificationEvent(Type type, Long targetUserId, Long actorUserId, String actorName, String actorProfileImage, Long refId, Long targetId, String contentPreview) {
        this.type = type;
        this.targetUserId = targetUserId;
        this.actorUserId = actorUserId;
        this.actorName = actorName;
        this.actorProfileImage = actorProfileImage;
        this.refId = refId;
        this.targetId = targetId;
        this.contentPreview = contentPreview;
    }

    // 피드 좋아요 이벤트
    public static NotificationEvent feedLike(Long targetUserId, Long actorUserId, String actorName, String actorProfileImage, Long feedId, String feedContent) {
        return new NotificationEvent(Type.FEED_LIKE, targetUserId, actorUserId, actorName, actorProfileImage, feedId, null, truncateContent(feedContent));
    }

    // 댓글 이벤트
    public static NotificationEvent comment(Long targetUserId, Long actorUserId, String actorName, String actorProfileImage, Long feedId, Long commentId, String commentContent) {
        return new NotificationEvent(Type.COMMENT, targetUserId, actorUserId, actorName, actorProfileImage, feedId, commentId, truncateContent(commentContent));
    }

    // 댓글 좋아요 이벤트
    public static NotificationEvent commentLike(Long targetUserId, Long actorUserId, String actorName, String actorProfileImage, Long commentId, String commentContent) {
        return new NotificationEvent(Type.COMMENT_LIKE, targetUserId, actorUserId, actorName, actorProfileImage, commentId, commentId, truncateContent(commentContent));
    }

    // 대댓글 이벤트
    public static NotificationEvent reply(Long targetUserId, Long actorUserId, String actorName, String actorProfileImage, Long parentCommentId, Long replyId, String replyContent) {
        return new NotificationEvent(Type.REPLY, targetUserId, actorUserId, actorName, actorProfileImage, parentCommentId, replyId, truncateContent(replyContent));
    }

    // 피드 삭제 알림
    public static NotificationEvent feedDeleted(Long targetUserId) {
        return new NotificationEvent(Type.FEED_DELETED, targetUserId, null, null, null, null, null, null);
    }

    // 댓글 삭제 알림
    public static NotificationEvent commentDeleted(Long targetUserId) {
        return new NotificationEvent(Type.COMMENT_DELETED, targetUserId, null, null, null, null, null, null);
    }

    /**
     * 콘텐츠를 50자로 자르고 말줄임표 추가
     */
    private static String truncateContent(String content) {
        if (content == null || content.isEmpty()) {
            return null;
        }
        if (content.length() <= 50) {
            return content;
        }
        return content.substring(0, 50) + "...";
    }
}
