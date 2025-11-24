package com.example.demo.domain.entity;

import com.example.demo.domain.enums.ChallengeStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Challenge 엔티티 테스트")
class ChallengeTest {

    @Test
    @DisplayName("챌린지 생성 - 기본값 확인")
    void create_Success() {
        // given & when
        Challenge challenge = Challenge.builder()
                .id(1L)
                .title("테스트 챌린지")
                .openTime(LocalDateTime.now())
                .endTime(LocalDateTime.now().plusDays(7))
                .status(ChallengeStatus.SCHEDULED)
                .build();

        // then
        assertThat(challenge.getId()).isEqualTo(1L);
        assertThat(challenge.getTitle()).isEqualTo("테스트 챌린지");
        assertThat(challenge.getStatus()).isEqualTo(ChallengeStatus.SCHEDULED);
    }

    @Test
    @DisplayName("챌린지 상태 - OPEN")
    void status_Open() {
        // given & when
        Challenge challenge = Challenge.builder()
                .id(1L)
                .title("오픈 챌린지")
                .openTime(LocalDateTime.now().minusHours(1))
                .endTime(LocalDateTime.now().plusDays(1))
                .status(ChallengeStatus.OPEN)
                .build();

        // then
        assertThat(challenge.getStatus()).isEqualTo(ChallengeStatus.OPEN);
    }

    @Test
    @DisplayName("챌린지 상태 - CLOSED")
    void status_Closed() {
        // given & when
        Challenge challenge = Challenge.builder()
                .id(1L)
                .title("종료된 챌린지")
                .openTime(LocalDateTime.now().minusDays(2))
                .endTime(LocalDateTime.now().minusDays(1))
                .status(ChallengeStatus.CLOSED)
                .build();

        // then
        assertThat(challenge.getStatus()).isEqualTo(ChallengeStatus.CLOSED);
    }

    @Test
    @DisplayName("챌린지 시간 정보 확인")
    void timeInfo_Success() {
        // given
        LocalDateTime openTime = LocalDateTime.of(2024, 1, 1, 9, 0);
        LocalDateTime endTime = LocalDateTime.of(2024, 1, 7, 18, 0);

        // when
        Challenge challenge = Challenge.builder()
                .id(1L)
                .title("시간 테스트")
                .openTime(openTime)
                .endTime(endTime)
                .status(ChallengeStatus.SCHEDULED)
                .build();

        // then
        assertThat(challenge.getOpenTime()).isEqualTo(openTime);
        assertThat(challenge.getEndTime()).isEqualTo(endTime);
    }
}
