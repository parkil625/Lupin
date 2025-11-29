package com.example.demo.service;

import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.FeedReport;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.Role;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.FeedReportRepository;
import com.example.demo.repository.FeedRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("FeedReportService 테스트")
class FeedReportServiceTest {

    @Mock
    private FeedReportRepository feedReportRepository;

    @Mock
    private FeedRepository feedRepository;

    @InjectMocks
    private FeedReportService feedReportService;

    private User reporter;
    private User writer;
    private Feed feed;

    @BeforeEach
    void setUp() {
        reporter = User.builder()
                .userId("reporter")
                .password("password")
                .name("신고자")
                .role(Role.MEMBER)
                .build();

        writer = User.builder()
                .userId("writer")
                .password("password")
                .name("작성자")
                .role(Role.MEMBER)
                .build();

        feed = Feed.builder()
                .writer(writer)
                .activity("running")
                .content("피드 내용")
                .build();
    }

    @Test
    @DisplayName("피드를 신고한다")
    void reportFeedTest() {
        // given
        Long feedId = 1L;
        given(feedRepository.findById(feedId)).willReturn(Optional.of(feed));
        given(feedReportRepository.existsByReporterAndFeed(reporter, feed)).willReturn(false);
        given(feedReportRepository.save(any(FeedReport.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        feedReportService.toggleReport(reporter, feedId);

        // then
        verify(feedReportRepository).save(any(FeedReport.class));
    }

    @Test
    @DisplayName("이미 신고한 피드를 다시 신고하면 신고가 취소된다")
    void cancelReportTest() {
        // given
        Long feedId = 1L;
        given(feedRepository.findById(feedId)).willReturn(Optional.of(feed));
        given(feedReportRepository.existsByReporterAndFeed(reporter, feed)).willReturn(true);

        // when
        feedReportService.toggleReport(reporter, feedId);

        // then
        verify(feedReportRepository).deleteByReporterAndFeed(reporter, feed);
        verify(feedReportRepository, never()).save(any(FeedReport.class));
    }

    @Test
    @DisplayName("존재하지 않는 피드를 신고하면 예외가 발생한다")
    void reportFeedNotFoundTest() {
        // given
        Long feedId = 999L;
        given(feedRepository.findById(feedId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> feedReportService.toggleReport(reporter, feedId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FEED_NOT_FOUND);
    }
}
