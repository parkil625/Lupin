package com.example.demo.service;

import com.example.demo.domain.entity.Notification;
import com.example.demo.domain.entity.User;
import com.example.demo.event.NotificationEvent;
import org.springframework.stereotype.Component;

/**
 * 알림 생성 팩토리 - 알림 생성 로직 캡슐화
 * NotificationEventListener에서 분리하여 단일 책임 원칙 준수
 */
@Component
public class NotificationFactory {

    /**
     * NotificationEvent로부터 Notification 엔티티 생성
     */
    public Notification create(NotificationEvent event, User targetUser) {
        String title = createTitle(event);

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

    /**
     * 알림 타입에 따른 제목 생성
     */
    private String createTitle(NotificationEvent event) {
        return switch (event.getType()) {
            case FEED_LIKE -> event.getActorName() + "님이 피드에 좋아요를 눌렀습니다";
            case COMMENT -> event.getActorName() + "님이 댓글을 남겼습니다";
            case COMMENT_LIKE -> event.getActorName() + "님이 댓글에 좋아요를 눌렀습니다";
            case REPLY -> event.getActorName() + "님이 답글을 남겼습니다";
            case FEED_DELETED -> "신고 누적으로 피드가 삭제되었습니다";
            case COMMENT_DELETED -> "신고 누적으로 댓글이 삭제되었습니다";
            case APPOINTMENT_REMINDER -> "진료 예약 알림";
            case AUCTION_WIN -> "축하합니다! 경매에 낙찰되셨습니다";
        };
    }
}
