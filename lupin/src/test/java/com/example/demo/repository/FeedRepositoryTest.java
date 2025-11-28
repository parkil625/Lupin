package com.example.demo.repository;

import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;

import static org.assertj.core.api.Assertions.assertThat;

class FeedRepositoryTest extends BaseRepositoryTest {

    @Test
    @DisplayName("사용자의 피드를 ID 내림차순으로 조회한다")
    void findByWriterOrderByIdDescTest() {
        // given
        User me = createAndSaveUser("me");
        createAndSaveFeed(me, "running");

        // when
        Slice<Feed> feeds = feedRepository.findByWriterOrderByIdDesc(me, PageRequest.of(0, 15));

        // then
        assertThat(feeds.getContent()).hasSize(1);
        assertThat(feeds.hasNext()).isFalse();
    }

    @Test
    @DisplayName("타 사용자의 피드를 ID 내림차순으로 조회한다")
    void findByWriterNotOrderByIdDescTest() {
        // given
        User me = createAndSaveUser("me");
        User other1 = createAndSaveUser("other1");
        User other2 = createAndSaveUser("other2");

        createAndSaveFeed(other1, "running");
        createAndSaveFeed(other2, "running");

        // when
        Slice<Feed> feeds = feedRepository.findByWriterNotOrderByIdDesc(me, PageRequest.of(0, 5));

        // then
        assertThat(feeds.getContent()).hasSize(2);
        assertThat(feeds.getContent()).extracting("writer")
                .doesNotContain(me);
        assertThat(feeds.getContent().get(0).getId())
                .isGreaterThan(feeds.getContent().get(1).getId());
    }

    @Test
    @DisplayName("작성자 이름으로 피드를 검색한다")
    void findByWriterNameContainingOrderByIdDescTest() {
        // given
        User user1 = createAndSaveUser("user1", "김철수");
        User user2 = createAndSaveUser("user2", "김영희");
        User user3 = createAndSaveUser("user3", "박민수");

        createAndSaveFeed(user1, "running");
        createAndSaveFeed(user2, "walking");
        createAndSaveFeed(user3, "cycling");

        // when
        Slice<Feed> feeds = feedRepository.findByWriterNameContainingOrderByIdDesc(
                "김", PageRequest.of(0, 5)
        );

        // then
        assertThat(feeds.getContent()).hasSize(2);
        assertThat(feeds.getContent()).extracting("writer.name")
                .allMatch(name -> ((String) name).contains("김"));
    }
}
