package com.example.demo.repository;

import com.example.demo.domain.entity.User;
import com.example.demo.domain.entity.UserPenalty;
import com.example.demo.domain.enums.PenaltyType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface UserPenaltyRepository extends JpaRepository<UserPenalty, Long> {

    // [수정] 객체 대신 ID로 조회하여 정확도 향상
    boolean existsByUserIdAndPenaltyTypeAndCreatedAtAfter(Long userId, PenaltyType penaltyType, LocalDateTime since);
}
