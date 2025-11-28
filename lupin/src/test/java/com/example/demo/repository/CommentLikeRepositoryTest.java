package com.example.demo.repository;

import com.example.demo.domain.entity.Comment;
import com.example.demo.domain.entity.CommentLike;
import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class CommentLikeRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private CommentLikeRepository commentLikeRepository;

    @Test
    @DisplayName("사용자가 댓글에 좋아요를 눌렀는지 확인한다")
    void existsByUserAndCommentTest() {
        // given
        User writer = createAndSaveUser("writer");
        User liker = createAndSaveUser("liker");
        User otherUser = createAndSaveUser("otherUser");
        Feed feed = createAndSaveFeed(writer, "running");
        Comment comment = createAndSaveComment(writer, feed, "댓글 내용");

        createAndSaveCommentLike(liker, comment);

        // when
        boolean exists = commentLikeRepository.existsByUserAndComment(liker, comment);
        boolean notExists = commentLikeRepository.existsByUserAndComment(otherUser, comment);

        // then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("사용자의 댓글 좋아요를 삭제한다")
    void deleteByUserAndCommentTest() {
        // given
        User writer = createAndSaveUser("writer");
        User liker = createAndSaveUser("liker");
        Feed feed = createAndSaveFeed(writer, "running");
        Comment comment = createAndSaveComment(writer, feed, "댓글 내용");

        createAndSaveCommentLike(liker, comment);

        // when
        commentLikeRepository.deleteByUserAndComment(liker, comment);

        // then
        boolean exists = commentLikeRepository.existsByUserAndComment(liker, comment);
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("댓글의 좋아요 수를 조회한다")
    void countByCommentTest() {
        // given
        User writer = createAndSaveUser("writer");
        User liker1 = createAndSaveUser("liker1");
        User liker2 = createAndSaveUser("liker2");
        User liker3 = createAndSaveUser("liker3");
        Feed feed = createAndSaveFeed(writer, "running");
        Comment comment = createAndSaveComment(writer, feed, "댓글 내용");

        createAndSaveCommentLike(liker1, comment);
        createAndSaveCommentLike(liker2, comment);
        createAndSaveCommentLike(liker3, comment);

        // when
        long count = commentLikeRepository.countByComment(comment);

        // then
        assertThat(count).isEqualTo(3);
    }

    @Test
    @DisplayName("댓글의 좋아요를 전체 삭제한다")
    void deleteByCommentTest() {
        // given
        User writer = createAndSaveUser("writer");
        User liker1 = createAndSaveUser("liker1");
        User liker2 = createAndSaveUser("liker2");
        Feed feed = createAndSaveFeed(writer, "running");
        Comment comment = createAndSaveComment(writer, feed, "댓글 내용");

        createAndSaveCommentLike(liker1, comment);
        createAndSaveCommentLike(liker2, comment);

        // when
        commentLikeRepository.deleteByComment(comment);

        // then
        assertThat(commentLikeRepository.countByComment(comment)).isZero();
    }

    private CommentLike createAndSaveCommentLike(User user, Comment comment) {
        CommentLike commentLike = CommentLike.builder()
                .user(user)
                .comment(comment)
                .build();
        return commentLikeRepository.save(commentLike);
    }
}
