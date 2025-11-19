package com.example.demo.repository;

import com.example.demo.domain.entity.LotteryTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LotteryTicketRepository extends JpaRepository<LotteryTicket, Long> {

    // 사용자의 미사용 추첨권 조회
    List<LotteryTicket> findByUserIdAndIsUsed(Long userId, String isUsed);

    // 사용자의 미사용 추첨권 개수
    Long countByUserIdAndIsUsed(Long userId, String isUsed);

    // 사용자의 모든 추첨권
    List<LotteryTicket> findByUserIdOrderByCreatedAtDesc(Long userId);
}
