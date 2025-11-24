package com.example.demo.domain.entity;

import com.example.demo.domain.enums.Role;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_user_email", columnList = "email"),
    @Index(name = "idx_user_monthly_points", columnList = "monthlyPoints DESC"),
    @Index(name = "idx_user_monthly_likes", columnList = "monthlyLikes DESC")
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true, length = 50)
    private String userId;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(name = "real_name", nullable = false, length = 100)
    private String realName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Role role;

    @Column
    private Double height;

    @Column
    private Double weight;

    @Column(length = 10)
    private String gender;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "current_points", nullable = false)
    @Builder.Default
    private Long currentPoints = 0L;

    @Column(name = "monthly_points", nullable = false)
    @Builder.Default
    private Long monthlyPoints = 0L;

    @Column(name = "monthly_likes", nullable = false)
    @Builder.Default
    private Long monthlyLikes = 0L;

    @Column(length = 100)
    private String department;

    @Column(length = 20)
    private String phone;

    @Column(name = "profile_image", length = 500)
    private String profileImage;

    // 동시성 제어 - 포인트 경쟁 상태 방지
    @Version
    private Long version;

    // 비즈니스 로직
    public void addPoints(Long amount) {
        this.currentPoints += amount;
        this.monthlyPoints += amount;
    }

    public void deductCurrentPointsForTicket() {
        if (this.currentPoints < 30) {
            throw new IllegalStateException("추첨권 발급에 필요한 포인트가 부족합니다.");
        }
        this.currentPoints -= 30;
    }

    public void revokePoints(Long amount) {
        this.currentPoints = Math.max(0, this.currentPoints - amount);
        this.monthlyPoints = Math.max(0, this.monthlyPoints - amount);
    }

    public void usePoints(Long amount) {
        if (this.currentPoints < amount) {
            throw new IllegalStateException("포인트가 부족합니다.");
        }
        this.currentPoints -= amount;
    }

    public void resetMonthlyData() {
        this.monthlyPoints = 0L;
        this.currentPoints = 0L;
        this.monthlyLikes = 0L;
    }

    public void incrementMonthlyLikes() {
        this.monthlyLikes++;
    }

    public void decrementMonthlyLikes() {
        if (this.monthlyLikes > 0) {
            this.monthlyLikes--;
        }
    }

    public String getName() {
        return this.realName;
    }
}
