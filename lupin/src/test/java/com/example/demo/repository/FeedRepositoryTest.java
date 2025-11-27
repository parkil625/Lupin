package com.example.demo.repository;

import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.Role;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class FeedRepositoryTest {

    @Autowired
    private FeedRepository feedRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("사용자의 피드를 ID 내림차순으로 조회한다")
    void findByWriterOrderByIdDescTest() {
        // given
        User me = createAndSaveUser("me");

        Feed feed = createAndSaveFeed(me, "running");

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
                .doesNotContain(me); // 내 피드 없음
        assertThat(feeds.getContent().get(0).getId())
                .isGreaterThan(feeds.getContent().get(1).getId()); // 내림차순
    }

    private User createAndSaveUser(String userId) {
        User user = User.builder()
                .userId(userId)
                .password("testPassword")
                .name("testName")
                .role(Role.MEMBER)
                .build();
        return userRepository.save(user);

    }

    private Feed createAndSaveFeed(User writer, String activity) {
        Feed feed = Feed.builder()
                .writer(writer)
                .activity(activity)
                .content("testContent")
                .build();
        return feedRepository.save(feed);
    }
}