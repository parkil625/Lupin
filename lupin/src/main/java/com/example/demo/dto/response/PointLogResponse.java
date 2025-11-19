package com.example.demo.dto.response;

import com.example.demo.domain.entity.PointLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 포인트 로그 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PointLogResponse {

    private Long id;
    private Long amount;
    private String reason;
    private String refId;
    private Long userId;
    private String userName;
    private LocalDateTime createdAt;

    /**
     * Entity -> Response DTO 변환
     */
    public static PointLogResponse from(PointLog pointLog) {
        return PointLogResponse.builder()
                .id(pointLog.getId())
                .amount(pointLog.getAmount())
                .reason(pointLog.getReason())
                .refId(pointLog.getRefId())
                .userId(pointLog.getUser().getId())
                .userName(pointLog.getUser().getName())
                .createdAt(pointLog.getCreatedAt())
                .build();
    }
}
