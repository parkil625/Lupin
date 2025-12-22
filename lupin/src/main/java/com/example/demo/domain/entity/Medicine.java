package com.example.demo.domain.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 약품 정보 엔티티
 * 약품 데이터베이스를 관리하며, 처방 시 참조됩니다.
 */
@Entity
@Table(name = "medicines", indexes = {
    @Index(name = "idx_medicine_name", columnList = "name"),
    @Index(name = "idx_medicine_code", columnList = "code", unique = true)
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Medicine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code; // 약품 코드 (예: A01234567)

    @Column(name = "name", nullable = false, length = 255)
    private String name; // 약품명

    @Column(name = "manufacturer", length = 255)
    private String manufacturer; // 제조사

    @Column(name = "standard_dosage", length = 100)
    private String standardDosage; // 표준 용량 (예: 500mg)

    @Column(name = "unit", length = 50)
    private String unit; // 단위 (예: 정, 캡슐, ml)

    @Column(columnDefinition = "TEXT")
    private String description; // 약품 설명

    @Column(columnDefinition = "TEXT")
    private String sideEffects; // 부작용

    @Column(columnDefinition = "TEXT")
    private String precautions; // 주의사항

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true; // 사용 가능 여부
}
