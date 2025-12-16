package com.example.demo.domain.entity;

import com.example.demo.domain.enums.Role;
import com.example.demo.domain.enums.SocialProvider;
import com.example.demo.domain.enums.UserStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true, length = 50)
    private String userId;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Role role;

    // 유저 상태 - 로그인 시 즉시 확인 가능
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    @Column
    private Double height;

    @Column
    private Double weight;

    @Column(length = 10)
    private String gender;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(length = 100)
    private String department;

    @Column(length = 500)
    private String avatar;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private SocialProvider provider;

    @Column(name = "provider_id", length = 255)
    private String providerId;

    @Column(name = "provider_email", length = 255)
    private String providerEmail;

    @Column(name = "current_points", nullable = false)
    @Builder.Default
    private Long totalPoints = 0L;

    @Version
    private Long version;

    public void updateProfile(String name, Double height, Double weight, LocalDate birthDate, String gender) {
        if (name != null) this.name = name;
        if (height != null) this.height = height;
        if (weight != null) this.weight = weight;
        if (birthDate != null) this.birthDate = birthDate;
        if (gender != null) this.gender = gender;
    }

    // 상태 변경 메서드
    public void ban() {
        this.status = UserStatus.BANNED;
    }

    public void activate() {
        this.status = UserStatus.ACTIVE;
    }

    public void withdraw() {
        this.status = UserStatus.WITHDRAWN;
    }

    public boolean isActive() {
        return this.status == UserStatus.ACTIVE;
    }

    // OAuth 연동 메서드
    public void linkOAuth(SocialProvider provider, String providerId, String providerEmail) {
        this.provider = provider;
        this.providerId = providerId;
        this.providerEmail = providerEmail;
    }

    // OAuth 연동 해제 메서드
    public void unlinkOAuth() {
        this.provider = null;
        this.providerId = null;
        this.providerEmail = null;
    }

    // 아바타 업데이트 메서드
    public void updateAvatar(String avatarUrl) {
        this.avatar = avatarUrl;
    }

    // 포인트 추가
    public void addPoints(long amount) {
        this.totalPoints += amount;
    }

    // 포인트 차감
    public void deductPoints(long amount) {
        this.totalPoints -= amount;
    }

    // 의사 진료과 변경용
    public void assignDepartment(String department) {
        this.department = department;
    }
}
