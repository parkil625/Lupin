package com.example.demo.repository;

import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.FeedReport;
import com.example.demo.domain.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class FeedReportRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private FeedReportRepository feedReportRepository;

    @Test
    @DisplayName("피드의 신고 수를 조회한다")
    void countByFeedTest() {
        // given
        User writer = createAndSaveUser("writer");
        User reporter1 = createAndSaveUser("reporter1");
        User reporter2 = createAndSaveUser("reporter2");
        User reporter3 = createAndSaveUser("reporter3");
        Feed feed = createAndSaveFeed(writer, "running");

        createAndSaveFeedReport(reporter1, feed);
        createAndSaveFeedReport(reporter2, feed);
        createAndSaveFeedReport(reporter3, feed);

        // when
        long count = feedReportRepository.countByFeed(feed);

        // then
        assertThat(count).isEqualTo(3);
    }

    @Test
    @DisplayName("사용자가 피드를 이미 신고했는지 확인한다")
    void existsByReporterAndFeedTest() {
        // given
        User writer = createAndSaveUser("writer");
        User reporter = createAndSaveUser("reporter");
        User otherUser = createAndSaveUser("otherUser");
        Feed feed = createAndSaveFeed(writer, "running");

        createAndSaveFeedReport(reporter, feed);

        // when
        boolean exists = feedReportRepository.existsByReporterAndFeed(reporter, feed);
        boolean notExists = feedReportRepository.existsByReporterAndFeed(otherUser, feed);

        // then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("피드의 신고를 전체 삭제한다")
    void deleteByFeedTest() {
        // given
        User writer = createAndSaveUser("writer");
        User reporter1 = createAndSaveUser("reporter1");
        User reporter2 = createAndSaveUser("reporter2");
        Feed feed = createAndSaveFeed(writer, "running");

        createAndSaveFeedReport(reporter1, feed);
        createAndSaveFeedReport(reporter2, feed);

        // when
        feedReportRepository.deleteByFeed(feed);

        // then
        assertThat(feedReportRepository.countByFeed(feed)).isZero();
    }

    private FeedReport createAndSaveFeedReport(User reporter, Feed feed) {
        FeedReport feedReport = FeedReport.builder()
                .reporter(reporter)
                .feed(feed)
                .build();
        return feedReportRepository.save(feedReport);
    }
}
