package com.example.demo.dto.prescription;

import com.example.demo.domain.entity.Prescription;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescriptionResponse {

    private Long id;
    private Long patientId;
    private String patientName;
    private Long doctorId;
    private String doctorName;
    private String departmentName;
    private Long appointmentId;
    private String diagnosis;
    private LocalDate date;

    private String medications;

    public static PrescriptionResponse from(Prescription prescription) {
        // Null safety: prescription의 연관 엔티티가 null인 경우 처리
        if (prescription.getPatient() == null || prescription.getDoctor() == null) {
            throw new IllegalStateException("처방전에 환자 또는 의사 정보가 없습니다.");
        }

        String departmentName = null;
        if (prescription.getAppointment() != null) {
            departmentName = prescription.getAppointment().getDepartmentName();
        }

        return PrescriptionResponse.builder()
                .id(prescription.getId())
                .patientId(prescription.getPatient().getId())
                .patientName(prescription.getPatient().getName())
                .doctorId(prescription.getDoctor().getId())
                .doctorName(prescription.getDoctor().getName())
                .departmentName(departmentName)
                .appointmentId(prescription.getAppointment() != null ?
                        prescription.getAppointment().getId() : null)
                .diagnosis(prescription.getDiagnosis())
                .date(prescription.getDate())
                .medications(prescription.getMedications())
                .build();
    }
}
