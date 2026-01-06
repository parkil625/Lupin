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
    @DisplayName("포인트 획득(earnPoints) 시 EARN 타입으로 저장되고 포인트 변경 이벤트가 발행된다")
    void earnPointsTest() {
        // given
        long amount = 100L;

        // when
        pointService.earnPoints(user, amount);

        // then
        // [수정] PointService는 더 이상 User를 직접 업데이트하지 않으므로 user.getTotalPoints() 검증 삭제

        ArgumentCaptor<PointLog> logCaptor = ArgumentCaptor.forClass(PointLog.class);
        verify(pointLogRepository).save(logCaptor.capture());
        
        PointLog savedLog = logCaptor.getValue();
        assertThat(savedLog.getPoints()).isEqualTo(100L);
        assertThat(savedLog.getType()).isEqualTo(PointType.EARN); 

        // 이벤트 발행 여부 검증
        verify(eventPublisher).publishEvent(any(PointChangedEvent.class));
    }

    @Test
    @DisplayName("포인트 회수(deductPoints) 시 DEDUCT 타입으로 저장되고 포인트 변경 이벤트가 발행된다")
    void deductPointsTest() {
        // given
        ReflectionTestUtils.setField(user, "totalPoints", 200L); 
        long amount = 50L;

        // when
        pointService.deductPoints(user, amount);

        // then
        // [수정] PointService는 더 이상 User를 직접 업데이트하지 않으므로 user.getTotalPoints() 검증 삭제

        ArgumentCaptor<PointLog> logCaptor = ArgumentCaptor.forClass(PointLog.class);
        verify(pointLogRepository).save(logCaptor.capture());

        PointLog savedLog = logCaptor.getValue();
        assertThat(savedLog.getPoints()).isEqualTo(-50L); 
        assertThat(savedLog.getType()).isEqualTo(PointType.DEDUCT); 
        
        // 이벤트 발행 여부 검증 추가
        verify(eventPublisher).publishEvent(any(PointChangedEvent.class));
    }

    @Test
    @DisplayName("포인트 사용(usePoints) 시 USE 타입으로 저장되고 포인트 변경 이벤트가 발행된다")
    void usePointsTest() {
        // given
        ReflectionTestUtils.setField(user, "totalPoints", 1000L);
        long amount = 500L;

        // when
        pointService.usePoints(user, amount);

        // then
        // [수정] PointService는 더 이상 User를 직접 업데이트하지 않으므로 user.getTotalPoints() 검증 삭제

        ArgumentCaptor<PointLog> logCaptor = ArgumentCaptor.forClass(PointLog.class);
        verify(pointLogRepository).save(logCaptor.capture());

        PointLog savedLog = logCaptor.getValue();
        assertThat(savedLog.getPoints()).isEqualTo(-500L);
        assertThat(savedLog.getType()).isEqualTo(PointType.USE);
        
        // 이벤트 발행 여부 검증 추가
        verify(eventPublisher).publishEvent(any(PointChangedEvent.class));
    }
}