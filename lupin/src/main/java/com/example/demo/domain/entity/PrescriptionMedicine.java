package com.example.demo.domain.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 처방전-약품 중간 테이블 엔티티
 * prescriptions와 medicines의 다대다 관계를 표현
 */
@Entity
@Table(name = "prescription_medicines")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PrescriptionMedicine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prescription_id", nullable = false)
    private Prescription prescription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medicine_id", nullable = false)
    private Medicine medicine;

    // 양방향 관계 설정을 위한 편의 메서드
    public void setPrescription(Prescription prescription) {
        this.prescription = prescription;
    }
}
