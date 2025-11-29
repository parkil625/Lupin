package com.example.demo.service;

import com.example.demo.domain.entity.PointLog;
import com.example.demo.domain.entity.User;
import com.example.demo.repository.PointLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PointService {

    private final PointLogRepository pointLogRepository;

    public long getTotalPoints(User user) {
        return pointLogRepository.sumPointsByUser(user);
    }

    public long getMonthlyPoints(User user, YearMonth yearMonth) {
        long sum = pointLogRepository.sumPointsByUserAndMonth(
                user,
                yearMonth.atDay(1).atStartOfDay(),
                yearMonth.atEndOfMonth().atTime(23, 59, 59)
        );
        return Math.max(sum, 0);
    }

    @Transactional
    public void addPoints(User user, long amount) {
        savePointLog(user, amount);
    }

    @Transactional
    public void deductPoints(User user, long amount) {
        savePointLog(user, -amount);
    }

    private void savePointLog(User user, long points) {
        PointLog pointLog = PointLog.builder()
                .user(user)
                .points(points)
                .build();
        pointLogRepository.save(pointLog);
    }
}
