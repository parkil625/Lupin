package com.example.demo.service;

import com.example.demo.domain.entity.Comment;
import com.example.demo.domain.entity.CommentReport;
import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.Role;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.CommentReportRepository;
import com.example.demo.repository.CommentRepository;
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
@DisplayName("CommentReportService 테스트")
class CommentReportServiceTest {

    @Mock
    private CommentReportRepository commentReportRepository;

    @Mock
    private CommentRepository commentRepository;

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
}
