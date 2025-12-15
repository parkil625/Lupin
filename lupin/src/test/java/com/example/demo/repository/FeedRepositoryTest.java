package com.example.demo.repository;

import com.example.demo.config.QueryDslConfig;
import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(QueryDslConfig.class)
class FeedRepositoryTest {

    @Autowired
    private FeedRepository feedRepository;

    @Autowired
    private UserRepository userRepository;

    private User user;
    private Feed feed;

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
    }

    @Test
    @DisplayName("작성자별 피드 조회")
    void findAllByWriterTest() {
        // when
        Slice<Feed> feeds = feedRepository.findAllByWriter(user.getId(), PageRequest.of(0, 10));

        // then
        assertThat(feeds.getContent()).hasSize(1);
        assertThat(feeds.getContent().get(0).getWriter().getId()).isEqualTo(user.getId());
    }

    @Test
    @DisplayName("작성자 제외 피드 조회")
    void findAllExceptWriterTest() {
        // given
        User otherUser = User.builder()
                .userId("other@test.com")
                .password("password")
                .name("otherUser")
                .role(Role.MEMBER)
                .build();
        userRepository.save(otherUser);

        Feed otherFeed = Feed.builder()
                .writer(otherUser)
                .content("other feed")
                .activity("swimming")
                .points(20)
                .calories(200)
                .build();
        feedRepository.save(otherFeed);

        // when
        Slice<Feed> feeds = feedRepository.findAllExceptWriter(user.getId(), PageRequest.of(0, 10));

        // then
        assertThat(feeds.getContent()).hasSize(1);
        assertThat(feeds.getContent().get(0).getWriter().getId()).isEqualTo(otherUser.getId());
    }

    @Test
    @DisplayName("작성자 및 날짜로 피드 존재 여부 확인")
    void existsByWriterAndDateTest() {
        // given
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now().plusDays(1);

        // when
        boolean exists = feedRepository.existsByWriterAndDate(user.getId(), start, end);

        // then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("댓글 카운트 증가")
    void incrementCommentCountTest() {
        // when
        feedRepository.incrementCommentCount(feed.getId());

        // then
        Feed updatedFeed = feedRepository.findById(feed.getId()).orElseThrow();
        assertThat(updatedFeed.getCommentCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("댓글 카운트 감소")
    void decrementCommentCountTest() {
        // given
        feedRepository.incrementCommentCount(feed.getId());

        // when
        feedRepository.decrementCommentCount(feed.getId());

        // then
        Feed updatedFeed = feedRepository.findById(feed.getId()).orElseThrow();
        assertThat(updatedFeed.getCommentCount()).isEqualTo(0);
    }
}