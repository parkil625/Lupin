package com.example.demo.repository;

import com.example.demo.domain.entity.UserPenalty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface UserPenaltyRepository extends JpaRepository<UserPenalty, Long> {

    // 사용자의 특정 타입 패널티 조회
    Optional<UserPenalty> findByUserIdAndPenaltyType(Long userId, String penaltyType);

    // 3일 내 패널티가 있는지 확인
    @Query("SELECT COUNT(p) > 0 FROM UserPenalty p WHERE p.user.id = :userId AND p.penaltyType = :penaltyType AND p.createdAt > :since")
    boolean hasActivePenalty(@Param("userId") Long userId, @Param("penaltyType") String penaltyType, @Param("since") LocalDateTime since);
}
