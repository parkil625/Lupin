package com.example.demo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * 처방전 생성 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescriptionCreateRequest {

    private String prescriptionName;

    @NotBlank(message = "진단 내용은 필수입니다.")
    private String diagnosis;

    private String instructions;

    @NotNull(message = "처방 날짜는 필수입니다.")
    private LocalDate prescribedDate;

    @NotNull(message = "환자 ID는 필수입니다.")
    private Long patientId;

    @NotNull(message = "의사 ID는 필수입니다.")
    private Long doctorId;

    // 약품 목록
    private List<MedicineDto> medicines;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MedicineDto {
        @NotBlank(message = "약품명은 필수입니다.")
        private String medicineName;
        private String dosage;
        private String frequency;
    }
}
