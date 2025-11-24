package com.example.demo.service;

import com.example.demo.domain.entity.Comment;
import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.Report;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.ReportTargetType;
import com.example.demo.domain.enums.Role;
import com.example.demo.exception.BusinessException;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.FeedRepository;
import com.example.demo.repository.ReportRepository;
import com.example.demo.repository.UserPenaltyRepository;
import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReportService 테스트")
class ReportServiceTest {

    @InjectMocks
    private ReportService reportService;

    @Mock
    private ReportRepository reportRepository;
    @Mock
    private FeedRepository feedRepository;
    @Mock
    private CommentRepository commentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserPenaltyRepository userPenaltyRepository;
    @Mock
    private FeedCommandService feedCommandService;
    @Mock
    private CommentService commentService;

    private User reporter;
    private User feedWriter;
    private Feed feed;
    private Comment comment;

    @BeforeEach
    void setUp() {
        reporter = User.builder()
                .id(1L)
                .userId("reporter01")
                .realName("신고자")
                .role(Role.MEMBER)
                .build();

        feedWriter = User.builder()
                .id(2L)
                .userId("writer01")
                .realName("작성자")
                .role(Role.MEMBER)
                .build();

        feed = Feed.builder()
                .id(1L)
                .content("신고 대상 피드")
                .build();
        feed.setWriter(feedWriter);
        ReflectionTestUtils.setField(feed, "createdAt", LocalDateTime.now());

        comment = Comment.builder()
                .id(1L)
                .content("신고 대상 댓글")
                .build();
        ReflectionTestUtils.setField(comment, "writer", feedWriter);
        ReflectionTestUtils.setField(comment, "feed", feed);
    }

    @Nested
    @DisplayName("피드 신고")
    class ReportFeed {

        @Test
        @DisplayName("피드 신고 성공")
        void reportFeed_Success() {
            // given
            given(feedRepository.findById(1L)).willReturn(Optional.of(feed));
            given(userRepository.findById(1L)).willReturn(Optional.of(reporter));
            given(reportRepository.findByReporterIdAndTargetTypeAndTargetId(1L, ReportTargetType.FEED, 1L))
                    .willReturn(Optional.empty());
            given(reportRepository.countByTargetTypeAndTargetId(ReportTargetType.FEED, 1L)).willReturn(0L);

            // when
            reportService.reportFeed(1L, 1L);

            // then
            then(reportRepository).should().save(any(Report.class));
        }

        @Test
        @DisplayName("자신의 피드 신고 실패")
        void reportFeed_OwnFeed_ThrowsException() {
            // given
            Feed myFeed = Feed.builder().id(1L).build();
            myFeed.setWriter(reporter);

            given(feedRepository.findById(1L)).willReturn(Optional.of(myFeed));
            given(userRepository.findById(1L)).willReturn(Optional.of(reporter));

            // when & then
            assertThatThrownBy(() -> reportService.reportFeed(1L, 1L))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("존재하지 않는 피드 신고 실패")
        void reportFeed_FeedNotFound_ThrowsException() {
            // given
            given(feedRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> reportService.reportFeed(999L, 1L))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("댓글 신고")
    class ReportComment {

        @Test
        @DisplayName("댓글 신고 성공")
        void reportComment_Success() {
            // given
            given(commentRepository.findById(1L)).willReturn(Optional.of(comment));
            given(userRepository.findById(1L)).willReturn(Optional.of(reporter));
            given(reportRepository.findByReporterIdAndTargetTypeAndTargetId(1L, ReportTargetType.COMMENT, 1L))
                    .willReturn(Optional.empty());
            given(reportRepository.countByTargetTypeAndTargetId(ReportTargetType.COMMENT, 1L)).willReturn(0L);

            // when
            reportService.reportComment(1L, 1L);

            // then
            then(reportRepository).should().save(any(Report.class));
        }

        @Test
        @DisplayName("자신의 댓글 신고 실패")
        void reportComment_OwnComment_ThrowsException() {
            // given
            Comment myComment = Comment.builder().id(1L).build();
            ReflectionTestUtils.setField(myComment, "writer", reporter);

            given(commentRepository.findById(1L)).willReturn(Optional.of(myComment));
            given(userRepository.findById(1L)).willReturn(Optional.of(reporter));

            // when & then
            assertThatThrownBy(() -> reportService.reportComment(1L, 1L))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("신고 수 조회")
    class GetReportCount {

        @Test
        @DisplayName("피드 신고 수 조회 성공")
        void getFeedReportCount_Success() {
            // given
            given(reportRepository.countByTargetTypeAndTargetId(ReportTargetType.FEED, 1L)).willReturn(3L);

            // when
            Long result = reportService.getFeedReportCount(1L);

            // then
            assertThat(result).isEqualTo(3L);
        }

        @Test
        @DisplayName("댓글 신고 수 조회 성공")
        void getCommentReportCount_Success() {
            // given
            given(reportRepository.countByTargetTypeAndTargetId(ReportTargetType.COMMENT, 1L)).willReturn(2L);

            // when
            Long result = reportService.getCommentReportCount(1L);

            // then
            assertThat(result).isEqualTo(2L);
        }
    }

    @Nested
    @DisplayName("신고 여부 확인")
    class HasUserReported {

        @Test
        @DisplayName("피드 신고 여부 확인 - 신고함")
        void hasUserReportedFeed_True() {
            // given
            Report report = Report.builder().id(1L).build();
            given(reportRepository.findByReporterIdAndTargetTypeAndTargetId(1L, ReportTargetType.FEED, 1L))
                    .willReturn(Optional.of(report));

            // when
            boolean result = reportService.hasUserReportedFeed(1L, 1L);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("피드 신고 여부 확인 - 신고 안함")
        void hasUserReportedFeed_False() {
            // given
            given(reportRepository.findByReporterIdAndTargetTypeAndTargetId(1L, ReportTargetType.FEED, 1L))
                    .willReturn(Optional.empty());

            // when
            boolean result = reportService.hasUserReportedFeed(1L, 1L);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("댓글 신고 여부 확인 - 신고함")
        void hasUserReportedComment_True() {
            // given
            Report report = Report.builder().id(1L).build();
            given(reportRepository.findByReporterIdAndTargetTypeAndTargetId(1L, ReportTargetType.COMMENT, 1L))
                    .willReturn(Optional.of(report));

            // when
            boolean result = reportService.hasUserReportedComment(1L, 1L);

            // then
            assertThat(result).isTrue();
        }
    }
}
