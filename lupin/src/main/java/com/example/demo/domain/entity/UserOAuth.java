package com.example.demo.domain.entity;

import com.example.demo.domain.enums.OAuthProvider;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_oauth", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"provider", "provider_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserOAuth extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OAuthProvider provider;  // GOOGLE, NAVER, KAKAO

    @Column(name = "provider_id", nullable = false, length = 255)
    private String providerId;  // OAuth 제공자의 고유 ID

    @Column(name = "provider_email", length = 255)
    private String providerEmail;  // 연동된 계정의 이메일 (UI 표시용)

    // 연동 해제를 위한 메서드
    public void updateProviderEmail(String email) {
        this.providerEmail = email;
    }
}
