package com.example.demo.service;

import com.example.demo.domain.entity.LotteryTicket;
import com.example.demo.domain.entity.User;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.LotteryTicketRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 추첨권 관련 비즈니스 로직을 처리하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LotteryService {

    private final LotteryTicketRepository lotteryTicketRepository;
    private final UserRepository userRepository;

    /**
     * 추첨권 발급
     */
    @Transactional
    public LotteryTicket issueTicket(Long userId) {
        User user = findUserById(userId);

        LotteryTicket ticket = LotteryTicket.builder()
                .user(user)
                .build();

        LotteryTicket savedTicket = lotteryTicketRepository.save(ticket);

        log.info("추첨권 발급 완료 - ticketId: {}, userId: {}", savedTicket.getId(), userId);

        return savedTicket;
    }

    /**
     * 사용자의 추첨권 조회
     */
    public List<LotteryTicket> getTickets(Long userId) {
        return lotteryTicketRepository.findByUserId(userId);
    }

    /**
     * 사용자의 추첨권 개수 조회
     */
    public Long countTickets(Long userId) {
        return lotteryTicketRepository.countByUserId(userId);
    }

    /**
     * ID로 사용자 조회 (내부 메서드)
     */
    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
