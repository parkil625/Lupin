package com.example.demo.service;

import com.example.demo.domain.entity.PointLog;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.Role;
import com.example.demo.event.PointChangedEvent;
import com.example.demo.repository.PointLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
    }

    @Test
    @DisplayName("사용자의 총 포인트를 조회한다 (반정규화 필드 사용)")
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
        ReflectionTestUtils.setField(user, "id", 1L);
        // userId만 사용하여 detached entity 문제 방지
        given(pointLogRepository.sumPointsByUserIdAndMonth(user.getId(), yearMonth.atDay(1).atStartOfDay(), yearMonth.atEndOfMonth().atTime(23, 59, 59)))
                .willReturn(300L);

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
        ReflectionTestUtils.setField(user, "id", 1L);
        // userId만 사용하여 detached entity 문제 방지
        given(pointLogRepository.sumPointsByUserIdAndMonth(user.getId(), yearMonth.atDay(1).atStartOfDay(), yearMonth.atEndOfMonth().atTime(23, 59, 59)))
                .willReturn(-200L);

        // when
        long result = pointService.getMonthlyPoints(user, yearMonth);

        // then
        assertThat(result).isZero();
    }

    @Test
    @DisplayName("포인트를 적립하면 PointLog 저장 및 이벤트 발행")
    void addPointsTest() {
        // given
        long amount = 100L;
        ReflectionTestUtils.setField(user, "id", 1L);
        given(pointLogRepository.save(any(PointLog.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        pointService.addPoints(user, amount);

        // then
        verify(pointLogRepository).save(any(PointLog.class));
        verify(eventPublisher).publishEvent(any(PointChangedEvent.class));
    }

    @Test
    @DisplayName("포인트를 차감하면 PointLog 저장 및 이벤트 발행")
    void deductPointsTest() {
        // given
        long amount = 50L;
        ReflectionTestUtils.setField(user, "id", 1L);
        given(pointLogRepository.save(any(PointLog.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        pointService.deductPoints(user, amount);

        // then
        verify(pointLogRepository).save(any(PointLog.class));
        verify(eventPublisher).publishEvent(any(PointChangedEvent.class));
    }
}
