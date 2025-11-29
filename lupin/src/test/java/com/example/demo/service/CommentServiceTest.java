package com.example.demo.service;

import com.example.demo.domain.entity.Comment;
import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.User;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.FeedRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommentService 테스트")
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private FeedRepository feedRepository;

    @InjectMocks
    private CommentService commentService;

    private User writer;
    private Feed feed;

    @BeforeEach
    void setUp() {
        writer = User.builder()
                .id(1L)
                .userId("user1")
                .name("테스트유저")
                .build();

        feed = Feed.builder()
                .id(1L)
                .writer(writer)
                .activity("running")
                .content("오늘 달리기 완료!")
                .build();
    }

    @Test
    @DisplayName("댓글을 작성한다")
    void createComment() {
        // given
        Long feedId = 1L;
        String content = "좋은 운동이네요!";

        Comment savedComment = Comment.builder()
                .id(1L)
                .writer(writer)
                .feed(feed)
                .content(content)
                .build();

        given(feedRepository.findById(feedId)).willReturn(Optional.of(feed));
        given(commentRepository.save(any(Comment.class))).willReturn(savedComment);

        // when
        Comment result = commentService.createComment(writer, feedId, content);

        // then
        assertThat(result.getContent()).isEqualTo(content);
        assertThat(result.getWriter()).isEqualTo(writer);
        assertThat(result.getFeed()).isEqualTo(feed);
        verify(feedRepository).findById(feedId);
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    @DisplayName("댓글을 수정한다")
    void updateComment() {
        // given
        Long commentId = 1L;
        String newContent = "수정된 댓글입니다";

        Comment existingComment = Comment.builder()
                .id(commentId)
                .writer(writer)
                .feed(feed)
                .content("원래 댓글")
                .build();

        given(commentRepository.findById(commentId)).willReturn(Optional.of(existingComment));

        // when
        Comment result = commentService.updateComment(writer, commentId, newContent);

        // then
        assertThat(result.getContent()).isEqualTo(newContent);
        verify(commentRepository).findById(commentId);
    }

    @Test
    @DisplayName("댓글을 삭제한다")
    void deleteComment() {
        // given
        Long commentId = 1L;

        Comment existingComment = Comment.builder()
                .id(commentId)
                .writer(writer)
                .feed(feed)
                .content("삭제할 댓글")
                .build();

        given(commentRepository.findById(commentId)).willReturn(Optional.of(existingComment));

        // when
        commentService.deleteComment(writer, commentId);

        // then
        verify(commentRepository).findById(commentId);
        verify(commentRepository).delete(existingComment);
    }

    @Test
    @DisplayName("피드의 댓글 목록을 최신순으로 조회한다")
    void getCommentsByFeedOrderByLatest() {
        // given
        Long feedId = 1L;
        Comment comment1 = Comment.builder().id(1L).writer(writer).feed(feed).content("첫번째").build();
        Comment comment2 = Comment.builder().id(2L).writer(writer).feed(feed).content("두번째").build();

        given(feedRepository.findById(feedId)).willReturn(Optional.of(feed));
        given(commentRepository.findByFeedOrderByIdDesc(feed)).willReturn(List.of(comment2, comment1));

        // when
        List<Comment> result = commentService.getCommentsByFeed(feedId);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getContent()).isEqualTo("두번째");
        verify(commentRepository).findByFeedOrderByIdDesc(feed);
    }

    @Test
    @DisplayName("피드의 댓글 목록을 인기순으로 조회한다")
    void getCommentsByFeedOrderByPopular() {
        // given
        Long feedId = 1L;
        Comment comment1 = Comment.builder().id(1L).writer(writer).feed(feed).content("좋아요 적음").build();
        Comment comment2 = Comment.builder().id(2L).writer(writer).feed(feed).content("좋아요 많음").build();

        given(feedRepository.findById(feedId)).willReturn(Optional.of(feed));
        given(commentRepository.findByFeedOrderByLikeCountDesc(feed)).willReturn(List.of(comment2, comment1));

        // when
        List<Comment> result = commentService.getCommentsByFeedOrderByPopular(feedId);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getContent()).isEqualTo("좋아요 많음");
        verify(commentRepository).findByFeedOrderByLikeCountDesc(feed);
    }

    @Test
    @DisplayName("대댓글을 작성한다")
    void createReply() {
        // given
        Long feedId = 1L;
        Long parentId = 1L;
        String content = "대댓글입니다";

        Comment parentComment = Comment.builder()
                .id(parentId)
                .writer(writer)
                .feed(feed)
                .content("부모 댓글")
                .build();

        Comment savedReply = Comment.builder()
                .id(2L)
                .writer(writer)
                .feed(feed)
                .parent(parentComment)
                .content(content)
                .build();

        given(feedRepository.findById(feedId)).willReturn(Optional.of(feed));
        given(commentRepository.findById(parentId)).willReturn(Optional.of(parentComment));
        given(commentRepository.save(any(Comment.class))).willReturn(savedReply);

        // when
        Comment result = commentService.createReply(writer, feedId, parentId, content);

        // then
        assertThat(result.getContent()).isEqualTo(content);
        assertThat(result.getParent()).isEqualTo(parentComment);
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    @DisplayName("대댓글 목록을 조회한다")
    void getReplies() {
        // given
        Long parentId = 1L;
        Comment parentComment = Comment.builder().id(parentId).writer(writer).feed(feed).content("부모").build();
        Comment reply1 = Comment.builder().id(2L).writer(writer).feed(feed).parent(parentComment).content("대댓글1").build();
        Comment reply2 = Comment.builder().id(3L).writer(writer).feed(feed).parent(parentComment).content("대댓글2").build();

        given(commentRepository.findById(parentId)).willReturn(Optional.of(parentComment));
        given(commentRepository.findByParentOrderByIdAsc(parentComment)).willReturn(List.of(reply1, reply2));

        // when
        List<Comment> result = commentService.getReplies(parentId);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getContent()).isEqualTo("대댓글1");
        verify(commentRepository).findByParentOrderByIdAsc(parentComment);
    }
}
