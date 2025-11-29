package com.example.demo.dto.response;

import com.example.demo.domain.entity.Notification;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class NotificationResponse {

    private Long id;
    private String type;
    private String title;
    private String content;
    private Boolean isRead;
    private String refId;
    private LocalDateTime createdAt;

    public static NotificationResponse from(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType())
                .title(notification.getTitle())
                .content(notification.getContent())
                .isRead(notification.getIsRead())
                .refId(notification.getRefId())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
