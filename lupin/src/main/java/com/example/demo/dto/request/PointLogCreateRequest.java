package com.example.demo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 포인트 로그 생성 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PointLogCreateRequest {

    @NotNull(message = "포인트 금액은 필수입니다.")
    private Long amount;

    @NotBlank(message = "사유는 필수입니다.")
    private String reason;

    private String refId;

    @NotNull(message = "사용자 ID는 필수입니다.")
    private Long userId;
}
