package com.example.demo.service;

import com.example.demo.domain.entity.Notification;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.NotificationType;
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
@lombok.extern.slf4j.Slf4j // [추가] 로그 사용을 위한 어노테이션
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public List<Notification> getNotifications(User user) {
        // [디버깅] 알림 조회 시작 로그
        log.info(">>> [NotificationService] Fetching notifications for UserID: {}", user.getId());
        long startTime = System.currentTimeMillis();

        // user.getId()만 사용하여 detached entity (@Version null) 문제 방지
        // Repository에서 JOIN FETCH로 user 정보를 함께 가져옵니다.
        List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDescIdDesc(user.getId());
        
        long duration = System.currentTimeMillis() - startTime;
        log.info("<<< [NotificationService] Fetched {} notifications in {}ms", notifications.size(), duration);

        // [디버깅] 조회된 알림의 데이터 무결성 샘플링 확인 (첫 번째 알림 기준)
        if (!notifications.isEmpty()) {
            Notification first = notifications.get(0);
            log.debug("    Sample Notification - ID: {}, Type: {}, IsRead: {}, RefId: {}", 
                    first.getId(), first.getType(), first.getIsRead(), first.getRefId());
        } else {
            log.info("    No notifications found for UserID: {}", user.getId());
        }

        return notifications;
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));

        notification.markAsRead();
    }

    public boolean hasUnreadNotifications(User user) {
        // user.getId()만 사용하여 detached entity (@Version null) 문제 방지
        return notificationRepository.existsByUserIdAndIsReadFalse(user.getId());
    }

    @Transactional
    public void markAllAsRead(User user) {
        // [최적화] 벌크 업데이트 - 개별 엔티티 로딩 없이 한 번에 처리
        // user.getId()만 사용하여 detached entity (@Version null) 문제 방지
        notificationRepository.markAllAsReadByUserId(user.getId());
    }

    @Transactional
    public void deleteNotification(Long notificationId) {
        if (!notificationRepository.existsById(notificationId)) {
            throw new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND);
        }
        notificationRepository.deleteById(notificationId);
    }

    /**
     * 피드 삭제 시 관련 알림 삭제
     * - FEED_LIKE: refId = feedId
     * - COMMENT: refId = feedId
     */
    @Transactional
    public void deleteFeedRelatedNotifications(Long feedId) {
        String feedIdStr = String.valueOf(feedId);
        notificationRepository.deleteByRefIdAndType(feedIdStr, NotificationType.FEED_LIKE);
        notificationRepository.deleteByRefIdAndType(feedIdStr, NotificationType.COMMENT);
    }

    /**
     * 댓글 관련 알림 삭제 (REPLY, COMMENT_LIKE)
     * @param parentCommentIds 부모 댓글 ID 목록 (REPLY 알림 삭제용)
     * @param allCommentIds 모든 댓글 ID 목록 (COMMENT_LIKE 알림 삭제용)
     */
    @Transactional
    public void deleteCommentRelatedNotifications(List<Long> parentCommentIds, List<Long> allCommentIds) {
        if (!parentCommentIds.isEmpty()) {
            List<String> parentIds = parentCommentIds.stream()
                    .map(String::valueOf)
                    .toList();
            notificationRepository.deleteByRefIdInAndType(parentIds, NotificationType.REPLY);
        }

        if (!allCommentIds.isEmpty()) {
            List<String> commentIds = allCommentIds.stream()
                    .map(String::valueOf)
                    .toList();
            notificationRepository.deleteByRefIdInAndType(commentIds, NotificationType.COMMENT_LIKE);
        }
    }
}
