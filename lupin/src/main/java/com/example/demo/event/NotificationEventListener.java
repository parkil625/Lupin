package com.example.demo.event;

import com.example.demo.domain.entity.Notification;
import com.example.demo.domain.entity.User;
import com.example.demo.dto.response.NotificationResponse;
import com.example.demo.repository.NotificationRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.NotificationFactory;
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
    private final NotificationFactory notificationFactory;
    private final UserRepository userRepository;
    private final com.example.demo.repository.FeedLikeRepository feedLikeRepository; // [추가] 좋아요 개수 조회용

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

            // [수정] 알림 중복 방지 및 뭉치기 전략 적용
            Notification savedNotification;

            // 1. FEED_LIKE(좋아요) 타입인 경우 덮어쓰기/뭉치기 시도
            if ("FEED_LIKE".equals(event.getType().name())) {
                // [뭉치기 로직] 현재 피드의 전체 좋아요 개수 조회
                long likeCount = feedLikeRepository.countByFeedId(event.getRefId());
                String newTitle;

                if (likeCount <= 1) {
                    newTitle = event.getActorName() + "님이 회원님의 게시물을 좋아합니다.";
                } else {
                    // 예: "철수님 외 2명이 회원님의 게시물을 좋아합니다."
                    newTitle = event.getActorName() + "님 외 " + (likeCount - 1) + "명이 회원님의 게시물을 좋아합니다.";
                }

                // [수정] ID 대신 User 객체로 조회 (JPA 매핑 문제 해결)
                java.util.Optional<Notification> existingOpt = notificationRepository
                        .findTopByUserAndTypeAndRefIdAndIsReadFalseOrderByCreatedAtDesc(
                                targetUser,
                                event.getType(),
                                String.valueOf(event.getRefId())
                        );

                if (existingOpt.isPresent()) {
                    Notification existing = existingOpt.get();
                    log.info("[Notification] 기존 알림 뭉치기 (ID: {}). Count: {}", existing.getId(), likeCount);

                    // 기존 알림 업데이트: 타이틀(뭉친 메시지), 프로필이미지(최신 누른 사람), 시간(Now)
                    existing.updateForAggregation(newTitle, event.getActorProfileImage());
                    savedNotification = notificationRepository.save(existing);
                } else {
                    // 기존 알림 없으면 신규 생성
                    log.info("[Notification] 신규 알림 생성. Count: {}", likeCount);
                    Notification newNotif = notificationFactory.create(event, targetUser);
                    
                    // Factory는 기본 메시지만 만드므로, 뭉치기 메시지로 덮어쓰기
                    newNotif.updateForAggregation(newTitle, event.getActorProfileImage());
                    
                    savedNotification = notificationRepository.save(newNotif);
                }
            } 
            // 2. 그 외 타입(댓글, 시스템 알림 등)은 항상 신규 생성
            else {
                savedNotification = notificationRepository.save(notificationFactory.create(event, targetUser));
            }

            // SSE로 실시간 알림 전송
            notificationSseService.sendNotification(
                    event.getTargetUserId(),
                    NotificationResponse.from(savedNotification)
            );

            log.debug("Notification sent: type={}, target={}, id={}", 
                    event.getType(), event.getTargetUserId(), savedNotification.getId());

        } catch (Exception e) {
            log.error("Failed to process notification event: {}", event, e);
            // 알림 처리 실패해도 메인 로직에 영향 없음
        }
    }
}
