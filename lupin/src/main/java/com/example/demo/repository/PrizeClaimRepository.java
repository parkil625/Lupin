package com.example.demo.repository;

import com.example.demo.domain.entity.PrizeClaim;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PrizeClaimRepository extends JpaRepository<PrizeClaim, Long> {

    // 사용자의 상금 수령 신청 목록
    List<PrizeClaim> findByUserIdOrderByCreatedAtDesc(Long userId);

    // 추첨권 ID로 조회
    Optional<PrizeClaim> findByLotteryTicketId(Long lotteryTicketId);

    // 상태별 조회
    List<PrizeClaim> findByStatus(String status);
}
