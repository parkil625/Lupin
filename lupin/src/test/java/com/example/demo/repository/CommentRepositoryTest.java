package com.example.demo.repository;

import com.example.demo.config.QueryDslConfig;
import com.example.demo.domain.entity.Comment;
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

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(QueryDslConfig.class)
class CommentRepositoryTest {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FeedRepository feedRepository;

    private User user;
    private Feed feed;

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
    }

    @Test
    @DisplayName("피드의 부모 댓글 목록 조회 (최신순)")
    void findParentCommentsByFeedTest() {
        // given
        Comment comment1 = Comment.builder()
                .writer(user)
                .feed(feed)
                .content("comment1")
                .build();
        Comment comment2 = Comment.builder()
                .writer(user)
                .feed(feed)
                .content("comment2")
                .build();
        commentRepository.save(comment1);
        commentRepository.save(comment2);

        // when
        List<Comment> comments = commentRepository.findParentCommentsByFeed(feed);

        // then
        assertThat(comments).hasSize(2);
        assertThat(comments.get(0).getContent()).isEqualTo("comment2"); // 최신순
        assertThat(comments.get(1).getContent()).isEqualTo("comment1");
    }

    @Test
    @DisplayName("대댓글 조회")
    void findByParentOrderByIdAscTest() {
        // given
        Comment parent = Comment.builder()
                .writer(user)
                .feed(feed)
                .content("parent")
                .build();
        commentRepository.save(parent);

        Comment reply1 = Comment.builder()
                .writer(user)
                .feed(feed)
                .parent(parent)
                .content("reply1")
                .build();
        Comment reply2 = Comment.builder()
                .writer(user)
                .feed(feed)
                .parent(parent)
                .content("reply2")
                .build();
        commentRepository.save(reply1);
        commentRepository.save(reply2);

        // when
        List<Comment> replies = commentRepository.findByParentOrderByIdAsc(parent);

        // then
        assertThat(replies).hasSize(2);
        assertThat(replies.get(0).getContent()).isEqualTo("reply1"); // 등록순
        assertThat(replies.get(1).getContent()).isEqualTo("reply2");
    }

    @Test
    @DisplayName("피드 삭제 시 대댓글 삭제")
    void deleteRepliesByFeedIdTest() {
        // given
        Comment parent = Comment.builder()
                .writer(user)
                .feed(feed)
                .content("parent")
                .build();
        commentRepository.save(parent);

        Comment reply = Comment.builder()
                .writer(user)
                .feed(feed)
                .parent(parent)
                .content("reply")
                .build();
        commentRepository.save(reply);

        // when
        commentRepository.deleteRepliesByFeedId(feed.getId());

        // then
        List<Comment> replies = commentRepository.findByParentOrderByIdAsc(parent);
        assertThat(replies).isEmpty();
    }

    @Test
    @DisplayName("피드 삭제 시 부모 댓글 삭제")
    void deleteParentCommentsByFeedIdTest() {
        // given
        Comment parent = Comment.builder()
                .writer(user)
                .feed(feed)
                .content("parent")
                .build();
        commentRepository.save(parent);

        // when
        commentRepository.deleteParentCommentsByFeedId(feed.getId());

        // then
        List<Comment> comments = commentRepository.findParentCommentsByFeed(feed);
        assertThat(comments).isEmpty();
    }
}