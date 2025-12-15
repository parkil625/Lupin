package com.example.demo.repository;

import com.example.demo.config.QueryDslConfig;
import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.FeedReport;
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
class FeedReportRepositoryTest {

    @Autowired
    private FeedReportRepository feedReportRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FeedRepository feedRepository;

    private User reporter;
    private Feed feed;

    @BeforeEach
    void setUp() {
        reporter = User.builder()
                .userId("reporter@test.com")
                .password("password")
                .name("reporter")
                .role(Role.MEMBER)
                .build();
        userRepository.save(reporter);

        User writer = User.builder()
                .userId("writer@test.com")
                .password("password")
                .name("writer")
                .role(Role.MEMBER)
                .build();
        userRepository.save(writer);

        feed = Feed.builder()
                .writer(writer)
                .content("content")
                .activity("running")
                .points(10)
                .calories(100)
                .build();
        feedRepository.save(feed);

        FeedReport report = FeedReport.builder()
                .reporter(reporter)
                .feed(feed)
                .build();
        feedReportRepository.save(report);
    }

    @Test
    @DisplayName("신고자와 피드로 신고 조회")
    void findByReporterAndFeedTest() {
        // when
        Optional<FeedReport> foundReport = feedReportRepository.findByReporterAndFeed(reporter, feed);

        // then
        assertThat(foundReport).isPresent();
    }
}