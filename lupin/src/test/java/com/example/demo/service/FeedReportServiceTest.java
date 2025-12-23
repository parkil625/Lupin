package com.example.demo.service;

import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.FeedReport;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.PenaltyType;
import com.example.demo.domain.enums.Role;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.FeedImageRepository;
import com.example.demo.repository.FeedLikeRepository;
import com.example.demo.repository.FeedReportRepository;
import com.example.demo.repository.FeedRepository;
import com.example.demo.repository.NotificationRepository;
import com.example.demo.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

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
    private UserRepository userRepository;

    @Mock
    private FeedRepository feedRepository;

    @Mock
    private FeedLikeRepository feedLikeRepository;

    @Mock
    private FeedImageRepository feedImageRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private UserPenaltyService userPenaltyService;

    @Mock
    private EntityManager entityManager;

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
        ReflectionTestUtils.setField(reporter, "id", 100L);

        writer = User.builder()
                .userId("writer")
                .password("password")
                .name("작성자")
                .role(Role.MEMBER)
                .build();
        ReflectionTestUtils.setField(writer, "id", 200L);

        feed = Feed.builder()
                .writer(writer)
                .activity("running")
                .content("피드 내용")
                .build();
        ReflectionTestUtils.setField(feed, "id", 1L);

        // 기본적으로 유저는 찾을 수 있다고 가정
        given(userRepository.findById(any())).willReturn(Optional.of(reporter));
    }

    @Test
    @DisplayName("피드를 신고한다")
    void reportFeedTest() {
        // given
        Long feedId = 1L;
        // [수정] existsById와 getReferenceById를 Mocking
        given(feedRepository.existsById(feedId)).willReturn(true);
        given(feedRepository.getReferenceById(feedId)).willReturn(feed);
        
        given(feedReportRepository.existsByReporterAndFeed(reporter, feed)).willReturn(false);
        given(feedReportRepository.saveAndFlush(any(FeedReport.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        feedReportService.toggleReport(reporter, feedId);

        // then
        verify(feedReportRepository).saveAndFlush(any(FeedReport.class));
    }

    @Test
    @DisplayName("이미 신고한 피드를 다시 신고하면 신고가 취소된다")
    void cancelReportTest() {
        // given
        Long feedId = 1L;
        // [수정] existsById와 getReferenceById를 Mocking
        given(feedRepository.existsById(feedId)).willReturn(true);
        given(feedRepository.getReferenceById(feedId)).willReturn(feed);
        
        given(feedReportRepository.existsByReporterAndFeed(reporter, feed)).willReturn(true);

        // when
        feedReportService.toggleReport(reporter, feedId);

        // then
        verify(feedReportRepository).deleteByReporterAndFeed(reporter, feed);
        verify(feedReportRepository, never()).save(any(FeedReport.class));
        verify(feedReportRepository, never()).saveAndFlush(any(FeedReport.class));
    }

    @Test
    @DisplayName("존재하지 않는 피드를 신고하면 예외가 발생한다")
    void reportFeedNotFoundTest() {
        // given
        Long feedId = 999L;
        // [수정] 존재하지 않음을 명시 (findById 대신 existsById 사용)
        given(feedRepository.existsById(feedId)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> feedReportService.toggleReport(reporter, feedId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FEED_NOT_FOUND);
    }

    @Test
    @DisplayName("신고 수가 임계값에 도달하면 피드 작성자에게 패널티가 부여된다")
    void reportFeedAppliesPenaltyWhenThresholdReachedTest() {
        // given
        Long feedId = 1L;
        long likeCount = 1L;
        long reportCount = 5L;

        given(feedRepository.existsById(feedId)).willReturn(true);
        given(feedRepository.getReferenceById(feedId)).willReturn(feed);
        
        given(feedReportRepository.existsByReporterAndFeed(reporter, feed)).willReturn(false);
        given(feedReportRepository.saveAndFlush(any(FeedReport.class))).willAnswer(invocation -> invocation.getArgument(0));
        
        given(feedLikeRepository.countByFeed(feed)).willReturn(likeCount);
        given(feedReportRepository.countByFeed(feed)).willReturn(reportCount);
        given(userPenaltyService.shouldApplyPenalty(likeCount, reportCount)).willReturn(true);
        given(userPenaltyService.hasActivePenalty(writer, PenaltyType.FEED)).willReturn(false);
        
        // 삭제 로직에서 findById가 호출됨
        given(feedRepository.findById(feedId)).willReturn(Optional.of(feed));

        // when
        feedReportService.toggleReport(reporter, feedId);

        // then
        verify(userPenaltyService).addPenalty(writer, PenaltyType.FEED);
    }

    @Test
    @DisplayName("신고 수가 임계값 미만이면 패널티가 부여되지 않는다")
    void reportFeedNoPenaltyWhenBelowThresholdTest() {
        // given
        Long feedId = 1L;
        long likeCount = 10L;
        long reportCount = 2L;

        given(feedRepository.existsById(feedId)).willReturn(true);
        given(feedRepository.getReferenceById(feedId)).willReturn(feed);
        
        given(feedReportRepository.existsByReporterAndFeed(reporter, feed)).willReturn(false);
        given(feedReportRepository.saveAndFlush(any(FeedReport.class))).willAnswer(invocation -> invocation.getArgument(0));
        
        given(feedLikeRepository.countByFeed(feed)).willReturn(likeCount);
        given(feedReportRepository.countByFeed(feed)).willReturn(reportCount);
        given(userPenaltyService.shouldApplyPenalty(likeCount, reportCount)).willReturn(false);

        // when
        feedReportService.toggleReport(reporter, feedId);

        // then
        verify(userPenaltyService, never()).addPenalty(any(User.class), any(PenaltyType.class));
    }

    @Test
    @DisplayName("이미 패널티가 활성화되어 있으면 중복 부여하지 않는다")
    void reportFeedNoDuplicatePenaltyTest() {
        // given
        Long feedId = 1L;
        long likeCount = 1L;
        long reportCount = 5L;

        given(feedRepository.existsById(feedId)).willReturn(true);
        given(feedRepository.getReferenceById(feedId)).willReturn(feed);
        
        given(feedReportRepository.existsByReporterAndFeed(reporter, feed)).willReturn(false);
        given(feedReportRepository.saveAndFlush(any(FeedReport.class))).willAnswer(invocation -> invocation.getArgument(0));
        
        given(feedLikeRepository.countByFeed(feed)).willReturn(likeCount);
        given(feedReportRepository.countByFeed(feed)).willReturn(reportCount);
        given(userPenaltyService.shouldApplyPenalty(likeCount, reportCount)).willReturn(true);
        given(userPenaltyService.hasActivePenalty(writer, PenaltyType.FEED)).willReturn(true);

        // when
        feedReportService.toggleReport(reporter, feedId);

        // then
        verify(userPenaltyService, never()).addPenalty(any(User.class), any(PenaltyType.class));
    }
}