package com.example.demo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 채팅 메시지 생성 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageCreateRequest {

    @NotBlank(message = "메시지 내용은 필수입니다.")
    private String content;

    @NotNull(message = "발신자 ID는 필수입니다.")
    private Long senderId;

    @NotNull(message = "환자 ID는 필수입니다.")
    private Long patientId;

    @NotNull(message = "의사 ID는 필수입니다.")
    private Long doctorId;
}
