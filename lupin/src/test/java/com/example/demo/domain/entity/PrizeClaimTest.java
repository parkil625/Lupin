package com.example.demo.domain.entity;

import com.example.demo.domain.enums.PrizeType;
import com.example.demo.domain.enums.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("PrizeClaim 엔티티 테스트")
class PrizeClaimTest {

    @Test
    @DisplayName("상금 수령 생성 - 기본값 확인")
    void create_DefaultValues() {
        // given & when
        PrizeClaim prizeClaim = PrizeClaim.builder()
                .id(1L)
                .prizeType(PrizeType.FIRST_PLACE)
                .build();

        // then
        assertThat(prizeClaim.getId()).isEqualTo(1L);
        assertThat(prizeClaim.getPrizeType()).isEqualTo(PrizeType.FIRST_PLACE);
        assertThat(prizeClaim.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("사용자 설정")
    void setUser_Success() {
        // given
        User user = User.builder()
                .id(1L)
                .userId("winner")
                .email("winner@test.com")
                .password("password")
                .realName("당첨자")
                .role(Role.MEMBER)
                .build();

        PrizeClaim prizeClaim = PrizeClaim.builder()
                .id(1L)
                .prizeType(PrizeType.SECOND_PLACE)
                .build();

        // when
        prizeClaim.setUser(user);

        // then
        assertThat(prizeClaim.getUser()).isEqualTo(user);
    }

    @Test
    @DisplayName("2등 상금 생성")
    void createSecondPlacePrize_Success() {
        // given
        User user = User.builder()
                .id(1L)
                .userId("challenger")
                .email("challenger@test.com")
                .password("password")
                .realName("챌린저")
                .role(Role.MEMBER)
                .build();

        // when
        PrizeClaim prizeClaim = PrizeClaim.builder()
                .id(1L)
                .prizeType(PrizeType.SECOND_PLACE)
                .user(user)
                .build();

        // then
        assertThat(prizeClaim.getPrizeType()).isEqualTo(PrizeType.SECOND_PLACE);
        assertThat(prizeClaim.getUser()).isEqualTo(user);
    }
}
