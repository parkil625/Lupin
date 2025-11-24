package com.example.demo.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "prescription_meds", indexes = {
    @Index(name = "idx_prescription_med_prescription", columnList = "prescriptionId")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PrescriptionMed {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, insertable = false, updatable = false)
    private Long prescriptionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prescriptionId", nullable = false)
    @Setter
    private Prescription prescription;

    @Column(name = "medicine_name", nullable = false, length = 255)
    private String medicineName;

    @Column(length = 100)
    private String dosage;

    @Column(length = 255)
    private String frequency;
}
