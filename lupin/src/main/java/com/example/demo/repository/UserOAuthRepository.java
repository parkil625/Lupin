package com.example.demo.repository;

import com.example.demo.domain.entity.UserOAuth;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserOAuthRepository extends JpaRepository<UserOAuth, Long> {

    // provider와 providerId로 조회 (OAuth 로그인 시 사용)
    Optional<UserOAuth> findByProviderAndProviderId(String provider, String providerId);

    // 특정 사용자의 모든 연동 계정 조회
    List<UserOAuth> findByUserId(Long userId);

    // 특정 사용자의 특정 provider 연동 조회
    Optional<UserOAuth> findByUserIdAndProvider(Long userId, String provider);

    // 특정 사용자가 특정 provider를 연동했는지 확인
    boolean existsByUserIdAndProvider(Long userId, String provider);

    // 연동 해제
    void deleteByUserIdAndProvider(Long userId, String provider);
}
