package com.example.demo.repository;

import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.FeedLike;
import com.example.demo.domain.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class FeedLikeRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private FeedLikeRepository feedLikeRepository;

    @Test
    @DisplayName("사용자가 피드에 좋아요를 눌렀는지 확인한다")
    void existsByUserAndFeedTest() {
        // given
        User user = createAndSaveUser("user1");
        User otherUser = createAndSaveUser("user2");
        Feed feed = createAndSaveFeed(user, "running");

        createAndSaveFeedLike(otherUser, feed);

        // when
        boolean exists = feedLikeRepository.existsByUserAndFeed(otherUser, feed);
        boolean notExists = feedLikeRepository.existsByUserAndFeed(user, feed);

        // then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("사용자의 피드 좋아요를 삭제한다")
    void deleteByUserAndFeedTest() {
        // given
        User user = createAndSaveUser("user1");
        Feed feed = createAndSaveFeed(user, "running");
        createAndSaveFeedLike(user, feed);

        // when
        feedLikeRepository.deleteByUserAndFeed(user, feed);

        // then
        boolean exists = feedLikeRepository.existsByUserAndFeed(user, feed);
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("피드의 좋아요를 전체 삭제한다")
    void deleteByFeedTest() {
        // given
        User user1 = createAndSaveUser("user1");
        User user2 = createAndSaveUser("user2");
        User user3 = createAndSaveUser("user3");
        Feed feed = createAndSaveFeed(user1, "running");

        createAndSaveFeedLike(user1, feed);
        createAndSaveFeedLike(user2, feed);
        createAndSaveFeedLike(user3, feed);

        // when
        feedLikeRepository.deleteByFeed(feed);

        // then
        assertThat(feedLikeRepository.existsByUserAndFeed(user1, feed)).isFalse();
        assertThat(feedLikeRepository.existsByUserAndFeed(user2, feed)).isFalse();
        assertThat(feedLikeRepository.existsByUserAndFeed(user3, feed)).isFalse();
    }

    @Test
    @DisplayName("피드의 좋아요 수를 조회한다")
    void countByFeedTest() {
        // given
        User user1 = createAndSaveUser("user1");
        User user2 = createAndSaveUser("user2");
        User user3 = createAndSaveUser("user3");
        Feed feed = createAndSaveFeed(user1, "running");

        createAndSaveFeedLike(user1, feed);
        createAndSaveFeedLike(user2, feed);
        createAndSaveFeedLike(user3, feed);

        // when
        long count = feedLikeRepository.countByFeed(feed);

        // then
        assertThat(count).isEqualTo(3);
    }

    private FeedLike createAndSaveFeedLike(User user, Feed feed) {
        FeedLike feedLike = FeedLike.builder()
                .user(user)
                .feed(feed)
                .build();
        return feedLikeRepository.save(feedLike);
    }
}
