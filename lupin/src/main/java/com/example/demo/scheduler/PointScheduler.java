package com.example.demo.scheduler;

import com.example.demo.domain.entity.User;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 포인트 관련 스케줄러
 * - 매월 1일 0시에 월간 포인트 초기화
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PointScheduler {

    private final UserRepository userRepository;

    /**
     * 매월 1일 0시에 월간 포인트 초기화
     * - monthlyPoints: 0으로 리셋
     * - currentPoints: 0으로 리셋 (추첨권 잔여분도 리셋)
     */
    @Scheduled(cron = "0 0 0 1 * *") // 매월 1일 0시 0분 0초
    @Transactional
    public void resetMonthlyPoints() {
        log.info("=== 월간 포인트 초기화 시작 ===");

        List<User> allUsers = userRepository.findAll();
        int resetCount = 0;

        for (User user : allUsers) {
            if (user.getMonthlyPoints() > 0 || user.getCurrentPoints() > 0 || user.getMonthlyLikes() > 0) {
                user.resetMonthlyData();
                resetCount++;
            }
        }

        log.info("=== 월간 포인트 초기화 완료 - {}명 리셋 ===", resetCount);
    }
}
