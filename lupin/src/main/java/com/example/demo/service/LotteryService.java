package com.example.demo.service;

import com.example.demo.domain.entity.LotteryTicket;
import com.example.demo.domain.entity.User;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.LotteryTicketRepository;
import com.example.demo.repository.UserRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
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
    private final DistributedLockService lockService;
    private final RedisLuaService redisLuaService;

    private static final long TICKET_PRICE = 30L;

    /**
     * 추첨권 발급 (분산 락 + Lua Script 원자적 처리)
     * 30 포인트 차감 후 추첨권 발급
     */
    @Transactional
    @CircuitBreaker(name = "redis", fallbackMethod = "issueTicketFallback")
    public LotteryTicket issueTicket(Long userId) {
        return lockService.withTicketIssueLock(userId, () -> {
            User user = findUserById(userId);

            // Redis Lua Script로 원자적 포인트 차감 + 티켓 발급
            boolean issued = redisLuaService.issueTicketAtomic(userId);
            if (!issued) {
                throw new BusinessException(ErrorCode.INSUFFICIENT_POINTS, "포인트가 부족합니다.");
            }

            // DB에도 저장 (영속성 보장)
            LotteryTicket ticket = LotteryTicket.builder()
                    .user(user)
                    .build();

            LotteryTicket savedTicket = lotteryTicketRepository.save(ticket);

            // 사용자 포인트 DB 반영
            user.setCurrentPoints(user.getCurrentPoints() - TICKET_PRICE);
            userRepository.save(user);

            log.info("추첨권 발급 완료 - ticketId: {}, userId: {}", savedTicket.getId(), userId);

            return savedTicket;
        });
    }

    /**
     * Redis 장애 시 폴백 (DB만 사용)
     */
    public LotteryTicket issueTicketFallback(Long userId, Throwable t) {
        log.warn("Redis 장애, DB 폴백 처리 - userId: {}, error: {}", userId, t.getMessage());

        User user = findUserById(userId);

        if (user.getCurrentPoints() < TICKET_PRICE) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_POINTS, "포인트가 부족합니다.");
        }

        LotteryTicket ticket = LotteryTicket.builder()
                .user(user)
                .build();

        LotteryTicket savedTicket = lotteryTicketRepository.save(ticket);

        user.setCurrentPoints(user.getCurrentPoints() - TICKET_PRICE);
        userRepository.save(user);

        log.info("추첨권 발급 완료 (폴백) - ticketId: {}, userId: {}", savedTicket.getId(), userId);

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
