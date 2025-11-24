package com.example.demo.service;

import com.example.demo.domain.entity.Draw;
import com.example.demo.domain.entity.DrawPrize;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.DrawResult;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.DrawPrizeRepository;
import com.example.demo.repository.DrawRepository;
import com.example.demo.repository.UserRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Random;

/**
 * 추첨 서비스 (분산 락 + Lua Script 원자적 처리)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DrawService {

    private final DrawRepository drawRepository;
    private final DrawPrizeRepository drawPrizeRepository;
    private final UserRepository userRepository;
    private final DistributedLockService lockService;
    private final RedisLuaService redisLuaService;

    private final Random random = new Random();

    /**
     * 추첨 실행 (분산 락 + Lua Script 원자적 재고 차감)
     */
    @Transactional
    @CircuitBreaker(name = "redis", fallbackMethod = "executeDrawFallback")
    public Draw executeDraw(Long userId, Long challengeId) {
        return lockService.withDrawLock(challengeId, () -> {
            User user = findUserById(userId);

            // 이미 추첨했는지 확인
            if (drawRepository.findByUserIdAndChallengeId(userId, challengeId).isPresent()) {
                throw new BusinessException(ErrorCode.ALREADY_DRAWN);
            }

            // 가용 상품 조회
            List<DrawPrize> availablePrizes = drawPrizeRepository.findAvailablePrizes();

            if (availablePrizes.isEmpty()) {
                // 모든 상품 소진 - 꽝 처리
                Draw draw = Draw.builder()
                        .userId(userId)
                        .challengeId(challengeId)
                        .prizeId(null)
                        .result(DrawResult.LOSE)
                        .build();
                return drawRepository.save(draw);
            }

            // 확률 기반 상품 선택
            DrawPrize selectedPrize = selectPrizeByProbability(availablePrizes);

            if (selectedPrize == null) {
                // 꽝
                Draw draw = Draw.builder()
                        .userId(userId)
                        .challengeId(challengeId)
                        .prizeId(null)
                        .result(DrawResult.LOSE)
                        .build();
                return drawRepository.save(draw);
            }

            // Redis Lua Script로 원자적 재고 차감
            Long remainingStock = redisLuaService.decrementStockAtomic(selectedPrize.getId());

            if (remainingStock == null || remainingStock < 0) {
                // 재고 부족 - 꽝 처리
                Draw draw = Draw.builder()
                        .userId(userId)
                        .challengeId(challengeId)
                        .prizeId(null)
                        .result(DrawResult.LOSE)
                        .build();
                return drawRepository.save(draw);
            }

            // 당첨
            Draw draw = Draw.builder()
                    .userId(userId)
                    .challengeId(challengeId)
                    .prizeId(selectedPrize.getId())
                    .result(DrawResult.WIN)
                    .build();

            Draw savedDraw = drawRepository.save(draw);

            // DB 재고 동기화
            drawPrizeRepository.updateRemainingQuantity(selectedPrize.getId(), remainingStock.intValue());

            log.info("추첨 당첨 - userId: {}, challengeId: {}, prizeId: {}", userId, challengeId, selectedPrize.getId());

            return savedDraw;
        });
    }

    /**
     * Redis 장애 시 폴백 (DB만 사용 - 비관적 락)
     */
    @Transactional
    public Draw executeDrawFallback(Long userId, Long challengeId, Throwable t) {
        log.warn("Redis 장애, DB 폴백 처리 - userId: {}, challengeId: {}, error: {}",
                userId, challengeId, t.getMessage());

        User user = findUserById(userId);

        if (drawRepository.findByUserIdAndChallengeId(userId, challengeId).isPresent()) {
            throw new BusinessException(ErrorCode.ALREADY_DRAWN);
        }

        List<DrawPrize> availablePrizes = drawPrizeRepository.findAvailablePrizes();

        if (availablePrizes.isEmpty()) {
            Draw draw = Draw.builder()
                    .userId(userId)
                    .challengeId(challengeId)
                    .prizeId(null)
                    .result(DrawResult.LOSE)
                    .build();
            return drawRepository.save(draw);
        }

        DrawPrize selectedPrize = selectPrizeByProbability(availablePrizes);

        if (selectedPrize == null || selectedPrize.getRemainingQuantity() <= 0) {
            Draw draw = Draw.builder()
                    .userId(userId)
                    .challengeId(challengeId)
                    .prizeId(null)
                    .result(DrawResult.LOSE)
                    .build();
            return drawRepository.save(draw);
        }

        // DB 직접 재고 차감
        selectedPrize.setRemainingQuantity(selectedPrize.getRemainingQuantity() - 1);
        drawPrizeRepository.save(selectedPrize);

        Draw draw = Draw.builder()
                .userId(userId)
                .challengeId(challengeId)
                .prizeId(selectedPrize.getId())
                .result(DrawResult.WIN)
                .build();

        log.info("추첨 당첨 (폴백) - userId: {}, challengeId: {}, prizeId: {}", userId, challengeId, selectedPrize.getId());

        return drawRepository.save(draw);
    }

    /**
     * 사용자의 추첨 기록 조회
     */
    public List<Draw> getUserDraws(Long userId) {
        return drawRepository.findByUserId(userId);
    }

    /**
     * 사용자의 당첨 기록 조회
     */
    public List<Draw> getUserWins(Long userId) {
        return drawRepository.findByUserIdAndResult(userId, DrawResult.WIN);
    }

    /**
     * 미사용 추첨권 개수 조회
     */
    public Long countUnusedDraws(Long userId) {
        return drawRepository.countUnusedByUserId(userId);
    }

    /**
     * 확률 기반 상품 선택
     */
    private DrawPrize selectPrizeByProbability(List<DrawPrize> prizes) {
        double totalProbability = prizes.stream()
                .mapToDouble(DrawPrize::getProbability)
                .sum();

        // 꽝 확률 = 1 - 총 당첨 확률
        double loseProb = 1.0 - totalProbability;
        double rand = random.nextDouble();

        if (rand < loseProb) {
            return null; // 꽝
        }

        double cumulative = loseProb;
        for (DrawPrize prize : prizes) {
            cumulative += prize.getProbability();
            if (rand < cumulative) {
                return prize;
            }
        }

        return null;
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
