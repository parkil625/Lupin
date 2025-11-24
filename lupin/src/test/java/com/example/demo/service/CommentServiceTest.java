package com.example.demo.service;

import com.example.demo.domain.entity.Comment;
import com.example.demo.domain.entity.CommentLike;
import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.PenaltyType;
import com.example.demo.domain.enums.Role;
import com.example.demo.dto.request.CommentCreateRequest;
import com.example.demo.dto.response.CommentResponse;
import com.example.demo.exception.BusinessException;
import com.example.demo.repository.CommentLikeRepository;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.FeedRepository;
import com.example.demo.repository.UserPenaltyRepository;
import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommentService 테스트")
class CommentServiceTest {

    @InjectMocks
    private CommentService commentService;

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
                .role(Role.MEMBER)
                .monthlyLikes(10L)
                .build();

        feedWriter = User.builder()
                .id(2L)
                .userId("writer01")
                .realName("피드작성자")
                .role(Role.MEMBER)
                .monthlyLikes(5L)
                .build();

        feed = Feed.builder()
                .id(1L)
                .content("테스트 피드")
                .build();
        feed.setWriter(feedWriter);

        comment = Comment.builder()
                .id(1L)
                .content("테스트 댓글")
                .writer(user)
                .build();
        comment.setFeed(feed);
        ReflectionTestUtils.setField(comment, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(comment, "replies", new ArrayList<>());
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
            given(userPenaltyRepository.hasActivePenalty(eq(1L), eq(PenaltyType.COMMENT), any(LocalDateTime.class)))
                    .willReturn(false);
            given(commentRepository.save(any(Comment.class))).willAnswer(invocation -> {
                Comment saved = invocation.getArgument(0);
                ReflectionTestUtils.setField(saved, "id", 10L);
                ReflectionTestUtils.setField(saved, "createdAt", LocalDateTime.now());
                ReflectionTestUtils.setField(saved, "replies", new ArrayList<>());
                return saved;
            });

            // when
            CommentResponse result = commentService.createComment(1L, 1L, request);

            // then
            assertThat(result).isNotNull();
            then(notificationService).should().createCommentNotification(eq(2L), eq(1L), eq(1L), eq(10L));
        }

        @Test
        @DisplayName("패널티 유저 댓글 생성 실패")
        void createComment_WithPenalty_ThrowsException() {
            // given
            CommentCreateRequest request = CommentCreateRequest.builder()
                    .content("새 댓글")
                    .build();

            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(feedRepository.findById(1L)).willReturn(Optional.of(feed));
            given(userPenaltyRepository.hasActivePenalty(eq(1L), eq(PenaltyType.COMMENT), any(LocalDateTime.class)))
                    .willReturn(true);

            // when & then
            assertThatThrownBy(() -> commentService.createComment(1L, 1L, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("신고");
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
            CommentResponse result = commentService.updateComment(1L, 1L, "수정된 내용");

            // then
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("다른 사용자 댓글 수정 실패")
        void updateComment_NotOwner_ThrowsException() {
            // given
            given(commentRepository.findById(1L)).willReturn(Optional.of(comment));

            // when & then
            assertThatThrownBy(() -> commentService.updateComment(1L, 999L, "수정된 내용"))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("빈 내용으로 수정 실패")
        void updateComment_EmptyContent_ThrowsException() {
            // given
            given(commentRepository.findById(1L)).willReturn(Optional.of(comment));

            // when & then
            assertThatThrownBy(() -> commentService.updateComment(1L, 1L, ""))
                    .isInstanceOf(BusinessException.class);
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
            commentService.deleteComment(1L, 1L);

            // then
            then(commentRepository).should().delete(comment);
        }

        @Test
        @DisplayName("다른 사용자 댓글 삭제 실패")
        void deleteComment_NotOwner_ThrowsException() {
            // given
            given(commentRepository.findById(1L)).willReturn(Optional.of(comment));

            // when & then
            assertThatThrownBy(() -> commentService.deleteComment(1L, 999L))
                    .isInstanceOf(BusinessException.class);
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
                    .content("다른 댓글")
                    .writer(feedWriter)
                    .build();
            otherComment.setFeed(feed);

            given(commentRepository.findById(2L)).willReturn(Optional.of(otherComment));
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(commentLikeRepository.existsByUserIdAndCommentId(1L, 2L)).willReturn(false);

            // when
            commentService.likeComment(2L, 1L);

            // then
            then(commentLikeRepository).should().save(any(CommentLike.class));
        }

        @Test
        @DisplayName("자신의 댓글 좋아요 실패")
        void likeComment_OwnComment_ThrowsException() {
            // given
            given(commentRepository.findById(1L)).willReturn(Optional.of(comment));
            given(userRepository.findById(1L)).willReturn(Optional.of(user));

            // when & then
            assertThatThrownBy(() -> commentService.likeComment(1L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("자신의 댓글");
        }

        @Test
        @DisplayName("이미 좋아요한 댓글 좋아요 실패")
        void likeComment_AlreadyLiked_ThrowsException() {
            // given
            Comment otherComment = Comment.builder()
                    .id(2L)
                    .content("다른 댓글")
                    .writer(feedWriter)
                    .build();

            given(commentRepository.findById(2L)).willReturn(Optional.of(otherComment));
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(commentLikeRepository.existsByUserIdAndCommentId(1L, 2L)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> commentService.likeComment(2L, 1L))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("댓글 좋아요 취소")
    class UnlikeComment {

        @Test
        @DisplayName("좋아요 취소 성공")
        void unlikeComment_Success() {
            // given
            CommentLike commentLike = CommentLike.builder()
                    .id(1L)
                    .user(user)
                    .comment(comment)
                    .build();

            given(commentRepository.findById(1L)).willReturn(Optional.of(comment));
            given(commentLikeRepository.findByUserIdAndCommentId(1L, 1L)).willReturn(Optional.of(commentLike));

            // when
            commentService.unlikeComment(1L, 1L);

            // then
            then(commentLikeRepository).should().delete(commentLike);
        }

        @Test
        @DisplayName("좋아요하지 않은 댓글 취소 실패")
        void unlikeComment_NotLiked_ThrowsException() {
            // given
            given(commentRepository.findById(1L)).willReturn(Optional.of(comment));
            given(commentLikeRepository.findByUserIdAndCommentId(1L, 1L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> commentService.unlikeComment(1L, 1L))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("좋아요 여부 확인")
    class HasUserLikedComment {

        @Test
        @DisplayName("좋아요 여부 확인 - 좋아요함")
        void hasUserLikedComment_True() {
            // given
            given(commentLikeRepository.existsByUserIdAndCommentId(1L, 1L)).willReturn(true);

            // when
            boolean result = commentService.hasUserLikedComment(1L, 1L);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("좋아요 여부 확인 - 좋아요 안함")
        void hasUserLikedComment_False() {
            // given
            given(commentLikeRepository.existsByUserIdAndCommentId(1L, 1L)).willReturn(false);

            // when
            boolean result = commentService.hasUserLikedComment(1L, 1L);

            // then
            assertThat(result).isFalse();
        }
    }
}
