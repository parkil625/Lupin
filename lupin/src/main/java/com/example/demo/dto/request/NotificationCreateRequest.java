package com.example.demo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 알림 생성 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationCreateRequest {

    @NotBlank(message = "알림 타입은 필수입니다.")
    private String type; // challenge, appointment, like, comment

    @NotBlank(message = "알림 제목은 필수입니다.")
    private String title;

    private String content;

    @NotNull(message = "수신자 ID는 필수입니다.")
    private Long userId;

    // 관련 엔티티 ID (선택)
    private Long relatedId;

    // 참조 타입 (FEED, COMMENT, CHAT 등)
    private String refType;

    // 참조 ID (String)
    private String refId;
}
