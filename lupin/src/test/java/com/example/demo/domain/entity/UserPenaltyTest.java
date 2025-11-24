package com.example.demo.domain.entity;

import com.example.demo.domain.enums.PenaltyType;
import com.example.demo.domain.enums.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@DisplayName("UserPenalty 엔티티 테스트")
class UserPenaltyTest {

    @Test
    @DisplayName("피드 패널티 생성")
    void createFeedPenalty_Success() {
        // given
        User user = User.builder()
                .id(1L)
                .userId("user01")
                .email("user@test.com")
                .password("password")
                .realName("테스터")
                .role(Role.MEMBER)
                .build();

        // when
        UserPenalty penalty = UserPenalty.builder()
                .id(1L)
                .penaltyType(PenaltyType.FEED)
                .user(user)
                .build();

        // then
        assertThat(penalty.getPenaltyType()).isEqualTo(PenaltyType.FEED);
        assertThat(penalty.getUser()).isEqualTo(user);
        assertThat(penalty.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("댓글 패널티 생성")
    void createCommentPenalty_Success() {
        // given
        User user = User.builder()
                .id(1L)
                .userId("user01")
                .email("user@test.com")
                .password("password")
                .realName("테스터")
                .role(Role.MEMBER)
                .build();

        // when
        UserPenalty penalty = UserPenalty.builder()
                .id(2L)
                .penaltyType(PenaltyType.COMMENT)
                .user(user)
                .build();

        // then
        assertThat(penalty.getPenaltyType()).isEqualTo(PenaltyType.COMMENT);
    }

    @Test
    @DisplayName("패널티 갱신")
    void refresh_Success() throws InterruptedException {
        // given
        LocalDateTime beforeRefresh = LocalDateTime.now().minusSeconds(1);
        UserPenalty penalty = UserPenalty.builder()
                .id(1L)
                .penaltyType(PenaltyType.FEED)
                .createdAt(beforeRefresh)
                .build();

        // when
        Thread.sleep(10); // 시간차를 위해 잠시 대기
        penalty.refresh();

        // then
        assertThat(penalty.getCreatedAt()).isAfter(beforeRefresh);
    }

    @Test
    @DisplayName("기본값 확인")
    void create_DefaultValues() {
        // given & when
        UserPenalty penalty = UserPenalty.builder()
                .id(1L)
                .penaltyType(PenaltyType.FEED)
                .build();

        // then
        assertThat(penalty.getCreatedAt()).isNotNull();
    }
}
