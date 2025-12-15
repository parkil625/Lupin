package com.example.demo.repository;

import com.example.demo.config.QueryDslConfig;
import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.FeedLike;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(QueryDslConfig.class)
class FeedLikeRepositoryTest {

    @Autowired
    private FeedLikeRepository feedLikeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FeedRepository feedRepository;

    private User user;
    private Feed feed;
    private FeedLike feedLike;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .userId("test@test.com")
                .password("password")
                .name("testUser")
                .role(Role.MEMBER)
                .build();
        userRepository.save(user);

        feed = Feed.builder()
                .writer(user)
                .content("test feed")
                .activity("running")
                .points(10)
                .calories(100)
                .build();
        feedRepository.save(feed);

        feedLike = FeedLike.builder()
                .user(user)
                .feed(feed)
                .build();
        feedLikeRepository.save(feedLike);
    }

    @Test
    @DisplayName("사용자 ID와 피드 ID로 좋아요 존재 확인")
    void existsByUserIdAndFeedIdTest() {
        // when
        boolean exists = feedLikeRepository.existsByUserIdAndFeedId(user.getId(), feed.getId());

        // then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("사용자 ID와 피드 ID로 좋아요 조회")
    void findByUserIdAndFeedIdTest() {
        // when
        Optional<FeedLike> foundLike = feedLikeRepository.findByUserIdAndFeedId(user.getId(), feed.getId());

        // then
        assertThat(foundLike).isPresent();
        assertThat(foundLike.get().getId()).isEqualTo(feedLike.getId());
    }

    @Test
    @DisplayName("피드 ID로 좋아요 삭제")
    void deleteByFeedIdTest() {
        // when
        feedLikeRepository.deleteByFeedId(feed.getId());

        // then
        boolean exists = feedLikeRepository.existsByUserIdAndFeedId(user.getId(), feed.getId());
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("피드별 좋아요 수 조회")
    void countByFeedTest() {
        // when
        long count = feedLikeRepository.countByFeed(feed);

        // then
        assertThat(count).isEqualTo(1);
    }
}