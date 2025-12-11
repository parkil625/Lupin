package com.example.demo.service;

import com.example.demo.domain.entity.Comment;
import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.Role;
import com.example.demo.event.NotificationEvent;
import com.example.demo.repository.CommentLikeRepository;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.FeedRepository;
import com.example.demo.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.context.ApplicationEventPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private CommentLikeRepository commentLikeRepository;

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
                .writer(writer)
                .activity("running")
                .content("오늘 달리기 완료!")
                .points(0L)
                .calories(0)
                .build();
        ReflectionTestUtils.setField(feed, "id", 1L);
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
        verify(commentLikeRepository).deleteByComment(existingComment);
        verify(commentRepository).delete(existingComment);
    }

    @Test
    @DisplayName("댓글 ID로 댓글을 조회한다")
    void getComment() {
        // given
        Long commentId = 1L;
        Comment comment = Comment.builder()
                .id(commentId)
                .writer(writer)
                .feed(feed)
                .content("테스트 댓글")
                .build();

        given(commentRepository.findByIdWithDetails(commentId)).willReturn(Optional.of(comment));

        // when
        Comment result = commentService.getComment(commentId);

        // then
        assertThat(result.getId()).isEqualTo(commentId);
        assertThat(result.getContent()).isEqualTo("테스트 댓글");
        verify(commentRepository).findByIdWithDetails(commentId);
    }

    @Test
    @DisplayName("존재하지 않는 댓글 ID로 조회 시 예외가 발생한다")
    void getCommentWithNonExistentIdThrowsException() {
        // given
        Long commentId = 999L;
        given(commentRepository.findByIdWithDetails(commentId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> commentService.getComment(commentId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COMMENT_NOT_FOUND);
    }

    @Test
    @DisplayName("피드의 댓글 목록을 최신순으로 조회한다")
    void getCommentsByFeedOrderByLatest() {
        // given
        Long feedId = 1L;
        Comment comment1 = Comment.builder().id(1L).writer(writer).feed(feed).content("첫번째").build();
        Comment comment2 = Comment.builder().id(2L).writer(writer).feed(feed).content("두번째").build();

        given(feedRepository.findById(feedId)).willReturn(Optional.of(feed));
        given(commentRepository.findByFeedAndParentIsNullOrderByIdDesc(feed)).willReturn(List.of(comment2, comment1));

        // when
        List<Comment> result = commentService.getCommentsByFeed(feedId);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getContent()).isEqualTo("두번째");
        verify(commentRepository).findByFeedAndParentIsNullOrderByIdDesc(feed);
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

    @Test
    @DisplayName("대댓글을 작성하면 부모 댓글 작성자에게 알림이 생성된다 (refId = parentId)")
    void createReplyCreatesNotificationTest() {
        // given
        User commentOwner = User.builder()
                .id(2L)
                .userId("commentOwner")
                .name("댓글주인")
                .role(Role.MEMBER)
                .build();

        User replier = User.builder()
                .id(3L)
                .userId("replier")
                .name("답글작성자")
                .role(Role.MEMBER)
                .build();

        Long feedId = 1L;
        Long parentId = 1L;
        String content = "대댓글입니다";

        Comment parentComment = Comment.builder()
                .id(parentId)
                .writer(commentOwner)
                .feed(feed)
                .content("부모 댓글")
                .build();

        Comment savedReply = Comment.builder()
                .id(100L)
                .writer(replier)
                .feed(feed)
                .parent(parentComment)
                .content(content)
                .build();

        given(feedRepository.findById(feedId)).willReturn(Optional.of(feed));
        given(commentRepository.findById(parentId)).willReturn(Optional.of(parentComment));
        given(commentRepository.save(any(Comment.class))).willReturn(savedReply);

        // when
        commentService.createReply(replier, feedId, parentId, content);

        // then - 이벤트 발행 확인 (비동기 알림 처리)
        verify(eventPublisher).publishEvent(any(NotificationEvent.class));
    }

    @Test
    @DisplayName("존재하지 않는 피드에 댓글 작성 시 예외가 발생한다")
    void createCommentWithNonExistentFeedThrowsException() {
        // given
        Long feedId = 999L;
        given(feedRepository.findById(feedId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> commentService.createComment(writer, feedId, "댓글"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FEED_NOT_FOUND);
    }

    @Test
    @DisplayName("존재하지 않는 댓글 수정 시 예외가 발생한다")
    void updateNonExistentCommentThrowsException() {
        // given
        Long commentId = 999L;
        given(commentRepository.findById(commentId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> commentService.updateComment(writer, commentId, "수정"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COMMENT_NOT_FOUND);
    }

    @Test
    @DisplayName("존재하지 않는 댓글 삭제 시 예외가 발생한다")
    void deleteNonExistentCommentThrowsException() {
        // given
        Long commentId = 999L;
        given(commentRepository.findById(commentId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> commentService.deleteComment(writer, commentId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COMMENT_NOT_FOUND);
    }

    @Test
    @DisplayName("존재하지 않는 피드의 댓글 목록 조회 시 예외가 발생한다")
    void getCommentsByNonExistentFeedThrowsException() {
        // given
        Long feedId = 999L;
        given(feedRepository.findById(feedId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> commentService.getCommentsByFeed(feedId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FEED_NOT_FOUND);
    }

    @Test
    @DisplayName("존재하지 않는 피드의 인기 댓글 조회 시 예외가 발생한다")
    void getPopularCommentsByNonExistentFeedThrowsException() {
        // given
        Long feedId = 999L;
        given(feedRepository.findById(feedId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> commentService.getCommentsByFeedOrderByPopular(feedId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FEED_NOT_FOUND);
    }

    @Test
    @DisplayName("존재하지 않는 피드에 대댓글 작성 시 예외가 발생한다")
    void createReplyWithNonExistentFeedThrowsException() {
        // given
        Long feedId = 999L;
        Long parentId = 1L;
        given(feedRepository.findById(feedId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> commentService.createReply(writer, feedId, parentId, "대댓글"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FEED_NOT_FOUND);
    }

    @Test
    @DisplayName("존재하지 않는 부모 댓글에 대댓글 작성 시 예외가 발생한다")
    void createReplyWithNonExistentParentThrowsException() {
        // given
        Long feedId = 1L;
        Long parentId = 999L;
        given(feedRepository.findById(feedId)).willReturn(Optional.of(feed));
        given(commentRepository.findById(parentId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> commentService.createReply(writer, feedId, parentId, "대댓글"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COMMENT_NOT_FOUND);
    }

    @Test
    @DisplayName("존재하지 않는 부모 댓글의 대댓글 조회 시 예외가 발생한다")
    void getRepliesWithNonExistentParentThrowsException() {
        // given
        Long parentId = 999L;
        given(commentRepository.findById(parentId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> commentService.getReplies(parentId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COMMENT_NOT_FOUND);
    }

    @Test
    @DisplayName("본인이 아닌 사용자가 댓글을 수정하면 예외가 발생한다")
    void updateCommentByNonOwnerThrowsExceptionTest() {
        // given
        Long commentId = 1L;
        User otherUser = User.builder()
                .id(99L)
                .userId("other")
                .name("다른사람")
                .role(Role.MEMBER)
                .build();

        Comment comment = Comment.builder()
                .id(commentId)
                .writer(writer)
                .feed(feed)
                .content("원래 댓글")
                .build();

        given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));

        // when & then
        assertThatThrownBy(() -> commentService.updateComment(otherUser, commentId, "수정 내용"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COMMENT_NOT_OWNER);
    }

    @Test
    @DisplayName("본인이 아닌 사용자가 댓글을 삭제하면 예외가 발생한다")
    void deleteCommentByNonOwnerThrowsExceptionTest() {
        // given
        Long commentId = 1L;
        User otherUser = User.builder()
                .id(99L)
                .userId("other")
                .name("다른사람")
                .role(Role.MEMBER)
                .build();

        Comment comment = Comment.builder()
                .id(commentId)
                .writer(writer)
                .feed(feed)
                .content("삭제할 댓글")
                .build();

        given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));

        // when & then
        assertThatThrownBy(() -> commentService.deleteComment(otherUser, commentId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COMMENT_NOT_OWNER);
    }

    @Test
    @DisplayName("댓글 삭제 시 관련 알림도 삭제된다")
    void deleteCommentDeletesRelatedNotificationsTest() {
        // given
        Long commentId = 1L;
        Comment comment = Comment.builder()
                .id(commentId)
                .writer(writer)
                .feed(feed)
                .content("삭제할 댓글")
                .build();
        // parent가 null이므로 부모 댓글 (대댓글 아님)

        given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));
        given(commentRepository.findByParentOrderByIdAsc(comment)).willReturn(List.of());

        // when
        commentService.deleteComment(writer, commentId);

        // then
        // COMMENT_LIKE 알림 삭제 (refId = commentId)
        verify(notificationRepository).deleteByRefIdAndType(String.valueOf(commentId), "COMMENT_LIKE");
        // 댓글 좋아요 삭제
        verify(commentLikeRepository).deleteByComment(comment);
        // 부모 댓글이므로 REPLY 알림도 삭제 (refId = 부모댓글ID = commentId)
        verify(notificationRepository).deleteByRefIdAndType(String.valueOf(commentId), "REPLY");
        verify(commentRepository).delete(comment);
    }
}
