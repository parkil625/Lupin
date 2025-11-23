package com.example.demo.domain.entity;

import com.example.demo.domain.enums.Role;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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
    private String userId;  // 로그인용 ID (user01, user02 등)

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

    @Column(name = "current_points")
    @Builder.Default
    private Long currentPoints = 0L;  // 추첨권 계산용 잔여 포인트 (0~29)

    @Column(name = "monthly_points")
    @Builder.Default
    private Long monthlyPoints = 0L;  // 이번 달 누적 포인트 (랭킹용, 매월 초기화)

    @Column(name = "monthly_likes")
    @Builder.Default
    private Long monthlyLikes = 0L;  // 이번 달 받은 좋아요 수 (랭킹용, 매월 초기화)

    @Column(length = 100)
    private String department;

    @Column(length = 20)
    private String phone;  // 연락처

    // 연관관계
    @OneToMany(mappedBy = "writer", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Feed> feeds = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Notification> notifications = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<LotteryTicket> lotteryTickets = new ArrayList<>();

    // 비즈니스 로직
    public void addPoints(Long amount) {
        this.currentPoints += amount;
        this.monthlyPoints += amount;
    }

    /**
     * 추첨권 발급 후 currentPoints 차감
     */
    public void deductCurrentPointsForTicket() {
        this.currentPoints -= 30;
    }

    public void usePoints(Long amount) {
        if (this.currentPoints < amount) {
            throw new IllegalStateException("포인트가 부족합니다.");
        }
        this.currentPoints -= amount;
    }

    public void revokePoints(Long amount) {
        // currentPoints 차감 (0 미만으로 내려가지 않음)
        this.currentPoints = Math.max(0, this.currentPoints - amount);
        // monthlyPoints도 차감
        this.monthlyPoints = Math.max(0, this.monthlyPoints - amount);
    }

    public void setCurrentPoints(Long currentPoints) {
        this.currentPoints = currentPoints;
    }

    public void setMonthlyPoints(Long monthlyPoints) {
        this.monthlyPoints = monthlyPoints;
    }

    public void setMonthlyLikes(Long monthlyLikes) {
        this.monthlyLikes = monthlyLikes;
    }

    /**
     * 월초 리셋
     */
    public void resetMonthlyData() {
        this.monthlyPoints = 0L;
        this.currentPoints = 0L;
        this.monthlyLikes = 0L;
    }

    /**
     * 월별 좋아요 증가
     */
    public void incrementMonthlyLikes() {
        this.monthlyLikes++;
    }

    /**
     * 월별 좋아요 감소
     */
    public void decrementMonthlyLikes() {
        if (this.monthlyLikes > 0) {
            this.monthlyLikes--;
        }
    }

    // 편의 메서드
    public String getName() {
        return this.realName; // 실제 이름 반환
    }

    public String getProfileImage() {
        // 추후 구현 - 현재는 null 반환
        return null;
    }
}
