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
    void existsByUserIdAndFeedIdTest() {
        // given
        User user = createAndSaveUser("user1");
        User otherUser = createAndSaveUser("user2");
        Feed feed = createAndSaveFeed(user, "running");

        createAndSaveFeedLike(otherUser, feed);

        // when - userId만 사용하여 detached entity 문제 방지
        boolean exists = feedLikeRepository.existsByUserIdAndFeedId(otherUser.getId(), feed.getId());
        boolean notExists = feedLikeRepository.existsByUserIdAndFeedId(user.getId(), feed.getId());

        // then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("사용자의 피드 좋아요를 삭제한다")
    void deleteByUserIdAndFeedIdTest() {
        // given
        User user = createAndSaveUser("user1");
        Feed feed = createAndSaveFeed(user, "running");
        createAndSaveFeedLike(user, feed);

        // when - userId만 사용하여 detached entity 문제 방지
        feedLikeRepository.deleteByUserIdAndFeedId(user.getId(), feed.getId());

        // then
        boolean exists = feedLikeRepository.existsByUserIdAndFeedId(user.getId(), feed.getId());
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

        // then - userId만 사용하여 detached entity 문제 방지
        assertThat(feedLikeRepository.existsByUserIdAndFeedId(user1.getId(), feed.getId())).isFalse();
        assertThat(feedLikeRepository.existsByUserIdAndFeedId(user2.getId(), feed.getId())).isFalse();
        assertThat(feedLikeRepository.existsByUserIdAndFeedId(user3.getId(), feed.getId())).isFalse();
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
