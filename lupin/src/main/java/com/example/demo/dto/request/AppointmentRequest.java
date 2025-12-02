package com.example.demo.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentRequest {

    @NotNull(message = "환자 ID는 필수입니다.")
    private Long patientId;

    @NotNull(message = "의사 ID는 필수입니다.")
    private Long doctorId;

    @NotNull(message = "예약 날짜는 필수입니다.")
    @Future(message = "예약 날짜는 현재 시간보다 빨라야 합니다.")
    private LocalDateTime date;
}
