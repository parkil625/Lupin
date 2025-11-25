package com.example.demo.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * 의사 프로필 엔티티
 * User(role=DOCTOR)와 1:1 관계
 * 의사 역할을 하는 직원의 추가 정보만 저장
 */
@Entity
@Table(name = "doctor_profiles", indexes = {
    @Index(name = "idx_doctor_profile_user", columnList = "userId")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class DoctorProfile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", nullable = false, unique = true)
    private User user;

    @Column(length = 100)
    private String specialty;  // 전공 (내과, 외과, 정형외과 등)

    @Column(name = "license_number", length = 50)
    private String licenseNumber;  // 의사 면허번호

    @Column(name = "medical_experience")
    private Integer medicalExperience;  // 경력 (년)

    @Column(length = 20)
    private String phone;  // 연락처

    @Column(name = "birth_date")
    private LocalDate birthDate;  // 생년월일

    @Column(length = 10)
    private String gender;  // 성별

    @Column(length = 500)
    private String address;  // 주소

    // 편의 메서드
    public String getName() {
        return user != null ? user.getRealName() : null;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
