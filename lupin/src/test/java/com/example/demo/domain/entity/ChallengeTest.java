package com.example.demo.domain.entity;

import com.example.demo.domain.enums.ChallengeStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Challenge 엔티티 테스트")
class ChallengeTest {

    @Test
    @DisplayName("챌린지 오픈 성공")
    void open_Success() {
        // given
        LocalDateTime opensAt = LocalDateTime.now().minusHours(1);
        LocalDateTime closesAt = LocalDateTime.now().plusDays(1);
        Challenge challenge = Challenge.builder()
                .id(1L)
                .title("테스트 챌린지")
                .opensAt(opensAt)
                .closesAt(closesAt)
                .maxWinners(10)
                .status(ChallengeStatus.SCHEDULED)
                .build();

        // when
        challenge.open(LocalDateTime.now());

        // then
        assertThat(challenge.getStatus()).isEqualTo(ChallengeStatus.ACTIVE);
    }

    @Test
    @DisplayName("챌린지 오픈 실패 - 이미 오픈됨")
    void open_AlreadyActive_ThrowsException() {
        // given
        Challenge challenge = Challenge.builder()
                .id(1L)
                .title("테스트 챌린지")
                .opensAt(LocalDateTime.now().minusHours(1))
                .closesAt(LocalDateTime.now().plusDays(1))
                .maxWinners(10)
                .status(ChallengeStatus.ACTIVE)
                .build();

        // when & then
        assertThatThrownBy(() -> challenge.open(LocalDateTime.now()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("챌린지 오픈 실패 - 시작 시간 전")
    void open_BeforeOpensAt_ThrowsException() {
        // given
        Challenge challenge = Challenge.builder()
                .id(1L)
                .title("테스트 챌린지")
                .opensAt(LocalDateTime.now().plusHours(1))
                .closesAt(LocalDateTime.now().plusDays(1))
                .maxWinners(10)
                .status(ChallengeStatus.SCHEDULED)
                .build();

        // when & then
        assertThatThrownBy(() -> challenge.open(LocalDateTime.now()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("챌린지 종료 성공")
    void close_Success() {
        // given
        LocalDateTime closesAt = LocalDateTime.now().minusHours(1);
        Challenge challenge = Challenge.builder()
                .id(1L)
                .title("테스트 챌린지")
                .opensAt(LocalDateTime.now().minusDays(1))
                .closesAt(closesAt)
                .maxWinners(10)
                .status(ChallengeStatus.ACTIVE)
                .build();

        // when
        challenge.close(LocalDateTime.now());

        // then
        assertThat(challenge.getStatus()).isEqualTo(ChallengeStatus.CLOSED);
    }

    @Test
    @DisplayName("챌린지 종료 실패 - 활성 상태 아님")
    void close_NotActive_ThrowsException() {
        // given
        Challenge challenge = Challenge.builder()
                .id(1L)
                .title("테스트 챌린지")
                .opensAt(LocalDateTime.now().minusDays(1))
                .closesAt(LocalDateTime.now().minusHours(1))
                .maxWinners(10)
                .status(ChallengeStatus.SCHEDULED)
                .build();

        // when & then
        assertThatThrownBy(() -> challenge.close(LocalDateTime.now()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("챌린지 종료 실패 - 종료 시간 전")
    void close_BeforeClosesAt_ThrowsException() {
        // given
        Challenge challenge = Challenge.builder()
                .id(1L)
                .title("테스트 챌린지")
                .opensAt(LocalDateTime.now().minusDays(1))
                .closesAt(LocalDateTime.now().plusHours(1))
                .maxWinners(10)
                .status(ChallengeStatus.ACTIVE)
                .build();

        // when & then
        assertThatThrownBy(() -> challenge.close(LocalDateTime.now()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("챌린지 참가 성공")
    void join_Success() {
        // given
        Challenge challenge = Challenge.builder()
                .id(1L)
                .title("테스트 챌린지")
                .opensAt(LocalDateTime.now().minusHours(1))
                .closesAt(LocalDateTime.now().plusHours(1))
                .maxWinners(10)
                .currentEntries(5)
                .status(ChallengeStatus.ACTIVE)
                .build();

        // when
        challenge.join(LocalDateTime.now());

        // then
        assertThat(challenge.getCurrentEntries()).isEqualTo(6);
    }

    @Test
    @DisplayName("챌린지 참가 실패 - 활성 상태 아님")
    void join_NotActive_ThrowsException() {
        // given
        Challenge challenge = Challenge.builder()
                .id(1L)
                .title("테스트 챌린지")
                .opensAt(LocalDateTime.now().minusHours(1))
                .closesAt(LocalDateTime.now().plusHours(1))
                .maxWinners(10)
                .status(ChallengeStatus.SCHEDULED)
                .build();

        // when & then
        assertThatThrownBy(() -> challenge.join(LocalDateTime.now()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("챌린지 참가 실패 - 오픈 시간 전")
    void join_BeforeOpensAt_ThrowsException() {
        // given
        Challenge challenge = Challenge.builder()
                .id(1L)
                .title("테스트 챌린지")
                .opensAt(LocalDateTime.now().plusHours(1))
                .closesAt(LocalDateTime.now().plusHours(2))
                .maxWinners(10)
                .status(ChallengeStatus.ACTIVE)
                .build();

        // when & then
        assertThatThrownBy(() -> challenge.join(LocalDateTime.now()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("챌린지 참가 실패 - 종료됨")
    void join_AfterClosesAt_ThrowsException() {
        // given
        Challenge challenge = Challenge.builder()
                .id(1L)
                .title("테스트 챌린지")
                .opensAt(LocalDateTime.now().minusHours(2))
                .closesAt(LocalDateTime.now().minusHours(1))
                .maxWinners(10)
                .status(ChallengeStatus.ACTIVE)
                .build();

        // when & then
        assertThatThrownBy(() -> challenge.join(LocalDateTime.now()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("챌린지 참가 실패 - 인원 초과")
    void join_MaxWinnersReached_ThrowsException() {
        // given
        Challenge challenge = Challenge.builder()
                .id(1L)
                .title("테스트 챌린지")
                .opensAt(LocalDateTime.now().minusHours(1))
                .closesAt(LocalDateTime.now().plusHours(1))
                .maxWinners(10)
                .currentEntries(10)
                .status(ChallengeStatus.ACTIVE)
                .build();

        // when & then
        assertThatThrownBy(() -> challenge.join(LocalDateTime.now()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("참가 가능 여부 확인 - 가능")
    void canJoin_True() {
        // given
        Challenge challenge = Challenge.builder()
                .id(1L)
                .title("테스트 챌린지")
                .opensAt(LocalDateTime.now().minusHours(1))
                .closesAt(LocalDateTime.now().plusHours(1))
                .maxWinners(10)
                .currentEntries(5)
                .status(ChallengeStatus.ACTIVE)
                .build();

        // when & then
        assertThat(challenge.canJoin(LocalDateTime.now())).isTrue();
    }

    @Test
    @DisplayName("참가 가능 여부 확인 - 불가능 (상태)")
    void canJoin_False_NotActive() {
        // given
        Challenge challenge = Challenge.builder()
                .id(1L)
                .title("테스트 챌린지")
                .opensAt(LocalDateTime.now().minusHours(1))
                .closesAt(LocalDateTime.now().plusHours(1))
                .maxWinners(10)
                .status(ChallengeStatus.SCHEDULED)
                .build();

        // when & then
        assertThat(challenge.canJoin(LocalDateTime.now())).isFalse();
    }

    @Test
    @DisplayName("참가 가능 여부 확인 - 불가능 (인원)")
    void canJoin_False_MaxReached() {
        // given
        Challenge challenge = Challenge.builder()
                .id(1L)
                .title("테스트 챌린지")
                .opensAt(LocalDateTime.now().minusHours(1))
                .closesAt(LocalDateTime.now().plusHours(1))
                .maxWinners(10)
                .currentEntries(10)
                .status(ChallengeStatus.ACTIVE)
                .build();

        // when & then
        assertThat(challenge.canJoin(LocalDateTime.now())).isFalse();
    }

    // 경계 조건 테스트
    @Test
    @DisplayName("경계 조건 - maxWinners가 1인 경우")
    void boundary_MaxWinnersOne() {
        // given
        Challenge challenge = Challenge.builder()
                .id(1L)
                .title("최소 인원 챌린지")
                .opensAt(LocalDateTime.now().minusHours(1))
                .closesAt(LocalDateTime.now().plusHours(1))
                .maxWinners(1)
                .currentEntries(0)
                .status(ChallengeStatus.ACTIVE)
                .build();

        // when
        challenge.join(LocalDateTime.now());

        // then
        assertThat(challenge.getCurrentEntries()).isEqualTo(1);
        assertThat(challenge.canJoin(LocalDateTime.now())).isFalse();
    }

    @Test
    @DisplayName("경계 조건 - 정확히 종료 시간에 참가 시도")
    void boundary_ExactlyAtClosesAt() {
        // given
        LocalDateTime closesAt = LocalDateTime.of(2024, 1, 1, 12, 0);
        Challenge challenge = Challenge.builder()
                .id(1L)
                .title("테스트 챌린지")
                .opensAt(closesAt.minusHours(1))
                .closesAt(closesAt)
                .maxWinners(10)
                .currentEntries(0)
                .status(ChallengeStatus.ACTIVE)
                .build();

        // when & then - 정확히 종료 시간이면 참가 불가
        assertThat(challenge.canJoin(closesAt)).isFalse();
    }

    @Test
    @DisplayName("경계 조건 - 정확히 시작 시간에 참가 시도")
    void boundary_ExactlyAtOpensAt() {
        // given
        LocalDateTime opensAt = LocalDateTime.of(2024, 1, 1, 12, 0);
        Challenge challenge = Challenge.builder()
                .id(1L)
                .title("테스트 챌린지")
                .opensAt(opensAt)
                .closesAt(opensAt.plusHours(1))
                .maxWinners(10)
                .currentEntries(0)
                .status(ChallengeStatus.ACTIVE)
                .build();

        // when & then - 정확히 시작 시간이면 참가 가능
        assertThat(challenge.canJoin(opensAt)).isTrue();
    }

    @Test
    @DisplayName("경계 조건 - currentEntries가 maxWinners-1일 때 마지막 참가")
    void boundary_LastSlot() {
        // given
        Challenge challenge = Challenge.builder()
                .id(1L)
                .title("테스트 챌린지")
                .opensAt(LocalDateTime.now().minusHours(1))
                .closesAt(LocalDateTime.now().plusHours(1))
                .maxWinners(10)
                .currentEntries(9)
                .status(ChallengeStatus.ACTIVE)
                .build();

        // when
        assertThat(challenge.canJoin(LocalDateTime.now())).isTrue();
        challenge.join(LocalDateTime.now());

        // then
        assertThat(challenge.getCurrentEntries()).isEqualTo(10);
        assertThat(challenge.canJoin(LocalDateTime.now())).isFalse();
    }

    @Test
    @DisplayName("기본값 확인 - 생성 시 currentEntries는 0")
    void defaultValues_CurrentEntries() {
        // given & when
        Challenge challenge = Challenge.builder()
                .id(1L)
                .title("테스트")
                .opensAt(LocalDateTime.now())
                .closesAt(LocalDateTime.now().plusDays(1))
                .maxWinners(10)
                .build();

        // then
        assertThat(challenge.getCurrentEntries()).isEqualTo(0);
        assertThat(challenge.getStatus()).isEqualTo(ChallengeStatus.SCHEDULED);
    }

    @Test
    @DisplayName("여러 번 연속 참가")
    void join_Multiple_Success() {
        // given
        Challenge challenge = Challenge.builder()
                .id(1L)
                .title("테스트 챌린지")
                .opensAt(LocalDateTime.now().minusHours(1))
                .closesAt(LocalDateTime.now().plusHours(1))
                .maxWinners(5)
                .currentEntries(0)
                .status(ChallengeStatus.ACTIVE)
                .build();

        // when
        for (int i = 0; i < 5; i++) {
            challenge.join(LocalDateTime.now());
        }

        // then
        assertThat(challenge.getCurrentEntries()).isEqualTo(5);
    }

    @Test
    @DisplayName("참가 가능 여부 - 종료 시간 전")
    void canJoin_BeforeClosesAt() {
        // given
        LocalDateTime closesAt = LocalDateTime.now().plusHours(1);
        Challenge challenge = Challenge.builder()
                .id(1L)
                .title("테스트 챌린지")
                .opensAt(LocalDateTime.now().minusHours(1))
                .closesAt(closesAt)
                .maxWinners(10)
                .status(ChallengeStatus.ACTIVE)
                .build();

        // when & then
        assertThat(challenge.canJoin(closesAt.minusSeconds(1))).isTrue();
    }
}
