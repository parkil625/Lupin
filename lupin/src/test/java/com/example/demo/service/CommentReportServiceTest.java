package com.example.demo.service;

import com.example.demo.domain.entity.Comment;
import com.example.demo.domain.entity.CommentReport;
import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.PenaltyType;
import com.example.demo.domain.enums.Role;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.CommentLikeRepository;
import com.example.demo.repository.CommentReportRepository;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
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
@DisplayName("CommentReportService 테스트")
class CommentReportServiceTest {

    @Mock
    private CommentReportRepository commentReportRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private CommentLikeRepository commentLikeRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private UserPenaltyService userPenaltyService;

    @InjectMocks
    private CommentReportService commentReportService;

    private User reporter;
    private User writer;
    private Feed feed;
    private Comment comment;

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

        comment = Comment.builder()
                .writer(writer)
                .feed(feed)
                .content("댓글 내용")
                .build();
        ReflectionTestUtils.setField(comment, "id", 1L);
    }

    @Test
    @DisplayName("댓글을 신고한다")
    void reportCommentTest() {
        // given
        Long commentId = 1L;
        given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));
        given(commentReportRepository.existsByReporterAndComment(reporter, comment)).willReturn(false);
        given(commentReportRepository.save(any(CommentReport.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        commentReportService.toggleReport(reporter, commentId);

        // then
        verify(commentReportRepository).save(any(CommentReport.class));
    }

    @Test
    @DisplayName("이미 신고한 댓글을 다시 신고하면 신고가 취소된다")
    void cancelReportTest() {
        // given
        Long commentId = 1L;
        given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));
        given(commentReportRepository.existsByReporterAndComment(reporter, comment)).willReturn(true);

        // when
        commentReportService.toggleReport(reporter, commentId);

        // then
        verify(commentReportRepository).deleteByReporterAndComment(reporter, comment);
        verify(commentReportRepository, never()).save(any(CommentReport.class));
    }

    @Test
    @DisplayName("존재하지 않는 댓글을 신고하면 예외가 발생한다")
    void reportCommentNotFoundTest() {
        // given
        Long commentId = 999L;
        given(commentRepository.findById(commentId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> commentReportService.toggleReport(reporter, commentId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COMMENT_NOT_FOUND);
    }

    @Test
    @DisplayName("신고 수가 임계값에 도달하면 댓글 작성자에게 패널티가 부여된다")
    void reportCommentAppliesPenaltyWhenThresholdReachedTest() {
        // given
        Long commentId = 1L;
        long likeCount = 1L;
        long reportCount = 5L;

        given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));
        given(commentReportRepository.existsByReporterAndComment(reporter, comment)).willReturn(false);
        given(commentReportRepository.save(any(CommentReport.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(commentLikeRepository.countByComment(comment)).willReturn(likeCount);
        given(commentReportRepository.countByComment(comment)).willReturn(reportCount);
        given(userPenaltyService.shouldApplyPenalty(likeCount, reportCount)).willReturn(true);
        given(userPenaltyService.hasActivePenalty(writer, PenaltyType.COMMENT)).willReturn(false);

        // when
        commentReportService.toggleReport(reporter, commentId);

        // then
        verify(userPenaltyService).addPenalty(writer, PenaltyType.COMMENT);
    }

    @Test
    @DisplayName("신고 수가 임계값 미만이면 패널티가 부여되지 않는다")
    void reportCommentNoPenaltyWhenBelowThresholdTest() {
        // given
        Long commentId = 1L;
        long likeCount = 10L;
        long reportCount = 2L;

        given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));
        given(commentReportRepository.existsByReporterAndComment(reporter, comment)).willReturn(false);
        given(commentReportRepository.save(any(CommentReport.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(commentLikeRepository.countByComment(comment)).willReturn(likeCount);
        given(commentReportRepository.countByComment(comment)).willReturn(reportCount);
        given(userPenaltyService.shouldApplyPenalty(likeCount, reportCount)).willReturn(false);

        // when
        commentReportService.toggleReport(reporter, commentId);

        // then
        verify(userPenaltyService, never()).addPenalty(any(User.class), any(PenaltyType.class));
    }

    @Test
    @DisplayName("이미 패널티가 활성화되어 있으면 중복 부여하지 않는다")
    void reportCommentNoDuplicatePenaltyTest() {
        // given
        Long commentId = 1L;
        long likeCount = 1L;
        long reportCount = 5L;

        given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));
        given(commentReportRepository.existsByReporterAndComment(reporter, comment)).willReturn(false);
        given(commentReportRepository.save(any(CommentReport.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(commentLikeRepository.countByComment(comment)).willReturn(likeCount);
        given(commentReportRepository.countByComment(comment)).willReturn(reportCount);
        given(userPenaltyService.shouldApplyPenalty(likeCount, reportCount)).willReturn(true);
        given(userPenaltyService.hasActivePenalty(writer, PenaltyType.COMMENT)).willReturn(true);

        // when
        commentReportService.toggleReport(reporter, commentId);

        // then
        verify(userPenaltyService, never()).addPenalty(any(User.class), any(PenaltyType.class));
    }
}
