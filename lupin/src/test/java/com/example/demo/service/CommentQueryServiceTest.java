package com.example.demo.service;

import com.example.demo.domain.entity.Comment;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.Role;
import com.example.demo.dto.response.CommentResponse;
import com.example.demo.exception.BusinessException;
import com.example.demo.repository.CommentLikeRepository;
import com.example.demo.repository.CommentRepository;
import com.querydsl.jpa.impl.JPAQueryFactory;
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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.example.demo.domain.entity.Feed;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommentQueryService 테스트")
class CommentQueryServiceTest {

    @InjectMocks
    private CommentQueryService commentQueryService;

    @Mock
    private JPAQueryFactory queryFactory;
    @Mock
    private CommentRepository commentRepository;
    @Mock
    private CommentLikeRepository commentLikeRepository;

    private User user;
    private Comment comment;
    private Feed feed;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .userId("user01")
                .realName("테스트유저")
                .role(Role.MEMBER)
                .build();

        feed = Feed.builder()
                .id(1L)
                .content("테스트 피드")
                .build();
        feed.setWriter(user);

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
    @DisplayName("댓글 상세 조회")
    class GetCommentDetail {

        @Test
        @DisplayName("댓글 상세 조회 성공")
        void getCommentDetail_Success() {
            // given
            given(commentRepository.findById(1L)).willReturn(Optional.of(comment));

            // when
            CommentResponse result = commentQueryService.getCommentDetail(1L);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEqualTo("테스트 댓글");
        }

        @Test
        @DisplayName("존재하지 않는 댓글 조회 실패")
        void getCommentDetail_NotFound_ThrowsException() {
            // given
            given(commentRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> commentQueryService.getCommentDetail(999L))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("댓글 수 조회")
    class GetCommentCount {

        @Test
        @DisplayName("피드 댓글 수 조회 성공")
        void getCommentCountByFeedId_Success() {
            // given
            given(commentRepository.countByFeedId(1L)).willReturn(5L);

            // when
            Long result = commentQueryService.getCommentCountByFeedId(1L);

            // then
            assertThat(result).isEqualTo(5L);
        }
    }

    @Nested
    @DisplayName("답글 조회")
    class GetReplies {

        @Test
        @DisplayName("답글 조회 성공")
        void getRepliesByCommentId_Success() {
            // given
            Comment reply1 = Comment.builder().id(2L).content("답글1").writer(user).build();
            Comment reply2 = Comment.builder().id(3L).content("답글2").writer(user).build();
            reply1.setFeed(feed);
            reply2.setFeed(feed);
            ReflectionTestUtils.setField(reply1, "createdAt", LocalDateTime.now());
            ReflectionTestUtils.setField(reply2, "createdAt", LocalDateTime.now());
            ReflectionTestUtils.setField(reply1, "replies", new ArrayList<>());
            ReflectionTestUtils.setField(reply2, "replies", new ArrayList<>());

            given(commentRepository.findRepliesByParentId(1L)).willReturn(Arrays.asList(reply1, reply2));

            // when
            List<CommentResponse> result = commentQueryService.getRepliesByCommentId(1L);

            // then
            assertThat(result).hasSize(2);
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
            boolean result = commentQueryService.hasUserLikedComment(1L, 1L);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("좋아요 수 조회 성공")
        void getCommentLikeCount_Success() {
            // given
            given(commentLikeRepository.countByCommentId(1L)).willReturn(10L);

            // when
            Long result = commentQueryService.getCommentLikeCount(1L);

            // then
            assertThat(result).isEqualTo(10L);
        }
    }
}
