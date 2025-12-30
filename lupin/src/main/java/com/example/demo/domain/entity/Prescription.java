package com.example.demo.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
// List, ArrayList 임포트 제거

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

    // 복잡한 연관관계 테이블 제거 -> 단순 텍스트로 저장
    @Column(columnDefinition = "TEXT")
    private String medications; 

    @Column(columnDefinition = "TEXT")
    private String diagnosis;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    // 생성일, 수정일 등은 필요하다면 BaseEntity 상속 또는 별도 추가

    public void updateDiagnosis(String diagnosis) {
        this.diagnosis = diagnosis;
    }

    public void updateMedications(String medications) {
        this.medications = medications;
    }
}