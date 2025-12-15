package com.example.demo.repository;

import com.example.demo.config.QueryDslConfig;
import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.FeedImage;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.ImageType;
import com.example.demo.domain.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(QueryDslConfig.class)
class FeedImageRepositoryTest {

    @Autowired
    private FeedImageRepository feedImageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FeedRepository feedRepository;

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
    @DisplayName("모든 S3 키 조회")
    void findAllS3KeysTest() {
        // given
        FeedImage image1 = FeedImage.builder()
                .feed(feed)
                .s3Key("key1")
                .imgType(ImageType.START)
                .sortOrder(0)
                .build();
        FeedImage image2 = FeedImage.builder()
                .feed(feed)
                .s3Key("key2")
                .imgType(ImageType.END)
                .sortOrder(1)
                .build();
        feedImageRepository.save(image1);
        feedImageRepository.save(image2);

        // when
        List<String> s3Keys = feedImageRepository.findAllS3Keys();

        // then
        assertThat(s3Keys).hasSize(2);
        assertThat(s3Keys).contains("key1", "key2");
    }
}