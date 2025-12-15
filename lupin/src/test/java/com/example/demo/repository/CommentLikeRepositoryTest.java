package com.example.demo.repository;

import com.example.demo.config.QueryDslConfig;
import com.example.demo.domain.entity.Comment;
import com.example.demo.domain.entity.CommentLike;
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
class CommentLikeRepositoryTest {

    @Autowired
    private CommentLikeRepository commentLikeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FeedRepository feedRepository;

    @Autowired
    private CommentRepository commentRepository;

    private User user;
    private Feed feed;
    private Comment comment;
    private CommentLike commentLike;

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

        commentLike = CommentLike.builder()
                .user(user)
                .comment(comment)
                .build();
        commentLikeRepository.save(commentLike);
    }

    @Test
    @DisplayName("사용자 ID와 댓글 ID로 좋아요 존재 확인")
    void existsByUserIdAndCommentIdTest() {
        // when
        boolean exists = commentLikeRepository.existsByUserIdAndCommentId(user.getId(), comment.getId());

        // then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("사용자 ID와 댓글 ID로 좋아요 조회")
    void findByUserIdAndCommentIdTest() {
        // when
        Optional<CommentLike> foundLike = commentLikeRepository.findByUserIdAndCommentId(user.getId(), comment.getId());

        // then
        assertThat(foundLike).isPresent();
        assertThat(foundLike.get().getId()).isEqualTo(commentLike.getId());
    }

    @Test
    @DisplayName("댓글 ID로 좋아요 삭제")
    void deleteByCommentIdTest() {
        // when
        commentLikeRepository.deleteByCommentId(comment.getId());

        // then
        boolean exists = commentLikeRepository.existsByUserIdAndCommentId(user.getId(), comment.getId());
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("댓글 ID 목록으로 좋아요 일괄 삭제")
    void deleteByCommentIdsTest() {
        // when
        commentLikeRepository.deleteByCommentIds(List.of(comment.getId()));

        // then
        boolean exists = commentLikeRepository.existsByUserIdAndCommentId(user.getId(), comment.getId());
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("피드 ID로 좋아요 삭제")
    void deleteByFeedIdTest() {
        // when
        commentLikeRepository.deleteByFeedId(feed.getId());

        // then
        boolean exists = commentLikeRepository.existsByUserIdAndCommentId(user.getId(), comment.getId());
        assertThat(exists).isFalse();
    }
}