package com.example.demo.service;

import com.example.demo.domain.entity.PointLog;
import com.example.demo.domain.entity.User;
import com.example.demo.event.PointChangedEvent;
import com.example.demo.repository.PointLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PointService {

    private final PointLogRepository pointLogRepository;
    private final ApplicationEventPublisher eventPublisher;

    public long getTotalPoints(User user) {
        return user.getTotalPoints();
    }

    public long getMonthlyPoints(User user, YearMonth yearMonth) {
        // user.getId()만 사용하여 detached entity (@Version null) 문제 방지
        long sum = pointLogRepository.sumPointsByUserIdAndMonth(
                user.getId(),
                yearMonth.atDay(1).atStartOfDay(),
                yearMonth.atEndOfMonth().atTime(23, 59, 59)
        );
        return Math.max(sum, 0);
    }

    @Transactional
    public void addPoints(User user, long amount) {
        savePointLog(user, amount);
        eventPublisher.publishEvent(PointChangedEvent.add(user.getId(), amount));
    }

    @Transactional
    public void deductPoints(User user, long amount) {
        savePointLog(user, -amount);
        eventPublisher.publishEvent(PointChangedEvent.deduct(user.getId(), amount));
    }

    /**
     * 피드 포인트 조정 (수정 시 기존 포인트 회수 후 새 포인트 부여)
     * 포인트 정책은 PointService가 담당 (SRP)
     */
    @Transactional
    public void adjustFeedPoints(User user, long oldPoints, long newPoints) {
        if (oldPoints > 0) {
            deductPoints(user, oldPoints);
        }
        if (newPoints > 0) {
            addPoints(user, newPoints);
        }
    }

    private void savePointLog(User user, long points) {
        PointLog pointLog = PointLog.builder()
                .user(user)
                .points(points)
                .build();
        pointLogRepository.save(pointLog);
    }
}
