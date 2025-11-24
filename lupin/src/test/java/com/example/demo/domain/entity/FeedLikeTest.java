package com.example.demo.domain.entity;

import com.example.demo.domain.enums.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("FeedLike 엔티티 테스트")
class FeedLikeTest {

    @Test
    @DisplayName("피드 좋아요 생성")
    void create_Success() {
        // given
        User user = User.builder()
                .id(1L)
                .userId("user01")
                .email("user@test.com")
                .password("password")
                .realName("테스터")
                .role(Role.MEMBER)
                .build();

        Feed feed = Feed.builder()
                .id(1L)
                .content("테스트 피드")
                .build();

        // when
        FeedLike feedLike = FeedLike.builder()
                .id(1L)
                .user(user)
                .feed(feed)
                .build();

        // then
        assertThat(feedLike.getId()).isEqualTo(1L);
        assertThat(feedLike.getUser()).isEqualTo(user);
        assertThat(feedLike.getFeed()).isEqualTo(feed);
    }
}
