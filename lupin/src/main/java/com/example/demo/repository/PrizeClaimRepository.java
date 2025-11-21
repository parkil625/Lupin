package com.example.demo.repository;

import com.example.demo.domain.entity.PrizeClaim;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PrizeClaimRepository extends JpaRepository<PrizeClaim, Long> {

    // 사용자의 당첨 조회
    List<PrizeClaim> findByUserId(Long userId);

    // 만료된 당첨 조회 (7일 이상)
    @Query("SELECT p FROM PrizeClaim p WHERE p.createdAt < :expireDate")
    List<PrizeClaim> findExpired(@Param("expireDate") LocalDateTime expireDate);
}
