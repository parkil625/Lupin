package com.example.demo.service;

import com.example.demo.domain.entity.PointLog;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.PointType;
import com.example.demo.event.PointChangedEvent;
import com.example.demo.repository.PointLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // [추가] Lombok 로그 import
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;

@Slf4j // [추가] 로그 객체(log) 자동 생성 어노테이션
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
        // [수정] 직접 호출 제거 (리스너 위임)
        savePointLog(user, amount, PointType.EARN);
        
        log.info(">>> [PointService] Earn points event published: userId={}, amount={}", user.getId(), amount);
        eventPublisher.publishEvent(PointChangedEvent.add(user.getId(), amount));
    }

    // 2. 포인트 회수 (피드 삭제 등) -> 랭킹 반영 O
    @Transactional
    public void deductPoints(User user, long amount) {
        // [수정] 직접 호출 제거 (리스너 위임)
        savePointLog(user, -amount, PointType.DEDUCT);
        
        log.info(">>> [PointService] Deduct points event published: userId={}, amount={}", user.getId(), amount);
        eventPublisher.publishEvent(PointChangedEvent.deduct(user.getId(), amount));
    }

    // 3. 포인트 사용 (경매 등) -> 랭킹 반영 X
    @Transactional
    public void usePoints(User user, long amount) {
        // [수정] 직접 호출 제거 (리스너 위임)
        // 참고: 잔액 부족 체크가 필요하다면 여기서 user.getTotalPoints()를 확인하고 예외를 던져야 합니다.
        
        savePointLog(user, -amount, PointType.USE);
        
        log.info(">>> [PointService] Use points event published: userId={}, amount={}", user.getId(), amount);
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