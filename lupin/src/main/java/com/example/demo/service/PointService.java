package com.example.demo.service;

import com.example.demo.config.properties.FeedProperties;
import com.example.demo.domain.entity.User;
import com.example.demo.event.PointChangedEvent;
import com.example.demo.repository.PointLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PointService {

    private final PointLogRepository pointLogRepository;
    private final FeedProperties feedProperties;
    private final PointManager pointManager;
    private final ApplicationEventPublisher eventPublisher;

    public long getTotalPoints(User user) {
        return user.getTotalPoints();
    }

    public long getMonthlyPoints(User user, YearMonth yearMonth) {
        long sum = pointLogRepository.sumPointsByUserIdAndMonth(
                user.getId(),
                yearMonth.atDay(1).atStartOfDay(),
                yearMonth.atEndOfMonth().atTime(23, 59, 59)
        );
        return Math.max(sum, 0);
    }

    @Transactional
    public void addPoints(User user, long amount) {
        pointManager.addPoints(user, amount);
        eventPublisher.publishEvent(PointChangedEvent.add(user.getId(), amount));
    }

    @Transactional
    public void deductPoints(User user, long amount) {
        pointManager.cancelPoints(user, amount); // cancelPoints는 음수로 기록하므로 deduct와 동일
        eventPublisher.publishEvent(PointChangedEvent.deduct(user.getId(), amount));
    }

    @Transactional
    public void recoverFeedPoints(User user, long points, LocalDateTime feedCreatedAt) {
        int recoveryDays = feedProperties.getPointRecoveryDays();
        LocalDateTime deadline = LocalDateTime.now().minusDays(recoveryDays);
        if (feedCreatedAt.isAfter(deadline)) {
            pointManager.cancelPoints(user, points);
            eventPublisher.publishEvent(PointChangedEvent.deduct(user.getId(), points));
        }
    }

    @Transactional
    public void adjustFeedPoints(User user, long oldPoints, long newPoints) {
        if (oldPoints > 0) {
            pointManager.cancelPoints(user, oldPoints);
            eventPublisher.publishEvent(PointChangedEvent.deduct(user.getId(), oldPoints));
        }
        if (newPoints > 0) {
            pointManager.addPoints(user, newPoints);
            eventPublisher.publishEvent(PointChangedEvent.add(user.getId(), newPoints));
        }
    }
}
