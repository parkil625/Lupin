package com.example.demo.repository;

import com.example.demo.domain.entity.Comment;
import com.example.demo.domain.entity.CommentLike;
import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CommentRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private CommentLikeRepository commentLikeRepository;

    @Test
    @DisplayName("피드의 댓글을 최신순으로 조회한다")
    void findByFeedOrderByIdDescTest() {
        // given
        User user = createAndSaveUser("user1");
        Feed feed = createAndSaveFeed(user, "running");

        createAndSaveComment(user, feed, "첫 번째 댓글");
        createAndSaveComment(user, feed, "두 번째 댓글");
        createAndSaveComment(user, feed, "세 번째 댓글");

        // when
        List<Comment> comments = commentRepository.findByFeedOrderByIdDesc(feed);

        // then
        assertThat(comments).hasSize(3);
        assertThat(comments.get(0).getContent()).isEqualTo("세 번째 댓글");
        assertThat(comments.get(1).getContent()).isEqualTo("두 번째 댓글");
        assertThat(comments.get(2).getContent()).isEqualTo("첫 번째 댓글");
    }

    @Test
    @DisplayName("피드의 댓글을 전체 삭제한다")
    void deleteByFeedTest() {
        // given
        User user1 = createAndSaveUser("user1");
        User user2 = createAndSaveUser("user2");
        Feed feed = createAndSaveFeed(user1, "running");

        Comment parent = createAndSaveComment(user1, feed, "부모 댓글");
        createAndSaveReply(user1, feed, parent, "대댓글");
        createAndSaveComment(user2, feed, "두 번째 댓글");

        // when - 대댓글 먼저 삭제 후 부모 댓글 삭제
        commentRepository.deleteRepliesByFeed(feed);
        commentRepository.deleteParentCommentsByFeed(feed);

        // then
        List<Comment> comments = commentRepository.findByFeedOrderByIdDesc(feed);
        assertThat(comments).isEmpty();
    }

    @Test
    @DisplayName("대댓글을 오래된 순으로 조회한다")
    void findByParentOrderByIdAscTest() {
        // given
        User user = createAndSaveUser("user1");
        Feed feed = createAndSaveFeed(user, "running");
        Comment parent = createAndSaveComment(user, feed, "부모 댓글");

        createAndSaveReply(user, feed, parent, "첫 번째 대댓글");
        createAndSaveReply(user, feed, parent, "두 번째 대댓글");
        createAndSaveReply(user, feed, parent, "세 번째 대댓글");

        // when
        List<Comment> replies = commentRepository.findByParentOrderByIdAsc(parent);

        // then
        assertThat(replies).hasSize(3);
        assertThat(replies.get(0).getContent()).isEqualTo("첫 번째 대댓글");
        assertThat(replies.get(1).getContent()).isEqualTo("두 번째 대댓글");
        assertThat(replies.get(2).getContent()).isEqualTo("세 번째 대댓글");
    }

    @Test
    @DisplayName("대댓글 개수를 조회한다")
    void countByParentTest() {
        // given
        User user = createAndSaveUser("user1");
        Feed feed = createAndSaveFeed(user, "running");
        Comment parent = createAndSaveComment(user, feed, "부모 댓글");

        createAndSaveReply(user, feed, parent, "첫 번째 대댓글");
        createAndSaveReply(user, feed, parent, "두 번째 대댓글");

        // when
        long count = commentRepository.countByParent(parent);

        // then
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("피드의 댓글을 인기순(좋아요 수)으로 조회한다")
    void findByFeedOrderByLikeCountDescTest() {
        // given
        User writer = createAndSaveUser("writer");
        User liker1 = createAndSaveUser("liker1");
        User liker2 = createAndSaveUser("liker2");
        User liker3 = createAndSaveUser("liker3");
        Feed feed = createAndSaveFeed(writer, "running");

        Comment comment1 = createAndSaveComment(writer, feed, "좋아요 1개");
        Comment comment2 = createAndSaveComment(writer, feed, "좋아요 3개");
        Comment comment3 = createAndSaveComment(writer, feed, "좋아요 2개");

        // comment1: 1개
        createAndSaveCommentLike(liker1, comment1);

        // comment2: 3개
        createAndSaveCommentLike(liker1, comment2);
        createAndSaveCommentLike(liker2, comment2);
        createAndSaveCommentLike(liker3, comment2);

        // comment3: 2개
        createAndSaveCommentLike(liker1, comment3);
        createAndSaveCommentLike(liker2, comment3);

        // when
        List<Comment> comments = commentRepository.findByFeedOrderByLikeCountDesc(feed);

        // then
        assertThat(comments).hasSize(3);
        assertThat(comments.get(0).getContent()).isEqualTo("좋아요 3개");
        assertThat(comments.get(1).getContent()).isEqualTo("좋아요 2개");
        assertThat(comments.get(2).getContent()).isEqualTo("좋아요 1개");
    }

    private Comment createAndSaveReply(User writer, Feed feed, Comment parent, String content) {
        Comment reply = Comment.builder()
                .writer(writer)
                .feed(feed)
                .parent(parent)
                .content(content)
                .build();
        return commentRepository.save(reply);
    }
}
