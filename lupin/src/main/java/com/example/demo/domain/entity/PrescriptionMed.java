package com.example.demo.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "prescription_meds", indexes = {
    @Index(name = "idx_prescription_med_prescription", columnList = "prescription_id"),
    @Index(name = "idx_prescription_med_medicine", columnList = "medicine_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PrescriptionMed {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prescription_id", nullable = false)
    @Setter
    private Prescription prescription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medicine_id")
    private Medicine medicine; // 약품 정보 참조 (NULL 가능 - 임의 입력도 허용)

    @Column(name = "medicine_name", nullable = false, length = 255)
    private String medicineName; // 약품명 (직접 입력 또는 Medicine에서 복사)

    @Column(length = 100)
    private String dosage; // 용량 (예: 1정)

    @Column(length = 255)
    private String frequency; // 복용 빈도 (예: 1일 3회, 식후)

    @Column(name = "duration_days")
    private Integer durationDays; // 복용 기간 (일)

    @Column(columnDefinition = "TEXT")
    private String instructions; // 추가 복용 지침
}
