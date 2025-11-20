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
public class User extends BaseEntity {

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
    private Long currentPoints = 0L;

    @Column(name = "total_points")
    @Builder.Default
    private Long totalPoints = 0L;

    @Column(length = 100)
    private String department;

    // 연관관계
    @OneToMany(mappedBy = "writer", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Feed> feeds = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Notification> notifications = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PointLog> pointLogs = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<LotteryTicket> lotteryTickets = new ArrayList<>();

    // 비즈니스 로직
    public void addPoints(Long amount) {
        this.currentPoints += amount;
        this.totalPoints += amount;
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
        // totalPoints도 차감 (추첨권 회수를 위해)
        this.totalPoints = Math.max(0, this.totalPoints - amount);
    }

    public void setCurrentPoints(Long currentPoints) {
        this.currentPoints = currentPoints;
    }

    public void setTotalPoints(Long totalPoints) {
        this.totalPoints = totalPoints;
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
