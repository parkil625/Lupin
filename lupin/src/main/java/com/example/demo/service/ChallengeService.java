package com.example.demo.service;

import com.example.demo.domain.entity.Challenge;
import com.example.demo.domain.entity.ChallengeEntry;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.ChallengeStatus;
import com.example.demo.dto.response.ChallengeJoinResponse;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.ChallengeEntryRepository;
import com.example.demo.repository.ChallengeRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

/**
 * 챌린지 관련 비즈니스 로직을 처리하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChallengeService {

    private final ChallengeRepository challengeRepository;
    private final ChallengeEntryRepository challengeEntryRepository;
    private final UserRepository userRepository;
    private final DistributedLockService lockService;
    private final RedisLuaService redisLuaService;

    /**
     * 활성화된 챌린지 목록 조회
     */
    public List<Challenge> getActiveChallenges() {
        return challengeRepository.findActiveChallenges(LocalDateTime.now());
    }

    /**
     * 챌린지 상세 조회
     */
    public Challenge getChallengeDetail(Long challengeId) {
        return findChallengeById(challengeId);
    }

    /**
     * 챌린지 참가 (분산 락 + Lua Script 원자적 처리)
     */
    @Transactional
    @CircuitBreaker(name = "redis", fallbackMethod = "joinChallengeFallback")
    public void joinChallenge(Long challengeId, Long userId) {
        lockService.withChallengeJoinLock(challengeId, userId, () -> {
            Challenge challenge = findChallengeById(challengeId);
            User user = findUserById(userId);

            // 챌린지 참가 가능 여부 확인
            if (!challenge.canJoin(LocalDateTime.now())) {
                throw new BusinessException(ErrorCode.CHALLENGE_NOT_ACTIVE);
            }

            // Redis Lua Script로 원자적 참가 처리
            boolean joined = redisLuaService.joinChallengeAtomic(challengeId, userId);
            if (!joined) {
                throw new BusinessException(ErrorCode.ALREADY_JOINED_CHALLENGE);
            }

            // DB에도 저장 (영속성 보장)
            ChallengeEntry entry = ChallengeEntry.of(challenge, user, LocalDateTime.now());
            challengeEntryRepository.save(entry);

            log.info("챌린지 참가 완료 - challengeId: {}, userId: {}", challengeId, userId);
            return null;
        });
    }

    /**
     * Redis 장애 시 폴백 (DB만 사용)
     */
    public void joinChallengeFallback(Long challengeId, Long userId, Throwable t) {
        log.warn("Redis 장애, DB 폴백 처리 - challengeId: {}, userId: {}, error: {}",
                challengeId, userId, t.getMessage());

        Challenge challenge = findChallengeById(challengeId);
        User user = findUserById(userId);

        if (challengeEntryRepository.existsByChallengeIdAndUserId(challengeId, userId)) {
            throw new BusinessException(ErrorCode.ALREADY_JOINED_CHALLENGE);
        }

        if (!challenge.canJoin(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.CHALLENGE_NOT_ACTIVE);
        }

        ChallengeEntry entry = ChallengeEntry.of(challenge, user, LocalDateTime.now());
        challengeEntryRepository.save(entry);

        log.info("챌린지 참가 완료 (폴백) - challengeId: {}, userId: {}", challengeId, userId);
    }

    /**
     * 챌린지 참가 여부 확인
     */
    public boolean isUserJoined(Long challengeId, Long userId) {
        return challengeEntryRepository.existsByChallengeIdAndUserId(challengeId, userId);
    }

    /**
     * 챌린지 참가자 목록 조회
     */
    public List<ChallengeEntry> getChallengeEntries(Long challengeId) {
        return challengeEntryRepository.findByChallengeIdOrderByJoinedAtAsc(challengeId);
    }

    /**
     * 챌린지 시작
     */
    @Transactional
    public void startChallenge(Long challengeId) {
        LocalDateTime now = LocalDateTime.now();
        Challenge challenge = findChallengeById(challengeId);

        try {
            challenge.open(now);
            log.info("챌린지 시작 - challengeId: {}", challengeId);
        } catch (IllegalStateException e) {
            throw new BusinessException(ErrorCode.CHALLENGE_NOT_ACTIVE, e.getMessage());
        }
    }

    /**
     * 챌린지 종료
     */
    @Transactional
    public void closeChallenge(Long challengeId) {
        Challenge challenge = findChallengeById(challengeId);
        challenge.close(LocalDateTime.now());

        log.info("챌린지 종료 - challengeId: {}", challengeId);
    }

    /**
     * ID로 챌린지 조회 (내부 메서드)
     */
    private Challenge findChallengeById(Long challengeId) {
        return challengeRepository.findById(challengeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHALLENGE_NOT_FOUND));
    }

    /**
     * ID로 사용자 조회 (내부 메서드)
     */
    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    public ChallengeJoinResponse checkChallengeByUserId(Long challengeId, Long userId) {
        return challengeEntryRepository.findByChallengeIdAndUserId(challengeId, userId)
                .map(entry -> ChallengeJoinResponse.from(entry))
                .orElse(ChallengeJoinResponse.notJoined());
    }
}
