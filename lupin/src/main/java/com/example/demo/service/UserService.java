package com.example.demo.service;

import com.example.demo.domain.entity.PointLog;
import com.example.demo.domain.entity.User;
import com.example.demo.dto.response.UserProfileResponse;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.FeedRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 관련 비즈니스 로직을 처리하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final FeedRepository feedRepository;
    private final UserRepository userRepository;

    /**
     * 사용자 프로필 조회
     */
    public UserProfileResponse getUserProfile(Long userId) {
        User user = findUserById(userId);

        Long feedCount = feedRepository.countUserFeeds(userId);
        Long totalActivityMinutes = feedRepository.sumUserActivityDuration(userId);

        return UserProfileResponse.from(
                user,
                feedCount.intValue(),
                totalActivityMinutes != null ? totalActivityMinutes.intValue() : 0
        );
    }

    /**
     * 포인트 적립
     */
    @Transactional
    public void addPoints(Long userId, Long amount, String reason, String refId) {
        User user = findUserById(userId);

        // 포인트 적립
        user.addPoints(amount);

        // 포인트 로그 생성
        PointLog pointLog = PointLog.builder()
                .amount(amount)
                .reason(reason)
                .refId(refId)
                .build();
        pointLog.setUser(user);

        log.info("포인트 적립 완료 - userId: {}, amount: {}, reason: {}", userId, amount, reason);
    }

    /**
     * 포인트 차감
     */
    @Transactional
    public void deductPoints(Long userId, Long amount, String reason) {
        User user = findUserById(userId);

        try {
            user.usePoints(amount);

            // 포인트 로그 생성 (음수로 기록)
            PointLog pointLog = PointLog.builder()
                    .amount(-amount)
                    .reason(reason)
                    .build();
            pointLog.setUser(user);

            log.info("포인트 차감 완료 - userId: {}, amount: {}, reason: {}", userId, amount, reason);
        } catch (IllegalStateException e) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_POINTS);
        }
    }

    /**
     * 이메일로 사용자 조회
     */
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    /**
     * ID로 사용자 조회 (내부 메서드)
     */
    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    /**
     * 이메일 중복 확인
     */
    public boolean isEmailExists(String email) {
        return userRepository.existsByEmail(email);
    }
}
