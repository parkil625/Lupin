package com.example.demo.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "medical_staff")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class MedicalStaff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true, length = 50)
    private String userId;  // 로그인용 ID

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(name = "real_name", nullable = false, length = 100)
    private String realName;

    @Column(length = 100)
    private String specialty;  // 전공

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
        return this.realName;
    }
}
