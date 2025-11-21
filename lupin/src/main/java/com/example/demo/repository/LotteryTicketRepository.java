package com.example.demo.repository;

import com.example.demo.domain.entity.LotteryTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LotteryTicketRepository extends JpaRepository<LotteryTicket, Long> {

    // 사용자의 추첨권 조회
    List<LotteryTicket> findByUserId(Long userId);

    // 사용자의 추첨권 개수
    Long countByUserId(Long userId);
}
