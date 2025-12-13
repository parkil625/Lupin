package com.example.demo.dto;

import com.example.demo.dto.response.NotificationResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Redis Pub/Sub를 통해 전달되는 알림 메시지
 * - userId: 알림을 받을 사용자 ID
 * - notification: 실제 알림 데이터
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long userId;
    private NotificationResponse notification;
}
