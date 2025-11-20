package com.example.demo.service;

import com.example.demo.domain.entity.Notification;
import com.example.demo.domain.entity.User;
import com.example.demo.dto.request.NotificationCreateRequest;
import com.example.demo.dto.response.NotificationResponse;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.NotificationRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 알림 관련 비즈니스 로직을 처리하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    /**
     * 알림 생성
     */
    @Transactional
    public NotificationResponse createNotification(NotificationCreateRequest request) {
        User user = findUserById(request.getUserId());

        Notification notification = Notification.builder()
                .type(request.getType())
                .title(request.getTitle())
                .content(request.getContent())
                .relatedId(request.getRelatedId())
                .refType(request.getRefType())
                .refId(request.getRefId())
                .build();

        notification.setUser(user);

        Notification savedNotification = notificationRepository.save(notification);

        log.info("알림 생성 완료 - notificationId: {}, userId: {}, type: {}",
                savedNotification.getId(), request.getUserId(), request.getType());

        return NotificationResponse.from(savedNotification);
    }

    /**
     * 특정 사용자의 알림 목록 조회 (페이징)
     */
    public Page<NotificationResponse> getNotificationsByUserId(Long userId, Pageable pageable) {
        return notificationRepository.findByUserId(userId, pageable)
                .map(NotificationResponse::from);
    }

    /**
     * 특정 사용자의 알림 목록 조회 (전체)
     */
    public List<NotificationResponse> getAllNotificationsByUserId(Long userId) {
        return notificationRepository.findByUserId(userId)
                .stream()
                .map(NotificationResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 특정 사용자의 읽지 않은 알림 조회
     */
    public List<NotificationResponse> getUnreadNotificationsByUserId(Long userId) {
        return notificationRepository.findUnreadNotificationsByUserId(userId)
                .stream()
                .map(NotificationResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 특정 사용자의 읽지 않은 알림 수 조회
     */
    public Long getUnreadCountByUserId(Long userId) {
        return notificationRepository.countUnreadByUserId(userId);
    }

    /**
     * 알림 상세 조회
     */
    public NotificationResponse getNotificationDetail(Long notificationId) {
        Notification notification = findNotificationById(notificationId);
        return NotificationResponse.from(notification);
    }

    /**
     * 알림 읽음 처리
     */
    @Transactional
    public NotificationResponse markAsRead(Long notificationId, Long userId) {
        Notification notification = findNotificationById(notificationId);

        // 알림 소유자 확인
        if (!notification.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "알림에 접근할 권한이 없습니다.");
        }

        notification.markAsRead();

        log.info("알림 읽음 처리 완료 - notificationId: {}, userId: {}", notificationId, userId);

        return NotificationResponse.from(notification);
    }

    /**
     * 특정 사용자의 알림 전체 읽음 처리
     */
    @Transactional
    public void markAllAsReadByUserId(Long userId) {
        notificationRepository.markAllAsReadByUserId(userId);

        log.info("사용자의 모든 알림 읽음 처리 완료 - userId: {}", userId);
    }

    /**
     * 알림 삭제
     */
    @Transactional
    public void deleteNotification(Long notificationId, Long userId) {
        Notification notification = findNotificationById(notificationId);

        // 알림 소유자 확인
        if (!notification.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "알림을 삭제할 권한이 없습니다.");
        }

        notificationRepository.delete(notification);

        log.info("알림 삭제 완료 - notificationId: {}, userId: {}", notificationId, userId);
    }

    /**
     * 특정 타입의 알림 조회
     */
    public Page<NotificationResponse> getNotificationsByType(Long userId, String type, Pageable pageable) {
        return notificationRepository.findByUserIdAndType(userId, type, pageable)
                .map(NotificationResponse::from);
    }

    /**
     * 오래된 읽은 알림 삭제 (30일 이상)
     */
    @Transactional
    public void deleteOldReadNotifications(Long userId) {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        notificationRepository.deleteOldReadNotificationsByUserId(userId, thirtyDaysAgo);

        log.info("오래된 읽은 알림 삭제 완료 - userId: {}", userId);
    }

    // === 다른 서비스에서 호출하는 헬퍼 메서드 ===

    /**
     * 좋아요 알림 생성
     */
    @Transactional
    public void createLikeNotification(Long feedOwnerId, Long likerUserId, Long feedId) {
        User feedOwner = findUserById(feedOwnerId);
        User liker = findUserById(likerUserId);

        // 자기 자신의 피드에 좋아요를 누른 경우 알림 생성하지 않음
        if (feedOwnerId.equals(likerUserId)) {
            return;
        }

        NotificationCreateRequest request = NotificationCreateRequest.builder()
                .type("like")
                .title("새로운 좋아요")
                .content(liker.getRealName() + "님이 회원님 피드를 좋아합니다.")
                .userId(feedOwnerId)
                .relatedId(feedId)
                .refType("FEED")
                .refId(String.valueOf(feedId))
                .build();

        createNotification(request);
    }

    /**
     * 댓글 알림 생성
     */
    @Transactional
    public void createCommentNotification(Long feedOwnerId, Long commenterUserId, Long feedId, Long commentId) {
        User feedOwner = findUserById(feedOwnerId);
        User commenter = findUserById(commenterUserId);

        // 자기 자신의 피드에 댓글을 단 경우 알림 생성하지 않음
        if (feedOwnerId.equals(commenterUserId)) {
            return;
        }

        NotificationCreateRequest request = NotificationCreateRequest.builder()
                .type("comment")
                .title("새로운 댓글")
                .content(commenter.getRealName() + "님이 회원님의 피드에 댓글을 남겼습니다.")
                .userId(feedOwnerId)
                .relatedId(commentId)
                .refType("FEED")
                .refId(String.valueOf(feedId))
                .build();

        createNotification(request);
    }

    /**
     * 답글 알림 생성
     */
    @Transactional
    public void createReplyNotification(Long parentCommentOwnerId, Long replierUserId, Long feedId, Long commentId) {
        User parentOwner = findUserById(parentCommentOwnerId);
        User replier = findUserById(replierUserId);

        // 자기 자신의 댓글에 답글을 단 경우 알림 생성하지 않음
        if (parentCommentOwnerId.equals(replierUserId)) {
            return;
        }

        NotificationCreateRequest request = NotificationCreateRequest.builder()
                .type("reply")
                .title("새로운 답글")
                .content(replier.getRealName() + "님이 회원님의 댓글에 답글을 남겼습니다.")
                .userId(parentCommentOwnerId)
                .relatedId(commentId)
                .refType("FEED")
                .refId(String.valueOf(feedId))
                .build();

        createNotification(request);
    }

    /**
     * 댓글 좋아요 알림 생성
     */
    @Transactional
    public void createCommentLikeNotification(Long commentOwnerId, Long likerUserId, Long feedId, Long commentId) {
        User commentOwner = findUserById(commentOwnerId);
        User liker = findUserById(likerUserId);

        // 자기 자신의 댓글에 좋아요를 누른 경우 알림 생성하지 않음
        if (commentOwnerId.equals(likerUserId)) {
            return;
        }

        NotificationCreateRequest request = NotificationCreateRequest.builder()
                .type("comment_like")
                .title("댓글 좋아요")
                .content(liker.getRealName() + "님이 회원님의 댓글을 좋아합니다.")
                .userId(commentOwnerId)
                .relatedId(commentId)
                .refType("FEED")
                .refId(String.valueOf(feedId))
                .build();

        createNotification(request);
    }

    /**
     * 챌린지 알림 생성
     */
    @Transactional
    public void createChallengeNotification(Long userId, String title, String content, Long challengeId) {
        NotificationCreateRequest request = NotificationCreateRequest.builder()
                .type("challenge")
                .title(title)
                .content(content)
                .userId(userId)
                .relatedId(challengeId)
                .build();

        createNotification(request);
    }

    /**
     * 예약 알림 생성
     */
    @Transactional
    public void createAppointmentNotification(Long userId, String title, String content, Long appointmentId) {
        NotificationCreateRequest request = NotificationCreateRequest.builder()
                .type("appointment")
                .title(title)
                .content(content)
                .userId(userId)
                .relatedId(appointmentId)
                .build();

        createNotification(request);
    }

    /**
     * 시스템 알림 생성 (추첨 당첨 등)
     */
    @Transactional
    public void createSystemNotification(Long userId, String content) {
        NotificationCreateRequest request = NotificationCreateRequest.builder()
                .type("system")
                .title("시스템 알림")
                .content(content)
                .userId(userId)
                .build();

        createNotification(request);
    }

    // === 헬퍼 메서드 ===

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private Notification findNotificationById(Long notificationId) {
        return notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));
    }
}
