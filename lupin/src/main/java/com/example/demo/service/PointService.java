package com.example.demo.service;

import com.example.demo.domain.entity.PointLog;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.PointType;
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
        long sum = pointLogRepository.sumPointsByUserIdAndMonth(
                user.getId(),
                yearMonth.atDay(1).atStartOfDay(),
                yearMonth.atEndOfMonth().atTime(23, 59, 59)
        );
        return Math.max(sum, 0);
    }

    // 1. 포인트 획득 (피드 작성 등) -> 랭킹 반영 O
    @Transactional
    public void earnPoints(User user, long amount) {
        user.addPoints(amount);
        savePointLog(user, amount, PointType.EARN);
        eventPublisher.publishEvent(PointChangedEvent.add(user.getId(), amount));
    }

    // 2. 포인트 회수 (피드 삭제 등) -> 랭킹 반영 O
    @Transactional
    public void deductPoints(User user, long amount) {
        user.deductPoints(amount);
        savePointLog(user, -amount, PointType.DEDUCT);
        eventPublisher.publishEvent(PointChangedEvent.deduct(user.getId(), amount));
    }

    // 3. 포인트 사용 (경매 등) -> 랭킹 반영 X, 잔액 부족 시 음수(빚) 허용
    @Transactional
    public void usePoints(User user, long amount) {
        
        // 그냥 차감 (User 엔티티가 음수를 허용하도록 수정되어야 함)
        user.deductPoints(amount); 
        
        savePointLog(user, -amount, PointType.USE);
        eventPublisher.publishEvent(PointChangedEvent.deduct(user.getId(), amount));
    }

    // 피드 수정 시 포인트 조정 로직
    @Transactional
    public void adjustFeedPoints(User user, long oldPoints, long newPoints) {
        if (oldPoints > 0) {
            deductPoints(user, oldPoints);
        }
        if (newPoints > 0) {
            earnPoints(user, newPoints);
        }
    }

    private void savePointLog(User user, long points, PointType type) {
        PointLog pointLog = PointLog.builder()
                .user(user)
                .points(points)
                .type(type)
                .build();
        pointLogRepository.save(pointLog);
    }
}