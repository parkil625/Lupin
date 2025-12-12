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
 * 알림 타입별 refId / targetId 구조
 * ========================================
 *
 * | 타입            | refId (네비게이션)  | targetId (하이라이트) | 삭제 시점                       |
 * |-----------------|--------------------|-----------------------|--------------------------------|
 * | FEED_LIKE       | Feed.id            | null                  | 피드 삭제, 좋아요 취소 시         |
 * | COMMENT         | Feed.id            | Comment.id            | 피드 삭제, 댓글 삭제 시           |
 * | COMMENT_LIKE    | Comment.id         | Comment.id            | 댓글 삭제, 좋아요 취소 시         |
 * | REPLY           | 부모 Comment.id    | Reply.id              | 부모댓글 삭제, 대댓글 삭제 시     |
 * | FEED_DELETED    | null               | null                  | -                              |
 * | COMMENT_DELETED | null               | null                  | -                              |
 *
 * ========================================
 * 프론트엔드 알림 클릭 시 로직
 * ========================================
 * - refId: 네비게이션용 (피드 이동, 부모 댓글로 스크롤 등)
 * - targetId: 하이라이트할 댓글/답글 ID
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
        // [최적화] 벌크 업데이트 - 개별 엔티티 로딩 없이 한 번에 처리
        notificationRepository.markAllAsRead(user);
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
     * @param commentId 생성된 댓글의 ID (targetId로 저장 - 하이라이트용)
     */
    @Transactional
    public void createCommentNotification(User feedOwner, User commenter, Long feedId, Long commentId) {
        if (feedOwner.getId().equals(commenter.getId())) {
            return;
        }

        Notification notification = Notification.builder()
                .user(feedOwner)
                .type("COMMENT")
                .title(commenter.getName() + "님이 댓글을 남겼습니다")
                .refId(String.valueOf(feedId))
                .targetId(commentId)
                .build();

        Notification saved = notificationRepository.save(notification);
        notificationSseService.sendNotification(feedOwner.getId(), NotificationResponse.from(saved));
    }

    /**
     * 댓글 좋아요 알림 생성
     * @param commentId 좋아요가 눌린 댓글의 ID (refId, targetId 모두 저장)
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
                .targetId(commentId)
                .build();

        Notification saved = notificationRepository.save(notification);
        notificationSseService.sendNotification(commentOwner.getId(), NotificationResponse.from(saved));
    }

    /**
     * 답글(대댓글) 알림 생성
     * @param parentCommentId 답글이 달린 부모 댓글의 ID (refId로 저장)
     * @param replyId 생성된 답글의 ID (targetId로 저장 - 하이라이트용)
     */
    @Transactional
    public void createReplyNotification(User commentOwner, User replier, Long parentCommentId, Long replyId) {
        if (commentOwner.getId().equals(replier.getId())) {
            return;
        }

        Notification notification = Notification.builder()
                .user(commentOwner)
                .type("REPLY")
                .title(replier.getName() + "님이 답글을 남겼습니다")
                .refId(String.valueOf(parentCommentId))
                .targetId(replyId)
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
