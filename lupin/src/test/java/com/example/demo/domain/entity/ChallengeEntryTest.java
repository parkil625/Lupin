package com.example.demo.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@DisplayName("ChallengeEntry 엔티티 테스트")
class ChallengeEntryTest {

    @Test
    @DisplayName("ChallengeEntry 빌더로 생성")
    void builder_Success() {
        // given
        LocalDateTime joinedAt = LocalDateTime.of(2024, 1, 1, 10, 0);

        // when
        ChallengeEntry entry = ChallengeEntry.builder()
                .id(1L)
                .challengeId(1L)
                .userId(1L)
                .joinedAt(joinedAt)
                .build();

        // then
        assertThat(entry.getId()).isEqualTo(1L);
        assertThat(entry.getChallengeId()).isEqualTo(1L);
        assertThat(entry.getUserId()).isEqualTo(1L);
        assertThat(entry.getJoinedAt()).isEqualTo(joinedAt);
    }

    @Test
    @DisplayName("ChallengeEntry 필드 확인")
    void fields_Success() {
        // given & when
        ChallengeEntry entry = ChallengeEntry.builder()
                .id(100L)
                .challengeId(10L)
                .userId(5L)
                .joinedAt(LocalDateTime.now())
                .build();

        // then
        assertThat(entry.getChallengeId()).isEqualTo(10L);
        assertThat(entry.getUserId()).isEqualTo(5L);
    }
}
