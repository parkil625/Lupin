package com.example.demo.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "doctor_profiles")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class DoctorProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(length = 100)
    private String specialty; // 전공 (내과, 외과, 정형외과 등)

    @Column(name = "license_number", length = 50)
    private String licenseNumber; // 의사 면허번호

    @Column(name = "medical_experience")
    private Integer medicalExperience; // 경력 (년)

    @Column(length = 20)
    private String phone; // 연락처

    @Column(name = "birth_date")
    private LocalDate birthDate; // 생년월일

    @Column(length = 10)
    private String gender; // 성별

    @Column(length = 500)
    private String address; // 주소

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
