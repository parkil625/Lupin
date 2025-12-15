package com.example.demo.service;

import com.example.demo.config.properties.FeedProperties;
import com.example.demo.domain.entity.User;
import com.example.demo.event.PointChangedEvent;
import com.example.demo.repository.PointLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PointServiceTest {

    @InjectMocks
    private PointService pointService;

    @Mock
    private PointLogRepository pointLogRepository;

    @Mock
    private FeedProperties feedProperties;

    @Mock
    private PointManager pointManager;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private User user;

    @Test
    @DisplayName("총 포인트 조회")
    void getTotalPointsTest() {
        given(user.getTotalPoints()).willReturn(100L);
        long totalPoints = pointService.getTotalPoints(user);
        assertThat(totalPoints).isEqualTo(100L);
    }

    @Test
    @DisplayName("월별 포인트 조회")
    void getMonthlyPointsTest() {
        YearMonth yearMonth = YearMonth.now();
        given(pointLogRepository.sumPointsByUserIdAndMonth(any(), any(), any())).willReturn(50L);
        long monthlyPoints = pointService.getMonthlyPoints(user, yearMonth);
        assertThat(monthlyPoints).isEqualTo(50L);
    }

    @Test
    @DisplayName("포인트를 적립하면 PointLog 저장 및 이벤트 발행")
    void addPointsTest() {
        long amount = 10L;
        pointService.addPoints(user, amount);
        verify(pointManager).addPoints(user, amount);
        verify(eventPublisher).publishEvent(any(PointChangedEvent.class));
    }

    @Test
    @DisplayName("포인트를 차감하면 PointLog 저장 및 이벤트 발행")
    void deductPointsTest() {
        long amount = 10L;
        pointService.deductPoints(user, amount);
        verify(pointManager).cancelPoints(user, amount);
        verify(eventPublisher).publishEvent(any(PointChangedEvent.class));
    }

    @Test
    @DisplayName("피드 삭제 시 포인트 회수 (기간 내)")
    void recoverFeedPointsWithinPeriodTest() {
        long points = 10L;
        LocalDateTime feedCreatedAt = LocalDateTime.now();
        given(feedProperties.getPointRecoveryDays()).willReturn(7);
        pointService.recoverFeedPoints(user, points, feedCreatedAt);
        verify(pointManager).cancelPoints(user, points);
        verify(eventPublisher).publishEvent(any(PointChangedEvent.class));
    }
}