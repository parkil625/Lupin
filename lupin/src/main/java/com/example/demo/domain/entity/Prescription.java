package com.example.demo.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "prescriptions", indexes = {
    @Index(name = "idx_prescription_patient", columnList = "patient_id"),
    @Index(name = "idx_prescription_doctor", columnList = "doctor_id"),
    @Index(name = "idx_prescription_date", columnList = "date DESC")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Prescription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private User patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private User doctor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id")
    private Appointment appointment;

    @Column(columnDefinition = "TEXT")
    private String diagnosis;

    @Column(columnDefinition = "TEXT")
    private String instructions;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    // 처방 약품 목록 (prescription_medicines 테이블과 조인)
    @OneToMany(mappedBy = "prescription", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PrescriptionMedicine> medicines = new ArrayList<>();

    // 생성일, 수정일 등은 필요하다면 BaseEntity 상속 또는 별도 추가

    public void updateDiagnosis(String diagnosis) {
        this.diagnosis = diagnosis;
    }

    public void updateInstructions(String instructions) {
        this.instructions = instructions;
    }

    // 약품 추가 편의 메서드
    public void addMedicine(PrescriptionMedicine medicine) {
        medicines.add(medicine);
        medicine.setPrescription(this);
    }

    // 약품 제거 편의 메서드
    public void removeMedicine(PrescriptionMedicine medicine) {
        medicines.remove(medicine);
        medicine.setPrescription(null);
    }

    // 모든 약품 제거
    public void clearMedicines() {
        medicines.clear();
    }
}