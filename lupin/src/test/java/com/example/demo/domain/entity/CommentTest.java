package com.example.demo.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Comment 엔티티 테스트")
class CommentTest {

    @Test
    @DisplayName("댓글 생성 - 기본값 확인")
    void create_DefaultValues() {
        // given & when
        Comment comment = Comment.builder()
                .id(1L)
                .writerId(1L)
                .feedId(1L)
                .content("테스트")
                .build();

        // then
        assertThat(comment.getLikesCount()).isEqualTo(0);
        assertThat(comment.getRepliesCount()).isEqualTo(0);
        assertThat(comment.getParentId()).isNull();
    }

    @Test
    @DisplayName("대댓글 여부 확인 - true")
    void isReply_True() {
        // given
        Comment reply = Comment.builder()
                .id(2L)
                .writerId(1L)
                .feedId(1L)
                .parentId(1L)
                .content("대댓글")
                .build();

        // when & then
        assertThat(reply.isReply()).isTrue();
    }

    @Test
    @DisplayName("대댓글 여부 확인 - false")
    void isReply_False() {
        // given
        Comment comment = Comment.builder()
                .id(1L)
                .writerId(1L)
                .feedId(1L)
                .content("일반 댓글")
                .build();

        // when & then
        assertThat(comment.isReply()).isFalse();
    }

    @Test
    @DisplayName("댓글 내용 수정")
    void updateContent_Success() {
        // given
        Comment comment = Comment.builder()
                .id(1L)
                .writerId(1L)
                .feedId(1L)
                .content("원본 내용")
                .build();

        // when
        comment.updateContent("수정된 내용");

        // then
        assertThat(comment.getContent()).isEqualTo("수정된 내용");
    }

    @Test
    @DisplayName("좋아요 수 증가")
    void incrementLikesCount_Success() {
        // given
        Comment comment = Comment.builder()
                .id(1L)
                .writerId(1L)
                .feedId(1L)
                .content("테스트")
                .build();

        // when
        comment.incrementLikesCount();

        // then
        assertThat(comment.getLikesCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("좋아요 수 감소")
    void decrementLikesCount_Success() {
        // given
        Comment comment = Comment.builder()
                .id(1L)
                .writerId(1L)
                .feedId(1L)
                .content("테스트")
                .likesCount(5)
                .build();

        // when
        comment.decrementLikesCount();

        // then
        assertThat(comment.getLikesCount()).isEqualTo(4);
    }

    @Test
    @DisplayName("좋아요 수 감소 - 0 미만으로 내려가지 않음")
    void decrementLikesCount_NotBelowZero() {
        // given
        Comment comment = Comment.builder()
                .id(1L)
                .writerId(1L)
                .feedId(1L)
                .content("테스트")
                .likesCount(0)
                .build();

        // when
        comment.decrementLikesCount();

        // then
        assertThat(comment.getLikesCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("대댓글 수 증가")
    void incrementRepliesCount_Success() {
        // given
        Comment comment = Comment.builder()
                .id(1L)
                .writerId(1L)
                .feedId(1L)
                .content("테스트")
                .build();

        // when
        comment.incrementRepliesCount();

        // then
        assertThat(comment.getRepliesCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("대댓글 수 감소")
    void decrementRepliesCount_Success() {
        // given
        Comment comment = Comment.builder()
                .id(1L)
                .writerId(1L)
                .feedId(1L)
                .content("테스트")
                .repliesCount(3)
                .build();

        // when
        comment.decrementRepliesCount();

        // then
        assertThat(comment.getRepliesCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("대댓글 수 감소 - 0 미만으로 내려가지 않음")
    void decrementRepliesCount_NotBelowZero() {
        // given
        Comment comment = Comment.builder()
                .id(1L)
                .writerId(1L)
                .feedId(1L)
                .content("테스트")
                .repliesCount(0)
                .build();

        // when
        comment.decrementRepliesCount();

        // then
        assertThat(comment.getRepliesCount()).isEqualTo(0);
    }
}
