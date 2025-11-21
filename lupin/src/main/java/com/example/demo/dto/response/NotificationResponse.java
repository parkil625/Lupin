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
    private String refId;    // 피드 ID
    private Long userId;
    private String userName;
    private LocalDateTime createdAt;
    private Long feedId;     // 피드 ID (refId에서 파싱)

    /**
     * Entity -> Response DTO 변환
     */
    public static NotificationResponse from(Notification notification) {
        Long feedId = null;

        // refId에서 feedId 추출
        if (notification.getRefId() != null) {
            try {
                feedId = Long.parseLong(notification.getRefId());
            } catch (NumberFormatException e) {
                // 무시
            }
        }

        return NotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType())
                .title(notification.getTitle())
                .content(notification.getContent())
                .isRead("Y".equals(notification.getIsRead()))
                .refId(notification.getRefId())
                .userId(notification.getUser().getId())
                .userName(notification.getUser().getRealName())
                .createdAt(notification.getCreatedAt())
                .feedId(feedId)
                .build();
    }
}
