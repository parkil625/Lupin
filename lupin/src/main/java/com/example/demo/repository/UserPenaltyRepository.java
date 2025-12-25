package com.example.demo.repository;

import com.example.demo.domain.entity.UserPenalty;
import com.example.demo.domain.enums.PenaltyType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query; // import 추가
import org.springframework.data.repository.query.Param; // import 추가
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface UserPenaltyRepository extends JpaRepository<UserPenalty, Long> {

    // [수정] @Query를 사용하여 DB에서 직접 카운트를 세도록 변경 (가장 확실한 방법)
    @Query("SELECT COUNT(p) > 0 FROM UserPenalty p WHERE p.user.id = :userId AND p.penaltyType = :penaltyType AND p.createdAt > :since")
    boolean existsByUserIdAndPenaltyTypeAndCreatedAtAfter(
            @Param("userId") Long userId, 
            @Param("penaltyType") PenaltyType penaltyType, 
            @Param("since") LocalDateTime since);
}