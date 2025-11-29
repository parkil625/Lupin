package com.example.demo.repository;

import com.example.demo.domain.entity.User;
import com.example.demo.domain.entity.UserPenalty;
import com.example.demo.domain.enums.PenaltyType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class UserPenaltyRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private UserPenaltyRepository userPenaltyRepository;

    @Test
    @DisplayName("3일 이내 활성 제재가 있으면 true를 반환한다")
    void existsActivePenaltyReturnsTrueTest() {
        // given
        User user = createAndSaveUser("testUser");
        userPenaltyRepository.save(UserPenalty.builder()
                .user(user)
                .penaltyType(PenaltyType.FEED)
                .createdAt(LocalDateTime.now().minusDays(1))
                .build());

        // when
        boolean exists = userPenaltyRepository.existsByUserAndPenaltyTypeAndCreatedAtAfter(
                user, PenaltyType.FEED, LocalDateTime.now().minusDays(3));

        // then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("3일 이전 제재만 있으면 false를 반환한다")
    void existsActivePenaltyReturnsFalseWhenExpiredTest() {
        // given
        User user = createAndSaveUser("testUser");
        userPenaltyRepository.save(UserPenalty.builder()
                .user(user)
                .penaltyType(PenaltyType.FEED)
                .createdAt(LocalDateTime.now().minusDays(5))
                .build());

        // when
        boolean exists = userPenaltyRepository.existsByUserAndPenaltyTypeAndCreatedAtAfter(
                user, PenaltyType.FEED, LocalDateTime.now().minusDays(3));

        // then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("다른 타입의 제재는 조회되지 않는다")
    void existsActivePenaltyReturnsFalseForDifferentTypeTest() {
        // given
        User user = createAndSaveUser("testUser");
        userPenaltyRepository.save(UserPenalty.builder()
                .user(user)
                .penaltyType(PenaltyType.COMMENT)
                .createdAt(LocalDateTime.now().minusDays(1))
                .build());

        // when
        boolean exists = userPenaltyRepository.existsByUserAndPenaltyTypeAndCreatedAtAfter(
                user, PenaltyType.FEED, LocalDateTime.now().minusDays(3));

        // then
        assertThat(exists).isFalse();
    }
}
