package com.example.demo.scheduler;

import com.example.demo.domain.entity.User;
import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PointScheduler 테스트")
class PointSchedulerTest {

    @InjectMocks
    private PointScheduler pointScheduler;

    @Mock
    private UserRepository userRepository;

    @Test
    @DisplayName("월간 포인트 초기화 성공")
    void resetMonthlyPoints_Success() {
        // given
        User user1 = User.builder().id(1L).userId("user01").build();
        ReflectionTestUtils.setField(user1, "monthlyPoints", 100L);
        ReflectionTestUtils.setField(user1, "currentPoints", 50L);

        User user2 = User.builder().id(2L).userId("user02").build();
        ReflectionTestUtils.setField(user2, "monthlyPoints", 200L);

        given(userRepository.findAll()).willReturn(Arrays.asList(user1, user2));

        // when
        pointScheduler.resetMonthlyPoints();

        // then
        then(userRepository).should().findAll();
    }

    @Test
    @DisplayName("초기화할 사용자가 없는 경우")
    void resetMonthlyPoints_NoUsers() {
        // given
        given(userRepository.findAll()).willReturn(Collections.emptyList());

        // when
        pointScheduler.resetMonthlyPoints();

        // then
        then(userRepository).should().findAll();
    }

    @Test
    @DisplayName("포인트가 0인 사용자는 리셋하지 않음")
    void resetMonthlyPoints_ZeroPoints() {
        // given
        User user = User.builder().id(1L).userId("user01").build();
        // monthlyPoints = 0, currentPoints = 0

        given(userRepository.findAll()).willReturn(Arrays.asList(user));

        // when
        pointScheduler.resetMonthlyPoints();

        // then
        then(userRepository).should().findAll();
    }
}
