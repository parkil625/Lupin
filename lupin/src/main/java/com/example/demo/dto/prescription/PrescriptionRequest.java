package com.example.demo.dto.prescription;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescriptionRequest {

    @NotNull(message = "예약 ID는 필수입니다.")
    private Long appointmentId;

    @NotNull(message = "환자 ID는 필수입니다.")
    private Long patientId;

    @NotBlank(message = "진단명은 필수입니다.")
    private String diagnosis;

    @NotEmpty(message = "최소 하나 이상의 약품을 처방해야 합니다.")
    @Valid
    private List<MedicineItem> medicines;

    private String additionalInstructions; // 추가 지침사항

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MedicineItem {

        private Long medicineId; // Medicine DB 참조 (NULL 가능 - 직접 입력 시)

        @NotBlank(message = "약품명은 필수입니다.")
        private String medicineName;

        private String dosage;

        private String frequency;

        private Integer durationDays; // 복용 기간

        private String instructions; // 복용 지침
    }
}
