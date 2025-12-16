package com.example.demo.service;

import com.example.demo.domain.entity.PointLog;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.PointType;
import com.example.demo.domain.enums.Role;
import com.example.demo.event.PointChangedEvent;
import com.example.demo.repository.PointLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("PointService 테스트")
class PointServiceTest {

    @Mock
    private PointLogRepository pointLogRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private PointService pointService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .userId("user")
                .password("password")
                .name("사용자")
                .role(Role.MEMBER)
                .build();

        ReflectionTestUtils.setField(user, "id", 1L);
    }

    @Test
    @DisplayName("사용자의 총 포인트를 조회한다")
    void getTotalPointsTest() {
        // given
        ReflectionTestUtils.setField(user, "totalPoints", 1500L);

        // when
        long result = pointService.getTotalPoints(user);

        // then
        assertThat(result).isEqualTo(1500L);
    }

    @Test
    @DisplayName("총 포인트 기본값은 0이다")
    void getTotalPointsDefaultZeroTest() {
        // when
        long result = pointService.getTotalPoints(user);

        // then
        assertThat(result).isZero();
    }

    @Test
    @DisplayName("이번 달 포인트 증감을 조회한다")
    void getMonthlyPointsTest() {
        // given
        YearMonth yearMonth = YearMonth.of(2024, 11);
        given(pointLogRepository.sumPointsByUserIdAndMonth(
                any(Long.class), 
                any(), 
                any()
        )).willReturn(300L);

        // when
        long result = pointService.getMonthlyPoints(user, yearMonth);

        // then
        assertThat(result).isEqualTo(300L);
    }

    @Test
    @DisplayName("이번 달 포인트 증감이 음수면 0을 반환한다")
    void getMonthlyPointsNegativeReturnsZeroTest() {
        // given
        YearMonth yearMonth = YearMonth.of(2024, 11);
        given(pointLogRepository.sumPointsByUserIdAndMonth(
                any(Long.class), 
                any(), 
                any()
        )).willReturn(-200L);

        // when
        long result = pointService.getMonthlyPoints(user, yearMonth);

        // then
        assertThat(result).isZero();
    }

    @Test
    @DisplayName("포인트 획득(earnPoints) 시 EARN 타입으로 저장되고 보유 포인트가 증가한다")
    void earnPointsTest() {
        // given
        long amount = 100L;

        // when
        pointService.earnPoints(user, amount);

        // then
        assertThat(user.getTotalPoints()).isEqualTo(100L);

        ArgumentCaptor<PointLog> logCaptor = ArgumentCaptor.forClass(PointLog.class);
        verify(pointLogRepository).save(logCaptor.capture());
        
        PointLog savedLog = logCaptor.getValue();
        assertThat(savedLog.getPoints()).isEqualTo(100L);
        assertThat(savedLog.getType()).isEqualTo(PointType.EARN); 

        verify(eventPublisher).publishEvent(any(PointChangedEvent.class));
    }

    @Test
    @DisplayName("포인트 회수(deductPoints) 시 DEDUCT 타입으로 저장되고 보유 포인트가 감소한다")
    void deductPointsTest() {
        // given
        ReflectionTestUtils.setField(user, "totalPoints", 200L); 
        long amount = 50L;

        // when
        pointService.deductPoints(user, amount);

        // then
        assertThat(user.getTotalPoints()).isEqualTo(150L);

        ArgumentCaptor<PointLog> logCaptor = ArgumentCaptor.forClass(PointLog.class);
        verify(pointLogRepository).save(logCaptor.capture());

        PointLog savedLog = logCaptor.getValue();
        assertThat(savedLog.getPoints()).isEqualTo(-50L); 
        assertThat(savedLog.getType()).isEqualTo(PointType.DEDUCT); 
    }

    @Test
    @DisplayName("포인트 사용(usePoints) 시 USE 타입으로 저장되고 보유 포인트가 감소한다")
    void usePointsTest() {
        // given
        ReflectionTestUtils.setField(user, "totalPoints", 1000L);
        long amount = 500L;

        // when
        pointService.usePoints(user, amount);

        // then
        assertThat(user.getTotalPoints()).isEqualTo(500L);

        ArgumentCaptor<PointLog> logCaptor = ArgumentCaptor.forClass(PointLog.class);
        verify(pointLogRepository).save(logCaptor.capture());

        PointLog savedLog = logCaptor.getValue();
        assertThat(savedLog.getPoints()).isEqualTo(-500L);
        assertThat(savedLog.getType()).isEqualTo(PointType.USE);
    }
}