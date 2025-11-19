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
import java.util.Random;

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
    private final UserService userService;
    private final Random random = new Random();

    // 추첨권 구매 가격
    private static final Long TICKET_PRICE = 100L;

    // 당첨 확률 (10%)
    private static final double WIN_PROBABILITY = 0.1;

    // 당첨 시 보상 포인트
    private static final Long WIN_REWARD = 1000L;

    /**
     * 추첨권 구매
     */
    @Transactional
    public LotteryTicket purchaseTicket(Long userId) {
        User user = findUserById(userId);

        // 포인트 차감
        userService.deductPoints(userId, TICKET_PRICE, "추첨권 구매");

        // 추첨권 생성
        LotteryTicket ticket = LotteryTicket.builder()
                .user(user)
                .isUsed("N")
                .build();

        LotteryTicket savedTicket = lotteryTicketRepository.save(ticket);

        log.info("추첨권 구매 완료 - ticketId: {}, userId: {}", savedTicket.getId(), userId);

        return savedTicket;
    }

    /**
     * 추첨권 사용 (추첨 진행)
     */
    @Transactional
    public String useLotteryTicket(Long ticketId) {
        LotteryTicket ticket = findTicketById(ticketId);

        // 추첨 진행
        boolean isWin = random.nextDouble() < WIN_PROBABILITY;
        String result = isWin ? "WIN" : "LOSE";

        ticket.use(result);

        // 당첨 시 보상 지급
        if (isWin) {
            userService.addPoints(
                    ticket.getUser().getId(),
                    WIN_REWARD,
                    "추첨 당첨",
                    String.valueOf(ticketId)
            );
            log.info("추첨 당첨! - ticketId: {}, userId: {}, reward: {}",
                    ticketId, ticket.getUser().getId(), WIN_REWARD);
        } else {
            log.info("추첨 낙첨 - ticketId: {}, userId: {}", ticketId, ticket.getUser().getId());
        }

        return result;
    }

    /**
     * 사용자의 미사용 추첨권 조회
     */
    public List<LotteryTicket> getUnusedTickets(Long userId) {
        return lotteryTicketRepository.findByUserIdAndIsUsed(userId, "N");
    }

    /**
     * 사용자의 미사용 추첨권 개수 조회
     */
    public Long countUnusedTickets(Long userId) {
        return lotteryTicketRepository.countByUserIdAndIsUsed(userId, "N");
    }

    /**
     * 사용자의 모든 추첨권 조회
     */
    public List<LotteryTicket> getAllTickets(Long userId) {
        return lotteryTicketRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * ID로 추첨권 조회 (내부 메서드)
     */
    private LotteryTicket findTicketById(Long ticketId) {
        return lotteryTicketRepository.findById(ticketId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LOTTERY_TICKET_NOT_FOUND));
    }

    /**
     * ID로 사용자 조회 (내부 메서드)
     */
    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
