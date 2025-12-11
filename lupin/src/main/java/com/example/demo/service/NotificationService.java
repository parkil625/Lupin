package com.example.demo.service;

import com.example.demo.domain.entity.Notification;
import com.example.demo.domain.entity.User;
import com.example.demo.dto.response.NotificationResponse;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 알림 서비스
 *
 * ========================================
 * 알림 타입별 refId 구조
 * ========================================
 *
 * | 타입            | refId 값           | 참조 대상      | 삭제 시점                       |
 * |-----------------|--------------------|---------------|--------------------------------|
 * | FEED_LIKE       | Feed.id            | 해당 피드      | 피드 삭제, 좋아요 취소 시         |
 * | COMMENT         | Feed.id            | 해당 피드      | 피드 삭제, 댓글 삭제 시           |
 * | COMMENT_LIKE    | Comment.id         | 해당 댓글      | 댓글 삭제, 좋아요 취소 시         |
 * | REPLY           | 부모 Comment.id    | 부모 댓글      | 부모댓글 삭제, 대댓글 삭제 시     |
 * | FEED_DELETED    | null               | -             | -                              |
 * | COMMENT_DELETED | null               | -             | -                              |
 *
 * ========================================
 * 프론트엔드 알림 클릭 시 이동 로직
 * ========================================
 * - FEED_LIKE: refId가 바로 Feed.id → 해당 피드로 이동
 * - COMMENT: refId가 바로 Feed.id → 해당 피드의 댓글로 이동
 * - COMMENT_LIKE: refId(Comment.id)로 Comment 조회 → Feed.id 얻어서 이동
 * - REPLY: refId(부모 Comment.id)로 Comment 조회 → Feed.id 얻어서 이동
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationSseService notificationSseService;

    public List<Notification> getNotifications(User user) {
        return notificationRepository.findByUserOrderByCreatedAtDescIdDesc(user);
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));

        notification.markAsRead();
    }

    public boolean hasUnreadNotifications(User user) {
        return notificationRepository.existsByUserAndIsReadFalse(user);
    }

    @Transactional
    public void markAllAsRead(User user) {
        List<Notification> unreadNotifications = notificationRepository.findByUserAndIsReadFalse(user);
        unreadNotifications.forEach(Notification::markAsRead);
    }

    @Transactional
    public void deleteNotification(Long notificationId) {
        if (!notificationRepository.existsById(notificationId)) {
            throw new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND);
        }
        notificationRepository.deleteById(notificationId);
    }

    /**
     * 피드 좋아요 알림 생성
     * @param feedId 좋아요가 눌린 피드의 ID (refId로 저장)
     */
    @Transactional
    public void createFeedLikeNotification(User feedOwner, User liker, Long feedId) {
        if (feedOwner.getId().equals(liker.getId())) {
            return;
        }

        Notification notification = Notification.builder()
                .user(feedOwner)
                .type("FEED_LIKE")
                .title(liker.getName() + "님이 피드에 좋아요를 눌렀습니다")
                .refId(String.valueOf(feedId))
                .build();

        Notification saved = notificationRepository.save(notification);
        notificationSseService.sendNotification(feedOwner.getId(), NotificationResponse.from(saved));
    }

    /**
     * 댓글 알림 생성
     * @param feedId 댓글이 달린 피드의 ID (refId로 저장)
     */
    @Transactional
    public void createCommentNotification(User feedOwner, User commenter, Long feedId) {
        if (feedOwner.getId().equals(commenter.getId())) {
            return;
        }

        Notification notification = Notification.builder()
                .user(feedOwner)
                .type("COMMENT")
                .title(commenter.getName() + "님이 댓글을 남겼습니다")
                .refId(String.valueOf(feedId))
                .build();

        Notification saved = notificationRepository.save(notification);
        notificationSseService.sendNotification(feedOwner.getId(), NotificationResponse.from(saved));
    }

    /**
     * 댓글 좋아요 알림 생성
     * @param commentId 좋아요가 눌린 댓글의 ID (refId로 저장)
     */
    @Transactional
    public void createCommentLikeNotification(User commentOwner, User liker, Long commentId) {
        if (commentOwner.getId().equals(liker.getId())) {
            return;
        }

        Notification notification = Notification.builder()
                .user(commentOwner)
                .type("COMMENT_LIKE")
                .title(liker.getName() + "님이 댓글에 좋아요를 눌렀습니다")
                .refId(String.valueOf(commentId))
                .build();

        Notification saved = notificationRepository.save(notification);
        notificationSseService.sendNotification(commentOwner.getId(), NotificationResponse.from(saved));
    }

    /**
     * 답글(대댓글) 알림 생성
     * @param parentCommentId 답글이 달린 부모 댓글의 ID (refId로 저장)
     */
    @Transactional
    public void createReplyNotification(User commentOwner, User replier, Long parentCommentId) {
        if (commentOwner.getId().equals(replier.getId())) {
            return;
        }

        Notification notification = Notification.builder()
                .user(commentOwner)
                .type("REPLY")
                .title(replier.getName() + "님이 답글을 남겼습니다")
                .refId(String.valueOf(parentCommentId))
                .build();

        Notification saved = notificationRepository.save(notification);
        notificationSseService.sendNotification(commentOwner.getId(), NotificationResponse.from(saved));
    }

    @Transactional
    public void createFeedDeletedByReportNotification(User feedOwner) {
        Notification notification = Notification.builder()
                .user(feedOwner)
                .type("FEED_DELETED")
                .title("신고 누적으로 피드가 삭제되었습니다")
                .build();

        Notification saved = notificationRepository.save(notification);
        notificationSseService.sendNotification(feedOwner.getId(), NotificationResponse.from(saved));
    }

    @Transactional
    public void createCommentDeletedByReportNotification(User commentOwner) {
        Notification notification = Notification.builder()
                .user(commentOwner)
                .type("COMMENT_DELETED")
                .title("신고 누적으로 댓글이 삭제되었습니다")
                .build();

        Notification saved = notificationRepository.save(notification);
        notificationSseService.sendNotification(commentOwner.getId(), NotificationResponse.from(saved));
    }
}
