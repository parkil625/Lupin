package com.example.demo.dto.prescription;

import com.example.demo.domain.entity.Prescription;
import com.example.demo.domain.entity.PrescriptionMed;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
    private List<MedicineItem> medicines;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MedicineItem {
        private Long id;
        private Long medicineId;
        private String medicineName;
        private String dosage;
        private String frequency;
        private Integer durationDays;
        private String instructions;
    }

    public static PrescriptionResponse from(Prescription prescription) {
        String departmentName = null;
        if (prescription.getAppointment() != null) {
            departmentName = prescription.getAppointment().getDepartmentName();
        }

        List<MedicineItem> medicineItems = new ArrayList<>();
        if (prescription.getMedicines() != null && !prescription.getMedicines().isEmpty()) {
            medicineItems = prescription.getMedicines().stream()
                    .map(med -> MedicineItem.builder()
                            .id(med.getId())
                            .medicineId(med.getMedicine() != null ? med.getMedicine().getId() : null)
                            .medicineName(med.getMedicineName())
                            .dosage(med.getDosage())
                            .frequency(med.getFrequency())
                            .durationDays(med.getDurationDays())
                            .instructions(med.getInstructions())
                            .build())
                    .collect(Collectors.toList());
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
                .medicines(medicineItems)
                .build();
    }
}
