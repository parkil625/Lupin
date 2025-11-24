package com.example.demo.domain.entity;

import com.example.demo.domain.enums.ChallengeStatus;
import com.example.demo.domain.enums.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@DisplayName("ChallengeEntry 엔티티 테스트")
class ChallengeEntryTest {

    @Test
    @DisplayName("ChallengeEntry 정적 팩토리 메서드로 생성")
    void of_Success() {
        // given
        LocalDateTime now = LocalDateTime.now();
        Challenge challenge = Challenge.builder()
                .id(1L)
                .title("테스트 챌린지")
                .opensAt(now.minusHours(1))
                .closesAt(now.plusHours(1))
                .maxWinners(10)
                .status(ChallengeStatus.ACTIVE)
                .build();

        User user = User.builder()
                .id(1L)
                .userId("user01")
                .email("user@test.com")
                .password("password")
                .realName("테스터")
                .role(Role.MEMBER)
                .build();

        // when
        ChallengeEntry entry = ChallengeEntry.of(challenge, user, now);

        // then
        assertThat(entry.getChallenge()).isEqualTo(challenge);
        assertThat(entry.getUser()).isEqualTo(user);
        assertThat(entry.getJoinedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("ChallengeEntry 생성 시 챌린지 참가자 수 증가")
    void of_IncreasesParticipantCount() {
        // given
        LocalDateTime now = LocalDateTime.now();
        Challenge challenge = Challenge.builder()
                .id(1L)
                .title("참가자 증가 테스트")
                .opensAt(now.minusHours(1))
                .closesAt(now.plusHours(1))
                .maxWinners(5)
                .status(ChallengeStatus.ACTIVE)
                .build();

        User user = User.builder()
                .id(1L)
                .userId("user01")
                .email("user@test.com")
                .password("password")
                .realName("테스터")
                .role(Role.MEMBER)
                .build();

        int initialCount = challenge.getCurrentEntries();

        // when
        ChallengeEntry.of(challenge, user, now);

        // then
        assertThat(challenge.getCurrentEntries()).isEqualTo(initialCount + 1);
    }

    @Test
    @DisplayName("빌더로 ChallengeEntry 생성")
    void builder_Success() {
        // given
        LocalDateTime joinedAt = LocalDateTime.of(2024, 1, 1, 10, 0);

        // when
        ChallengeEntry entry = ChallengeEntry.builder()
                .id(1L)
                .joinedAt(joinedAt)
                .build();

        // then
        assertThat(entry.getId()).isEqualTo(1L);
        assertThat(entry.getJoinedAt()).isEqualTo(joinedAt);
    }
}
