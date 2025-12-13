package com.example.demo.service;

import com.example.demo.config.properties.PenaltyProperties;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.entity.UserPenalty;
import com.example.demo.domain.enums.PenaltyType;
import com.example.demo.domain.enums.Role;
import com.example.demo.repository.UserPenaltyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserPenaltyService 테스트")
class UserPenaltyServiceTest {

    @Mock
    private UserPenaltyRepository userPenaltyRepository;

    @Mock
    private PenaltyProperties penaltyProperties;

    @InjectMocks
    private UserPenaltyService userPenaltyService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .userId("testUser")
                .password("password")
                .name("테스트유저")
                .role(Role.MEMBER)
                .build();

        // PenaltyProperties mock 설정 (lenient - 모든 테스트에서 사용되지 않아도 에러 발생 안함)
        lenient().when(penaltyProperties.getDurationDays()).thenReturn(3);
        lenient().when(penaltyProperties.getThresholdMultiplier()).thenReturn(5);
    }

    @Test
    @DisplayName("사용자에게 제재를 부여한다")
    void addPenaltyTest() {
        // given
        PenaltyType penaltyType = PenaltyType.FEED;
        given(userPenaltyRepository.save(any(UserPenalty.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        UserPenalty result = userPenaltyService.addPenalty(user, penaltyType);

        // then
        assertThat(result.getUser()).isEqualTo(user);
        assertThat(result.getPenaltyType()).isEqualTo(penaltyType);
        verify(userPenaltyRepository).save(any(UserPenalty.class));
    }

    @Test
    @DisplayName("활성 제재가 있으면 true를 반환한다")
    void hasActivePenaltyTrueTest() {
        // given
        PenaltyType penaltyType = PenaltyType.FEED;
        given(userPenaltyRepository.existsByUserAndPenaltyTypeAndCreatedAtAfter(
                eq(user), eq(penaltyType), any(LocalDateTime.class)))
                .willReturn(true);

        // when
        boolean result = userPenaltyService.hasActivePenalty(user, penaltyType);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("활성 제재가 없으면 false를 반환한다")
    void hasActivePenaltyFalseTest() {
        // given
        PenaltyType penaltyType = PenaltyType.COMMENT;
        given(userPenaltyRepository.existsByUserAndPenaltyTypeAndCreatedAtAfter(
                eq(user), eq(penaltyType), any(LocalDateTime.class)))
                .willReturn(false);

        // when
        boolean result = userPenaltyService.hasActivePenalty(user, penaltyType);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("신고가 좋아요의 5배 이상이면 페널티 조건을 충족한다")
    void shouldApplyPenaltyTrueTest() {
        // given
        long likeCount = 2;
        long reportCount = 10;

        // when
        boolean result = userPenaltyService.shouldApplyPenalty(likeCount, reportCount);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("신고가 좋아요의 5배 미만이면 페널티 조건을 충족하지 않는다")
    void shouldApplyPenaltyFalseTest() {
        // given
        long likeCount = 2;
        long reportCount = 9;

        // when
        boolean result = userPenaltyService.shouldApplyPenalty(likeCount, reportCount);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("좋아요가 0이면 1로 계산하여 신고 5개 이상이면 페널티 조건을 충족한다")
    void shouldApplyPenaltyWithZeroLikesTest() {
        // given
        long likeCount = 0;
        long reportCount = 5;

        // when
        boolean result = userPenaltyService.shouldApplyPenalty(likeCount, reportCount);

        // then
        assertThat(result).isTrue();
    }
}
