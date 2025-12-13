package com.example.demo.event;

import com.example.demo.domain.entity.Notification;
import com.example.demo.domain.entity.User;
import com.example.demo.dto.response.NotificationResponse;
import com.example.demo.repository.NotificationRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.NotificationSseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 알림 이벤트 리스너 - 트랜잭션 커밋 후 비동기 처리
 *
 * 장점:
 * 1. 메인 로직(좋아요, 댓글 등)과 알림 로직 분리
 * 2. 알림 처리 실패해도 메인 로직에 영향 없음
 * 3. 비동기 처리로 응답 속도 향상
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationRepository notificationRepository;
    private final NotificationSseService notificationSseService;
    private final UserRepository userRepository;

    /**
     * 트랜잭션 커밋 후 비동기로 알림 처리
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleNotificationEvent(NotificationEvent event) {
        try {
            // 자기 자신에게 알림 보내지 않음
            if (event.getActorUserId() != null &&
                event.getTargetUserId().equals(event.getActorUserId())) {
                return;
            }

            User targetUser = userRepository.findById(event.getTargetUserId())
                    .orElse(null);
            if (targetUser == null) {
                log.warn("Target user not found: {}", event.getTargetUserId());
                return;
            }

            Notification notification = createNotification(event, targetUser);
            Notification saved = notificationRepository.save(notification);

            // SSE로 실시간 알림 전송
            notificationSseService.sendNotification(
                    event.getTargetUserId(),
                    NotificationResponse.from(saved)
            );

            log.debug("Notification sent: type={}, target={}", event.getType(), event.getTargetUserId());

        } catch (Exception e) {
            log.error("Failed to process notification event: {}", event, e);
            // 알림 처리 실패해도 메인 로직에 영향 없음
        }
    }

    private Notification createNotification(NotificationEvent event, User targetUser) {
        String title = switch (event.getType()) {
            case FEED_LIKE -> event.getActorName() + "님이 피드에 좋아요를 눌렀습니다";
            case COMMENT -> event.getActorName() + "님이 댓글을 남겼습니다";
            case COMMENT_LIKE -> event.getActorName() + "님이 댓글에 좋아요를 눌렀습니다";
            case REPLY -> event.getActorName() + "님이 답글을 남겼습니다";
            case FEED_DELETED -> "신고 누적으로 피드가 삭제되었습니다";
            case COMMENT_DELETED -> "신고 누적으로 댓글이 삭제되었습니다";
        };

        return Notification.builder()
                .user(targetUser)
                .type(event.getType())
                .title(title)
                .content(event.getContentPreview())
                .refId(event.getRefId() != null ? String.valueOf(event.getRefId()) : null)
                .targetId(event.getTargetId())
                .actorProfileImage(event.getActorProfileImage())
                .build();
    }
}
