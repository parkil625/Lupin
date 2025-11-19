package com.example.demo.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "prescription_med")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PrescriptionMed extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "medicine_name", nullable = false, length = 255)
    private String medicineName;

    @Column(length = 100)
    private String dosage; // 복용량

    @Column(length = 255)
    private String frequency; // 복용 빈도

    // 연관관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prescription_id", nullable = false)
    @Setter
    private Prescription prescription;
}
