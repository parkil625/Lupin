package com.example.demo.domain.entity;

import com.example.demo.domain.enums.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("User 엔티티 테스트")
class UserTest {

    @Test
    @DisplayName("포인트 추가 성공")
    void addPoints_Success() {
        // given
        User user = User.builder()
                .id(1L)
                .userId("user01")
                .email("user01@test.com")
                .password("password")
                .realName("테스트")
                .role(Role.MEMBER)
                .currentPoints(0L)
                .monthlyPoints(0L)
                .build();

        // when
        user.addPoints(10L);

        // then
        assertThat(user.getCurrentPoints()).isEqualTo(10L);
        assertThat(user.getMonthlyPoints()).isEqualTo(10L);
    }

    @Test
    @DisplayName("추첨권 발급을 위한 포인트 차감 성공")
    void deductCurrentPointsForTicket_Success() {
        // given
        User user = User.builder()
                .id(1L)
                .userId("user01")
                .email("user01@test.com")
                .password("password")
                .realName("테스트")
                .role(Role.MEMBER)
                .currentPoints(50L)
                .monthlyPoints(50L)
                .build();

        // when
        user.deductCurrentPointsForTicket();

        // then
        assertThat(user.getCurrentPoints()).isEqualTo(20L);
    }

    @Test
    @DisplayName("추첨권 발급 실패 - 포인트 부족")
    void deductCurrentPointsForTicket_InsufficientPoints_ThrowsException() {
        // given
        User user = User.builder()
                .id(1L)
                .userId("user01")
                .email("user01@test.com")
                .password("password")
                .realName("테스트")
                .role(Role.MEMBER)
                .currentPoints(20L)
                .monthlyPoints(20L)
                .build();

        // when & then
        assertThatThrownBy(() -> user.deductCurrentPointsForTicket())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("포인트가 부족합니다");
    }

    @Test
    @DisplayName("사용자 생성 - 기본값 확인")
    void create_DefaultValues() {
        // given & when
        User user = User.builder()
                .id(1L)
                .userId("user01")
                .email("user01@test.com")
                .password("password")
                .realName("테스트")
                .role(Role.MEMBER)
                .build();

        // then
        assertThat(user.getCurrentPoints()).isEqualTo(0L);
        assertThat(user.getMonthlyPoints()).isEqualTo(0L);
        assertThat(user.getMonthlyLikes()).isEqualTo(0L);
    }

    @Test
    @DisplayName("포인트 회수")
    void revokePoints_Success() {
        // given
        User user = User.builder()
                .id(1L)
                .userId("user01")
                .email("user01@test.com")
                .password("password")
                .realName("테스트")
                .role(Role.MEMBER)
                .currentPoints(50L)
                .monthlyPoints(50L)
                .build();

        // when
        user.revokePoints(30L);

        // then
        assertThat(user.getCurrentPoints()).isEqualTo(20L);
        assertThat(user.getMonthlyPoints()).isEqualTo(20L);
    }

    @Test
    @DisplayName("포인트 회수 - 0 미만으로 내려가지 않음")
    void revokePoints_NotBelowZero() {
        // given
        User user = User.builder()
                .id(1L)
                .userId("user01")
                .email("user01@test.com")
                .password("password")
                .realName("테스트")
                .role(Role.MEMBER)
                .currentPoints(10L)
                .monthlyPoints(10L)
                .build();

        // when
        user.revokePoints(50L);

        // then
        assertThat(user.getCurrentPoints()).isEqualTo(0L);
        assertThat(user.getMonthlyPoints()).isEqualTo(0L);
    }

    @Test
    @DisplayName("월별 데이터 리셋")
    void resetMonthlyData_Success() {
        // given
        User user = User.builder()
                .id(1L)
                .userId("user01")
                .email("user01@test.com")
                .password("password")
                .realName("테스트")
                .role(Role.MEMBER)
                .currentPoints(100L)
                .monthlyPoints(100L)
                .monthlyLikes(50L)
                .build();

        // when
        user.resetMonthlyData();

        // then
        assertThat(user.getCurrentPoints()).isEqualTo(0L);
        assertThat(user.getMonthlyPoints()).isEqualTo(0L);
        assertThat(user.getMonthlyLikes()).isEqualTo(0L);
    }

    @Test
    @DisplayName("월별 좋아요 증가")
    void incrementMonthlyLikes_Success() {
        // given
        User user = User.builder()
                .id(1L)
                .userId("user01")
                .email("user01@test.com")
                .password("password")
                .realName("테스트")
                .role(Role.MEMBER)
                .monthlyLikes(5L)
                .build();

        // when
        user.incrementMonthlyLikes();

        // then
        assertThat(user.getMonthlyLikes()).isEqualTo(6L);
    }

    @Test
    @DisplayName("월별 좋아요 감소")
    void decrementMonthlyLikes_Success() {
        // given
        User user = User.builder()
                .id(1L)
                .userId("user01")
                .email("user01@test.com")
                .password("password")
                .realName("테스트")
                .role(Role.MEMBER)
                .monthlyLikes(5L)
                .build();

        // when
        user.decrementMonthlyLikes();

        // then
        assertThat(user.getMonthlyLikes()).isEqualTo(4L);
    }

    @Test
    @DisplayName("월별 좋아요 감소 - 0 이하로 내려가지 않음")
    void decrementMonthlyLikes_NotBelowZero() {
        // given
        User user = User.builder()
                .id(1L)
                .userId("user01")
                .email("user01@test.com")
                .password("password")
                .realName("테스트")
                .role(Role.MEMBER)
                .monthlyLikes(0L)
                .build();

        // when
        user.decrementMonthlyLikes();

        // then
        assertThat(user.getMonthlyLikes()).isEqualTo(0L);
    }

    @Test
    @DisplayName("이름 조회")
    void getName_Success() {
        // given
        User user = User.builder()
                .id(1L)
                .userId("user01")
                .email("user01@test.com")
                .password("password")
                .realName("홍길동")
                .role(Role.MEMBER)
                .build();

        // when & then
        assertThat(user.getName()).isEqualTo("홍길동");
    }
}
