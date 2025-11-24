package com.example.demo.service;

import com.example.demo.domain.entity.*;
import com.example.demo.dto.request.CommentCreateRequest;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommentCommandService 테스트")
class CommentCommandServiceTest {

    @InjectMocks
    private CommentCommandService commentCommandService;

    @Mock
    private CommentRepository commentRepository;
    @Mock
    private CommentLikeRepository commentLikeRepository;
    @Mock
    private FeedRepository feedRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserPenaltyRepository userPenaltyRepository;
    @Mock
    private NotificationService notificationService;

    private User user;
    private User feedWriter;
    private Feed feed;
    private Comment comment;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .userId("user01")
                .realName("테스트유저")
                .monthlyLikes(0L)
                .build();

        feedWriter = User.builder()
                .id(2L)
                .userId("writer01")
                .realName("피드작성자")
                .build();

        feed = Feed.builder()
                .id(1L)
                .content("테스트 피드")
                .writer(feedWriter)
                .build();

        comment = Comment.builder()
                .id(1L)
                .content("테스트 댓글")
                .writer(user)
                .build();
        comment.setFeed(feed);
        ReflectionTestUtils.setField(comment, "createdAt", LocalDateTime.now());
    }

    @Nested
    @DisplayName("댓글 생성")
    class CreateComment {

        @Test
        @DisplayName("댓글 생성 성공")
        void createComment_Success() {
            // given
            CommentCreateRequest request = CommentCreateRequest.builder()
                    .content("새 댓글")
                    .build();

            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(feedRepository.findById(1L)).willReturn(Optional.of(feed));
            given(userPenaltyRepository.hasActivePenalty(eq(1L), eq("COMMENT"), any(LocalDateTime.class)))
                    .willReturn(false);
            given(commentRepository.save(any(Comment.class))).willAnswer(invocation -> {
                Comment saved = invocation.getArgument(0);
                return Comment.builder()
                        .id(10L)
                        .content(saved.getContent())
                        .writer(saved.getWriter())
                        .build();
            });

            // when
            Long commentId = commentCommandService.createComment(1L, 1L, request);

            // then
            assertThat(commentId).isEqualTo(10L);
            then(commentRepository).should().save(any(Comment.class));
            then(notificationService).should().createCommentNotification(eq(2L), eq(1L), eq(1L), eq(10L));
        }

        @Test
        @DisplayName("패널티 유저는 댓글 생성 실패")
        void createComment_WithPenalty_ThrowsException() {
            // given
            CommentCreateRequest request = CommentCreateRequest.builder()
                    .content("새 댓글")
                    .build();

            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(feedRepository.findById(1L)).willReturn(Optional.of(feed));
            given(userPenaltyRepository.hasActivePenalty(eq(1L), eq("COMMENT"), any(LocalDateTime.class)))
                    .willReturn(true);

            // when & then
            assertThatThrownBy(() -> commentCommandService.createComment(1L, 1L, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("신고로 인해 3일간 댓글 작성이 제한됩니다.");
        }

        @Test
        @DisplayName("답글 생성 성공")
        void createReply_Success() {
            // given
            Comment parentComment = Comment.builder()
                    .id(5L)
                    .content("부모 댓글")
                    .writer(feedWriter)
                    .build();
            parentComment.setFeed(feed);

            CommentCreateRequest request = CommentCreateRequest.builder()
                    .content("답글")
                    .parentId(5L)
                    .build();

            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(feedRepository.findById(1L)).willReturn(Optional.of(feed));
            given(userPenaltyRepository.hasActivePenalty(eq(1L), eq("COMMENT"), any(LocalDateTime.class)))
                    .willReturn(false);
            given(commentRepository.findById(5L)).willReturn(Optional.of(parentComment));
            given(commentRepository.save(any(Comment.class))).willAnswer(invocation -> {
                Comment saved = invocation.getArgument(0);
                return Comment.builder()
                        .id(11L)
                        .content(saved.getContent())
                        .writer(saved.getWriter())
                        .build();
            });

            // when
            Long commentId = commentCommandService.createComment(1L, 1L, request);

            // then
            assertThat(commentId).isEqualTo(11L);
            then(notificationService).should().createReplyNotification(eq(2L), eq(1L), eq(1L), eq(11L));
        }

        @Test
        @DisplayName("답글에 답글 달기 실패")
        void createReplyToReply_ThrowsException() {
            // given
            Comment parentComment = Comment.builder()
                    .id(5L)
                    .content("부모 댓글")
                    .writer(feedWriter)
                    .parent(comment) // 이미 답글인 댓글
                    .build();

            CommentCreateRequest request = CommentCreateRequest.builder()
                    .content("답글의 답글")
                    .parentId(5L)
                    .build();

            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(feedRepository.findById(1L)).willReturn(Optional.of(feed));
            given(userPenaltyRepository.hasActivePenalty(eq(1L), eq("COMMENT"), any(LocalDateTime.class)))
                    .willReturn(false);
            given(commentRepository.findById(5L)).willReturn(Optional.of(parentComment));

            // when & then
            assertThatThrownBy(() -> commentCommandService.createComment(1L, 1L, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("답글에는 답글을 달 수 없습니다.");
        }

        @Test
        @DisplayName("존재하지 않는 유저로 댓글 생성 실패")
        void createComment_UserNotFound_ThrowsException() {
            // given
            CommentCreateRequest request = CommentCreateRequest.builder()
                    .content("새 댓글")
                    .build();

            given(userRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> commentCommandService.createComment(1L, 999L, request))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("존재하지 않는 피드에 댓글 생성 실패")
        void createComment_FeedNotFound_ThrowsException() {
            // given
            CommentCreateRequest request = CommentCreateRequest.builder()
                    .content("새 댓글")
                    .build();

            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(feedRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> commentCommandService.createComment(999L, 1L, request))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("댓글 수정")
    class UpdateComment {

        @Test
        @DisplayName("댓글 수정 성공")
        void updateComment_Success() {
            // given
            given(commentRepository.findById(1L)).willReturn(Optional.of(comment));

            // when
            commentCommandService.updateComment(1L, 1L, "수정된 댓글");

            // then
            assertThat(comment.getContent()).isEqualTo("수정된 댓글");
        }

        @Test
        @DisplayName("다른 사용자가 댓글 수정 시 실패")
        void updateComment_NotOwner_ThrowsException() {
            // given
            given(commentRepository.findById(1L)).willReturn(Optional.of(comment));

            // when & then
            assertThatThrownBy(() -> commentCommandService.updateComment(1L, 999L, "수정된 댓글"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("권한이 없습니다");
        }

        @Test
        @DisplayName("빈 내용으로 수정 시 실패")
        void updateComment_EmptyContent_ThrowsException() {
            // given
            given(commentRepository.findById(1L)).willReturn(Optional.of(comment));

            // when & then
            assertThatThrownBy(() -> commentCommandService.updateComment(1L, 1L, ""))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("필수");
        }

        @Test
        @DisplayName("null 내용으로 수정 시 실패")
        void updateComment_NullContent_ThrowsException() {
            // given
            given(commentRepository.findById(1L)).willReturn(Optional.of(comment));

            // when & then
            assertThatThrownBy(() -> commentCommandService.updateComment(1L, 1L, null))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("필수");
        }
    }

    @Nested
    @DisplayName("댓글 삭제")
    class DeleteComment {

        @Test
        @DisplayName("댓글 삭제 성공")
        void deleteComment_Success() {
            // given
            given(commentRepository.findById(1L)).willReturn(Optional.of(comment));

            // when
            commentCommandService.deleteComment(1L, 1L);

            // then
            then(commentRepository).should().delete(comment);
        }

        @Test
        @DisplayName("다른 사용자가 삭제 시 실패")
        void deleteComment_NotOwner_ThrowsException() {
            // given
            given(commentRepository.findById(1L)).willReturn(Optional.of(comment));

            // when & then
            assertThatThrownBy(() -> commentCommandService.deleteComment(1L, 999L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("권한이 없습니다");
        }
    }

    @Nested
    @DisplayName("댓글 좋아요")
    class LikeComment {

        @Test
        @DisplayName("댓글 좋아요 성공")
        void likeComment_Success() {
            // given
            Comment otherComment = Comment.builder()
                    .id(2L)
                    .content("다른 사람 댓글")
                    .writer(feedWriter)
                    .build();
            otherComment.setFeed(feed);

            given(commentRepository.findById(2L)).willReturn(Optional.of(otherComment));
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(commentLikeRepository.existsByUserIdAndCommentId(1L, 2L)).willReturn(false);

            // when
            commentCommandService.likeComment(2L, 1L);

            // then
            then(commentLikeRepository).should().save(any(CommentLike.class));
            then(notificationService).should().createCommentLikeNotification(eq(2L), eq(1L), eq(1L), eq(2L));
        }

        @Test
        @DisplayName("자신의 댓글에 좋아요 실패")
        void likeComment_OwnComment_ThrowsException() {
            // given
            given(commentRepository.findById(1L)).willReturn(Optional.of(comment));
            given(userRepository.findById(1L)).willReturn(Optional.of(user));

            // when & then
            assertThatThrownBy(() -> commentCommandService.likeComment(1L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("자신의 댓글");
        }

        @Test
        @DisplayName("이미 좋아요 누른 경우 실패")
        void likeComment_AlreadyLiked_ThrowsException() {
            // given
            Comment otherComment = Comment.builder()
                    .id(2L)
                    .content("다른 사람 댓글")
                    .writer(feedWriter)
                    .build();

            given(commentRepository.findById(2L)).willReturn(Optional.of(otherComment));
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(commentLikeRepository.existsByUserIdAndCommentId(1L, 2L)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> commentCommandService.likeComment(2L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("이미 좋아요");
        }
    }

    @Nested
    @DisplayName("댓글 좋아요 취소")
    class UnlikeComment {

        @Test
        @DisplayName("좋아요 취소 성공")
        void unlikeComment_Success() {
            // given
            Comment otherComment = Comment.builder()
                    .id(2L)
                    .content("다른 사람 댓글")
                    .writer(feedWriter)
                    .build();

            CommentLike commentLike = CommentLike.builder()
                    .id(1L)
                    .user(user)
                    .comment(otherComment)
                    .build();

            given(commentRepository.findById(2L)).willReturn(Optional.of(otherComment));
            given(commentLikeRepository.findByUserIdAndCommentId(1L, 2L)).willReturn(Optional.of(commentLike));

            // when
            commentCommandService.unlikeComment(2L, 1L);

            // then
            then(commentLikeRepository).should().delete(commentLike);
        }

        @Test
        @DisplayName("좋아요 누르지 않은 경우 취소 실패")
        void unlikeComment_NotLiked_ThrowsException() {
            // given
            given(commentRepository.findById(2L)).willReturn(Optional.of(comment));
            given(commentLikeRepository.findByUserIdAndCommentId(1L, 2L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> commentCommandService.unlikeComment(2L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("좋아요를 누르지 않았습니다");
        }
    }
}
