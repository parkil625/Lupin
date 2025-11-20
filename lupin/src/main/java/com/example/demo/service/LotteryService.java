package com.example.demo.service;

import com.example.demo.domain.entity.LotteryTicket;
import com.example.demo.domain.entity.PrizeClaim;
import com.example.demo.domain.entity.User;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.LotteryTicketRepository;
import com.example.demo.repository.PrizeClaimRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * 추첨권 관련 비즈니스 로직을 처리하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LotteryService {

    private final LotteryTicketRepository lotteryTicketRepository;
    private final PrizeClaimRepository prizeClaimRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final NotificationService notificationService;
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
     * 상금 수령 신청
     */
    @Transactional
    public PrizeClaim claimPrize(Long ticketId, String bankName, String accountNumber, String accountHolder) {
        LotteryTicket ticket = findTicketById(ticketId);

        // 당첨된 티켓인지 확인
        if (!"Y".equals(ticket.getIsUsed()) || ticket.getWinResult() == null || ticket.getWinResult().contains("낙첨")) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "당첨된 추첨권이 아닙니다.");
        }

        // 이미 수령 신청했는지 확인
        if (prizeClaimRepository.findByLotteryTicketId(ticketId).isPresent()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "이미 상금 수령 신청을 완료했습니다.");
        }

        // 상금액 추출
        String prizeAmount = ticket.getWinResult().contains("100만원") ? "100만원" : "50만원";

        PrizeClaim claim = PrizeClaim.builder()
                .bankName(bankName)
                .accountNumber(accountNumber)
                .accountHolder(accountHolder)
                .prizeAmount(prizeAmount)
                .build();

        claim.setUser(ticket.getUser());
        claim.setLotteryTicket(ticket);

        PrizeClaim savedClaim = prizeClaimRepository.save(claim);

        log.info("상금 수령 신청 완료 - userId: {}, 상금: {}, 은행: {}",
            ticket.getUser().getId(), prizeAmount, bankName);

        return savedClaim;
    }

    /**
     * 사용자의 당첨 내역 조회
     */
    public List<LotteryTicket> getWinningTickets(Long userId) {
        return lotteryTicketRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(ticket -> "Y".equals(ticket.getIsUsed())
                    && ticket.getWinResult() != null
                    && !ticket.getWinResult().contains("낙첨"))
                .collect(Collectors.toList());
    }

    /**
     * 사용자의 상금 수령 신청 내역 조회
     */
    public List<PrizeClaim> getPrizeClaims(Long userId) {
        return prizeClaimRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * ID로 사용자 조회 (내부 메서드)
     */
    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    /**
     * 수동 추첨 실행 (테스트용)
     */
    @Transactional
    public void runManualLottery() {
        log.info("=== 수동 추첨 시작 ===");

        // 모든 미사용 추첨권 조회
        List<LotteryTicket> unusedTickets = lotteryTicketRepository.findByIsUsed("N");

        if (unusedTickets.isEmpty()) {
            log.info("미사용 추첨권이 없습니다.");
            return;
        }

        int totalTickets = unusedTickets.size();
        log.info("총 추첨권: {}장", totalTickets);

        // 추첨권 섞기
        List<LotteryTicket> shuffledTickets = new ArrayList<>(unusedTickets);
        Collections.shuffle(shuffledTickets);

        int firstPlaceCount = 0;
        int secondPlaceCount = 0;

        // 1등 추첨 (1명, 100만원)
        if (shuffledTickets.size() >= 1) {
            LotteryTicket winner = shuffledTickets.get(0);
            winner.use("1등_100만원");
            firstPlaceCount++;

            // 당첨 알림 생성
            notificationService.createSystemNotification(
                winner.getUser().getId(),
                "🎉 축하합니다! 1등 당첨 (100만원)"
            );

            log.info("1등 당첨 - userId: {}, userName: {}",
                winner.getUser().getId(), winner.getUser().getRealName());
        }

        // 2등 추첨 (2명, 50만원)
        for (int i = 1; i < Math.min(3, shuffledTickets.size()); i++) {
            LotteryTicket winner = shuffledTickets.get(i);
            winner.use("2등_50만원");
            secondPlaceCount++;

            // 당첨 알림 생성
            notificationService.createSystemNotification(
                winner.getUser().getId(),
                "🎉 축하합니다! 2등 당첨 (50만원)"
            );

            log.info("2등 당첨 - userId: {}, userName: {}",
                winner.getUser().getId(), winner.getUser().getRealName());
        }

        // 나머지 추첨권은 낙첨 처리
        for (int i = 3; i < shuffledTickets.size(); i++) {
            LotteryTicket loser = shuffledTickets.get(i);
            loser.use("낙첨");

            // 낙첨 알림 생성
            notificationService.createSystemNotification(
                loser.getUser().getId(),
                "😢 아쉽게도 이번 추첨에서 당첨되지 않았습니다. 다음 기회에 도전하세요!"
            );
        }

        log.info("=== 수동 추첨 완료 ===");
        log.info("1등: {}명, 2등: {}명, 낙첨: {}명",
            firstPlaceCount,
            secondPlaceCount,
            Math.max(0, totalTickets - firstPlaceCount - secondPlaceCount));
    }
}
