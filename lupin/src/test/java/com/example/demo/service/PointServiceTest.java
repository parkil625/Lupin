package com.example.demo.service;

import com.example.demo.domain.entity.PointLog;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.Role;
import com.example.demo.repository.PointLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    @DisplayName("사용자의 총 포인트를 조회한다")
    void getTotalPointsTest() {
        // given
        given(pointLogRepository.sumPointsByUser(user)).willReturn(1500L);

        // when
        long result = pointService.getTotalPoints(user);

        // then
        assertThat(result).isEqualTo(1500L);
    }

    @Test
    @DisplayName("총 포인트가 음수일 수 있다")
    void getTotalPointsNegativeTest() {
        // given
        given(pointLogRepository.sumPointsByUser(user)).willReturn(-500L);

        // when
        long result = pointService.getTotalPoints(user);

        // then
        assertThat(result).isEqualTo(-500L);
    }

    @Test
    @DisplayName("이번 달 포인트 증감을 조회한다")
    void getMonthlyPointsTest() {
        // given
        YearMonth yearMonth = YearMonth.of(2024, 11);
        given(pointLogRepository.sumPointsByUserAndMonth(user, yearMonth.atDay(1).atStartOfDay(), yearMonth.atEndOfMonth().atTime(23, 59, 59)))
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
        given(pointLogRepository.sumPointsByUserAndMonth(user, yearMonth.atDay(1).atStartOfDay(), yearMonth.atEndOfMonth().atTime(23, 59, 59)))
                .willReturn(-200L);

        // when
        long result = pointService.getMonthlyPoints(user, yearMonth);

        // then
        assertThat(result).isZero();
    }

    @Test
    @DisplayName("포인트를 적립한다")
    void addPointsTest() {
        // given
        long amount = 100L;
        given(pointLogRepository.save(any(PointLog.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        pointService.addPoints(user, amount);

        // then
        verify(pointLogRepository).save(any(PointLog.class));
    }

    @Test
    @DisplayName("포인트를 차감한다")
    void deductPointsTest() {
        // given
        long amount = 50L;
        given(pointLogRepository.save(any(PointLog.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        pointService.deductPoints(user, amount);

        // then
        verify(pointLogRepository).save(any(PointLog.class));
    }
}
