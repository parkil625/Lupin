package com.example.demo.dto.response;

import com.example.demo.domain.entity.Notification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 알림 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {

    private Long id;
    private String type;
    private String title;
    private String content;
    private Boolean isRead;
    @Deprecated
    private Long relatedId;  // 하위 호환성을 위해 유지
    private String refType;  // FEED, COMMENT, CHAT 등
    private String refId;    // 관련 엔티티 ID
    private Long userId;
    private String userName;
    private LocalDateTime createdAt;
    private Long feedId;     // 피드 ID
    private Long commentId;  // 댓글 ID (댓글 알림인 경우)

    /**
     * Entity -> Response DTO 변환
     */
    public static NotificationResponse from(Notification notification) {
        Long feedId = null;
        Long commentId = null;

        // refId에서 feedId 추출
        if (notification.getRefId() != null && "FEED".equals(notification.getRefType())) {
            try {
                feedId = Long.parseLong(notification.getRefId());
            } catch (NumberFormatException e) {
                // 무시
            }
        }

        // comment/reply/comment_like 타입인 경우 relatedId는 commentId
        if ("comment".equals(notification.getType()) || "reply".equals(notification.getType()) || "comment_like".equals(notification.getType())) {
            commentId = notification.getRelatedId();
        } else if ("like".equals(notification.getType())) {
            // like 타입인 경우 relatedId는 feedId
            if (feedId == null) {
                feedId = notification.getRelatedId();
            }
        }

        return NotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType())
                .title(notification.getTitle())
                .content(notification.getContent())
                .isRead("Y".equals(notification.getIsRead()))
                .relatedId(notification.getRelatedId())
                .refType(notification.getRefType())
                .refId(notification.getRefId())
                .userId(notification.getUser().getId())
                .userName(notification.getUser().getRealName())
                .createdAt(notification.getCreatedAt())
                .feedId(feedId)
                .commentId(commentId)
                .build();
    }
}
