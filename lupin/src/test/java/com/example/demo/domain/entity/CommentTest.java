package com.example.demo.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Comment 엔티티 테스트")
class CommentTest {

    @Test
    @DisplayName("댓글에 대댓글 추가")
    void addReply_Success() {
        // given
        Comment parent = Comment.builder()
                .id(1L)
                .content("부모 댓글")
                .build();

        Comment reply = Comment.builder()
                .id(2L)
                .content("대댓글")
                .build();

        // when
        parent.addReply(reply);

        // then
        assertThat(parent.getReplies()).hasSize(1);
        assertThat(parent.getReplies().get(0)).isEqualTo(reply);
        assertThat(reply.getParent()).isEqualTo(parent);
    }

    @Test
    @DisplayName("대댓글 여부 확인 - true")
    void isReply_True() {
        // given
        Comment parent = Comment.builder()
                .id(1L)
                .content("부모 댓글")
                .build();

        Comment reply = Comment.builder()
                .id(2L)
                .content("대댓글")
                .build();

        parent.addReply(reply);

        // when & then
        assertThat(reply.isReply()).isTrue();
    }

    @Test
    @DisplayName("대댓글 여부 확인 - false")
    void isReply_False() {
        // given
        Comment comment = Comment.builder()
                .id(1L)
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
                .content("원본 내용")
                .build();

        // when
        comment.updateContent("수정된 내용");

        // then
        assertThat(comment.getContent()).isEqualTo("수정된 내용");
    }

    @Test
    @DisplayName("댓글 생성 - 기본값 확인")
    void create_DefaultValues() {
        // given & when
        Comment comment = Comment.builder()
                .id(1L)
                .content("테스트")
                .build();

        // then
        assertThat(comment.getReplies()).isEmpty();
        assertThat(comment.getLikes()).isEmpty();
        assertThat(comment.getParent()).isNull();
    }
}
