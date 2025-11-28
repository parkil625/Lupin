package com.example.demo.repository;

import com.example.demo.domain.entity.Comment;
import com.example.demo.domain.entity.CommentReport;
import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class CommentReportRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private CommentReportRepository commentReportRepository;

    @Test
    @DisplayName("댓글의 신고 수를 조회한다")
    void countByCommentTest() {
        // given
        User writer = createAndSaveUser("writer");
        User reporter1 = createAndSaveUser("reporter1");
        User reporter2 = createAndSaveUser("reporter2");
        Feed feed = createAndSaveFeed(writer, "running");
        Comment comment = createAndSaveComment(writer, feed, "댓글 내용");

        createAndSaveCommentReport(reporter1, comment);
        createAndSaveCommentReport(reporter2, comment);

        // when
        long count = commentReportRepository.countByComment(comment);

        // then
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("사용자가 댓글을 이미 신고했는지 확인한다")
    void existsByReporterAndCommentTest() {
        // given
        User writer = createAndSaveUser("writer");
        User reporter = createAndSaveUser("reporter");
        User otherUser = createAndSaveUser("otherUser");
        Feed feed = createAndSaveFeed(writer, "running");
        Comment comment = createAndSaveComment(writer, feed, "댓글 내용");

        createAndSaveCommentReport(reporter, comment);

        // when
        boolean exists = commentReportRepository.existsByReporterAndComment(reporter, comment);
        boolean notExists = commentReportRepository.existsByReporterAndComment(otherUser, comment);

        // then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("댓글의 신고를 전체 삭제한다")
    void deleteByCommentTest() {
        // given
        User writer = createAndSaveUser("writer");
        User reporter1 = createAndSaveUser("reporter1");
        User reporter2 = createAndSaveUser("reporter2");
        Feed feed = createAndSaveFeed(writer, "running");
        Comment comment = createAndSaveComment(writer, feed, "댓글 내용");

        createAndSaveCommentReport(reporter1, comment);
        createAndSaveCommentReport(reporter2, comment);

        // when
        commentReportRepository.deleteByComment(comment);

        // then
        assertThat(commentReportRepository.countByComment(comment)).isZero();
    }

    private CommentReport createAndSaveCommentReport(User reporter, Comment comment) {
        CommentReport commentReport = CommentReport.builder()
                .reporter(reporter)
                .comment(comment)
                .build();
        return commentReportRepository.save(commentReport);
    }
}
