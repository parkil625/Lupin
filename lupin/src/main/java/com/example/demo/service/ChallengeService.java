package com.example.demo.service;

import com.example.demo.domain.entity.Challenge;
import com.example.demo.domain.entity.ChallengeEntry;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.ChallengeStatus;
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
     * 챌린지 참가
     */
    @Transactional
    public void joinChallenge(Long challengeId, Long userId) {
        Challenge challenge = findChallengeById(challengeId);
        User user = findUserById(userId);
        LocalDateTime now =  LocalDateTime.now();

        // 이미 참가한 경우
        if (challengeEntryRepository.existsByChallengeIdAndUserId(challengeId, userId)) {
            throw new BusinessException(ErrorCode.ALREADY_JOINED_CHALLENGE);
        }

        // 챌린지 참가 가능 여부 확인
        if (!challenge.canJoin(now)) {
            throw new BusinessException(ErrorCode.CHALLENGE_NOT_ACTIVE);
        }

        ChallengeEntry entry = ChallengeEntry.of(challenge, user);
        challengeEntryRepository.save(entry);

        log.info("챌린지 참가 완료 - challengeId: {}, userId: {}", challengeId, userId);
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
        Challenge challenge = findChallengeById(challengeId);

        try {
            challenge.open();
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
}
