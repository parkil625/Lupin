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

    // [수정] 스프링 데이터 JPA의 쿼리 메소드 기능을 사용하여 자동 생성 (가장 안전한 방법)
    boolean existsByUserIdAndPenaltyTypeAndCreatedAtAfter(
            @Param("userId") Long userId, 
            @Param("penaltyType") PenaltyType penaltyType, 
            @Param("since") LocalDateTime since);
}