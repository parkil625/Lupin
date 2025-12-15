package com.example.demo.service;

import com.example.demo.domain.entity.*;
import com.example.demo.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @InjectMocks
    private ReportService reportService;

    @Mock
    private FeedRepository feedRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private FeedReportRepository feedReportRepository;

    @Mock
    private CommentReportRepository commentReportRepository;

    @Mock
    private FeedLikeRepository feedLikeRepository;

    @Mock
    private CommentLikeRepository commentLikeRepository;

    @Mock
    private UserPenaltyService userPenaltyService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private FeedDeleteFacade feedDeleteFacade;

    @Mock
    private CommentDeleteFacade commentDeleteFacade;

    @Mock
    private User user;

    @Mock
    private Feed feed;

    @Mock
    private Comment comment;

    @Test
    @DisplayName("피드 신고 토글 - 신고 생성")
    void toggleFeedReportCreateTest() {
        // given
        Long feedId = 1L;
        // findByIdOrThrow는 내부적으로 findById를 호출하므로 findById를 stubbing
        given(feedRepository.findById(feedId)).willReturn(Optional.of(feed));
        given(feedReportRepository.findByReporterAndFeed(user, feed)).willReturn(Optional.empty());

        // when
        reportService.toggleFeedReport(user, feedId);

        // then
        verify(feedReportRepository).save(any(FeedReport.class));
    }

    @Test
    @DisplayName("피드 신고 토글 - 신고 취소")
    void toggleFeedReportDeleteTest() {
        // given
        Long feedId = 1L;
        FeedReport report = FeedReport.builder().reporter(user).feed(feed).build();
        // findByIdOrThrow는 내부적으로 findById를 호출하므로 findById를 stubbing
        given(feedRepository.findById(feedId)).willReturn(Optional.of(feed));
        given(feedReportRepository.findByReporterAndFeed(user, feed)).willReturn(Optional.of(report));

        // when
        reportService.toggleFeedReport(user, feedId);

        // then
        verify(feedReportRepository).delete(report);
    }

    @Test
    @DisplayName("댓글 신고 토글 - 신고 생성")
    void toggleCommentReportCreateTest() {
        // given
        Long commentId = 1L;
        // findByIdOrThrow는 내부적으로 findById를 호출하므로 findById를 stubbing
        given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));
        given(commentReportRepository.findByReporterAndComment(user, comment)).willReturn(Optional.empty());

        // when
        reportService.toggleCommentReport(user, commentId);

        // then
        verify(commentReportRepository).save(any(CommentReport.class));
    }

    @Test
    @DisplayName("댓글 신고 토글 - 신고 취소")
    void toggleCommentReportDeleteTest() {
        // given
        Long commentId = 1L;
        CommentReport report = CommentReport.builder().reporter(user).comment(comment).build();
        // findByIdOrThrow는 내부적으로 findById를 호출하므로 findById를 stubbing
        given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));
        given(commentReportRepository.findByReporterAndComment(user, comment)).willReturn(Optional.of(report));

        // when
        reportService.toggleCommentReport(user, commentId);

        // then
        verify(commentReportRepository).delete(report);
    }
}