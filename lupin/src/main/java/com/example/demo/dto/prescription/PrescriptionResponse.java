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
        // Null safety: prescription의 연관 엔티티가 null인 경우 기본값 처리
        Long patientId = null;
        String patientName = "알 수 없음";
        Long doctorId = null;
        String doctorName = "알 수 없음";

        if (prescription.getPatient() != null) {
            patientId = prescription.getPatient().getId();
            patientName = prescription.getPatient().getName();
        }

        if (prescription.getDoctor() != null) {
            doctorId = prescription.getDoctor().getId();
            doctorName = prescription.getDoctor().getName();
        }

        String departmentName = null;
        if (prescription.getAppointment() != null) {
            departmentName = prescription.getAppointment().getDepartmentName();
        }

        return PrescriptionResponse.builder()
                .id(prescription.getId())
                .patientId(patientId)
                .patientName(patientName)
                .doctorId(doctorId)
                .doctorName(doctorName)
                .departmentName(departmentName)
                .appointmentId(prescription.getAppointment() != null ?
                        prescription.getAppointment().getId() : null)
                .diagnosis(prescription.getDiagnosis())
                .date(prescription.getDate())
                .medications(prescription.getMedications())
                .build();
    }
}
