package com.example.demo.domain.entity;

import com.example.demo.domain.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FeedTest {

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .userId("testuser")
                .password("password")
                .name("테스트유저")
                .role(Role.MEMBER)
                .build();
    }

    @Test
    @DisplayName("피드 내용을 수정한다")
    void updateTest() {
        // given
        Feed feed = Feed.builder()
                .writer(testUser)
                .content("원래 내용")
                .activity("running")
                .points(0L)
                .calories(0)
                .build();

        // when
        feed.update("수정된 내용", "walking");

        // then
        assertThat(feed.getContent()).isEqualTo("수정된 내용");
        assertThat(feed.getActivity()).isEqualTo("walking");
    }

    @Test
    @DisplayName("피드 점수와 칼로리를 수정한다")
    void updateScoreTest() {
        // given
        Feed feed = Feed.builder()
                .writer(testUser)
                .content("내용")
                .activity("running")
                .points(0L)
                .calories(0)
                .build();

        // when
        feed.updateScore(100L, 200);

        // then
        assertThat(feed.getPoints()).isEqualTo(100L);
        assertThat(feed.getCalories()).isEqualTo(200);
    }
}
