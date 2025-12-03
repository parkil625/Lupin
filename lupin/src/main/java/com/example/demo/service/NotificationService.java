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

    @Transactional
    public void createFeedLikeNotification(User feedOwner, User liker, Long feedLikeId) {
        if (feedOwner.getId().equals(liker.getId())) {
            return;
        }

        Notification notification = Notification.builder()
                .user(feedOwner)
                .type("FEED_LIKE")
                .title(liker.getName() + "님이 피드에 좋아요를 눌렀습니다")
                .refId(String.valueOf(feedLikeId))
                .build();

        Notification saved = notificationRepository.save(notification);
        notificationSseService.sendNotification(feedOwner.getId(), NotificationResponse.from(saved));
    }

    @Transactional
    public void createCommentNotification(User feedOwner, User commenter, Long commentId) {
        if (feedOwner.getId().equals(commenter.getId())) {
            return;
        }

        Notification notification = Notification.builder()
                .user(feedOwner)
                .type("COMMENT")
                .title(commenter.getName() + "님이 댓글을 남겼습니다")
                .refId(String.valueOf(commentId))
                .build();

        Notification saved = notificationRepository.save(notification);
        notificationSseService.sendNotification(feedOwner.getId(), NotificationResponse.from(saved));
    }

    @Transactional
    public void createCommentLikeNotification(User commentOwner, User liker, Long commentLikeId) {
        if (commentOwner.getId().equals(liker.getId())) {
            return;
        }

        Notification notification = Notification.builder()
                .user(commentOwner)
                .type("COMMENT_LIKE")
                .title(liker.getName() + "님이 댓글에 좋아요를 눌렀습니다")
                .refId(String.valueOf(commentLikeId))
                .build();

        Notification saved = notificationRepository.save(notification);
        notificationSseService.sendNotification(commentOwner.getId(), NotificationResponse.from(saved));
    }

    @Transactional
    public void createReplyNotification(User commentOwner, User replier, Long replyId) {
        if (commentOwner.getId().equals(replier.getId())) {
            return;
        }

        Notification notification = Notification.builder()
                .user(commentOwner)
                .type("REPLY")
                .title(replier.getName() + "님이 답글을 남겼습니다")
                .refId(String.valueOf(replyId))
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
