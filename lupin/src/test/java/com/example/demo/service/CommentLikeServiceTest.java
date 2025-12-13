package com.example.demo.service;

import com.example.demo.domain.entity.Comment;
import com.example.demo.domain.entity.CommentLike;
import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.NotificationType;
import com.example.demo.domain.enums.Role;
import com.example.demo.event.NotificationEvent;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.CommentLikeRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommentLikeService 테스트")
class CommentLikeServiceTest {

    @Mock
    private CommentLikeRepository commentLikeRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private CommentLikeService commentLikeService;

    private User user;
    private User writer;
    private Feed feed;
    private Comment comment;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .userId("user")
                .password("password")
                .name("사용자")
                .role(Role.MEMBER)
                .build();
        ReflectionTestUtils.setField(user, "id", 10L);

        writer = User.builder()
                .userId("writer")
                .password("password")
                .name("작성자")
                .role(Role.MEMBER)
                .build();
        ReflectionTestUtils.setField(writer, "id", 20L);

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
    @DisplayName("댓글에 좋아요를 누른다")
    void likeCommentTest() {
        // given
        Long commentId = 1L;
        given(commentLikeRepository.existsByUserIdAndCommentId(user.getId(), commentId)).willReturn(false);
        given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));
        given(commentLikeRepository.save(any(CommentLike.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        CommentLike result = commentLikeService.likeComment(user, commentId);

        // then
        assertThat(result.getUser()).isEqualTo(user);
        assertThat(result.getComment()).isEqualTo(comment);
        verify(commentLikeRepository).save(any(CommentLike.class));
    }

    @Test
    @DisplayName("댓글에 좋아요를 누르면 댓글 작성자에게 알림 이벤트가 발행된다")
    void likeCommentCreatesNotificationTest() {
        // given
        Long commentId = 1L;
        given(commentLikeRepository.existsByUserIdAndCommentId(user.getId(), commentId)).willReturn(false);
        given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));
        given(commentLikeRepository.save(any(CommentLike.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        commentLikeService.likeComment(user, commentId);

        // then - 이벤트 발행 검증
        verify(eventPublisher).publishEvent(any(NotificationEvent.class));
    }

    @Test
    @DisplayName("이미 좋아요한 댓글에 다시 좋아요를 누르면 예외가 발생한다")
    void likeCommentAlreadyLikedTest() {
        // given
        Long commentId = 1L;
        given(commentLikeRepository.existsByUserIdAndCommentId(user.getId(), commentId)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> commentLikeService.likeComment(user, commentId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ALREADY_LIKED);
    }

    @Test
    @DisplayName("댓글 좋아요를 취소하면 관련 알림도 삭제된다")
    void unlikeCommentTest() {
        // given
        Long commentId = 1L;
        CommentLike commentLike = CommentLike.builder()
                .user(user)
                .comment(comment)
                .build();

        given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));
        given(commentLikeRepository.findByUserIdAndCommentId(user.getId(), commentId)).willReturn(Optional.of(commentLike));

        // when
        commentLikeService.unlikeComment(user, commentId);

        // then - refId는 commentId 사용
        verify(notificationRepository).deleteByRefIdAndType(String.valueOf(commentId), NotificationType.COMMENT_LIKE);
        verify(commentLikeRepository).delete(commentLike);
    }

    @Test
    @DisplayName("좋아요하지 않은 댓글의 좋아요를 취소하면 예외가 발생한다")
    void unlikeCommentNotLikedTest() {
        // given
        Long commentId = 1L;
        given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));
        given(commentLikeRepository.findByUserIdAndCommentId(user.getId(), commentId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> commentLikeService.unlikeComment(user, commentId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.LIKE_NOT_FOUND);
    }

    @Test
    @DisplayName("존재하지 않는 댓글에 좋아요를 누르면 예외가 발생한다")
    void likeCommentNotFoundTest() {
        // given
        Long commentId = 999L;
        given(commentLikeRepository.existsByUserIdAndCommentId(user.getId(), commentId)).willReturn(false);
        given(commentRepository.findById(commentId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> commentLikeService.likeComment(user, commentId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COMMENT_NOT_FOUND);
    }
}
