package com.example.demo.dto.response;

import com.example.demo.domain.entity.Prescription;
import com.example.demo.domain.entity.PrescriptionMed;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 처방전 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescriptionResponse {

    private Long id;
    private String prescriptionName;
    private String diagnosis;
    private String instructions;
    private LocalDate prescribedDate;
    private Long patientId;
    private String patientName;
    private Long doctorId;
    private String doctorName;
    private List<MedicineDto> medicines;
    private LocalDateTime createdAt;

    /**
     * Entity -> Response DTO 변환
     */
    public static PrescriptionResponse from(Prescription prescription) {
        return PrescriptionResponse.builder()
                .id(prescription.getId())
                .prescriptionName(prescription.getPrescriptionName())
                .diagnosis(prescription.getDiagnosis())
                .instructions(prescription.getInstructions())
                .prescribedDate(prescription.getPrescribedDate())
                .patientId(prescription.getPatient().getId())
                .patientName(prescription.getPatient().getName())
                .doctorId(prescription.getDoctor().getId())
                .doctorName(prescription.getDoctor().getName())
                .medicines(prescription.getMedicines().stream()
                        .map(MedicineDto::from)
                        .collect(Collectors.toList()))
                .createdAt(prescription.getCreatedAt())
                .build();
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MedicineDto {
        private Long id;
        private String medicineName;
        private String dosage;
        private String frequency;

        public static MedicineDto from(PrescriptionMed prescriptionMed) {
            return MedicineDto.builder()
                    .id(prescriptionMed.getId())
                    .medicineName(prescriptionMed.getMedicineName())
                    .dosage(prescriptionMed.getDosage())
                    .frequency(prescriptionMed.getFrequency())
                    .build();
        }
    }
}
