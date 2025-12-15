package com.example.demo.repository;

import com.example.demo.config.QueryDslConfig;
import com.example.demo.domain.entity.Comment;
import com.example.demo.domain.entity.CommentReport;
import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(QueryDslConfig.class)
class CommentReportRepositoryTest {

    @Autowired
    private CommentReportRepository commentReportRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FeedRepository feedRepository;

    @Autowired
    private CommentRepository commentRepository;

    private User user;
    private Feed feed;
    private Comment comment;
    private CommentReport commentReport;

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

        comment = Comment.builder()
                .writer(user)
                .feed(feed)
                .content("test comment")
                .build();
        commentRepository.save(comment);

        commentReport = CommentReport.builder()
                .reporter(user)
                .comment(comment)
                .build();
        commentReportRepository.save(commentReport);
    }

    @Test
    @DisplayName("신고자와 댓글로 신고 조회")
    void findByReporterAndCommentTest() {
        // when
        Optional<CommentReport> foundReport = commentReportRepository.findByReporterAndComment(user, comment);

        // then
        assertThat(foundReport).isPresent();
        assertThat(foundReport.get().getId()).isEqualTo(commentReport.getId());
    }

    @Test
    @DisplayName("댓글 ID로 신고 삭제")
    void deleteByCommentIdTest() {
        // when
        commentReportRepository.deleteByCommentId(comment.getId());

        // then
        Optional<CommentReport> foundReport = commentReportRepository.findByReporterAndComment(user, comment);
        assertThat(foundReport).isEmpty();
    }

    @Test
    @DisplayName("댓글 ID 목록으로 신고 일괄 삭제")
    void deleteByCommentIdsTest() {
        // when
        commentReportRepository.deleteByCommentIds(List.of(comment.getId()));

        // then
        Optional<CommentReport> foundReport = commentReportRepository.findByReporterAndComment(user, comment);
        assertThat(foundReport).isEmpty();
    }

    @Test
    @DisplayName("피드 ID로 신고 삭제")
    void deleteByFeedIdTest() {
        // when
        commentReportRepository.deleteByFeedId(feed.getId());

        // then
        Optional<CommentReport> foundReport = commentReportRepository.findByReporterAndComment(user, comment);
        assertThat(foundReport).isEmpty();
    }
}