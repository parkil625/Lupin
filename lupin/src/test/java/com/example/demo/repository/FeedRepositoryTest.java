package com.example.demo.repository;

import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class FeedRepositoryTest {

    @Autowired
    private FeedRepository feedRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void 사용자의_피드를_ID_내림차순으로_조회한다() {
        // given
        User user = User.builder()
        .userId("testUser")
        .password("testPassword")
        .name("testName")
        .role(Role.MEMBER)
        .build();
        userRepository.save(user);

        Feed feed = Feed.builder()
        .writer(user)
        .activity("running")
        .content("testContent")
        .build();
        feedRepository.save(feed);

        // when
        Slice<Feed> feeds = feedRepository.findByWriterOrderByIdDesc(user, PageRequest.of(0, 15));

        // then
        assertThat(feeds.getContent()).hasSize(1);
        assertThat(feeds.hasNext()).isFalse();
    }
}