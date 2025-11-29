package com.example.demo.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CommentTest {

    @Test
    @DisplayName("댓글 내용을 수정한다")
    void updateTest() {
        // given
        Comment comment = Comment.builder()
                .content("원래 내용")
                .build();

        // when
        comment.update("수정된 내용");

        // then
        assertThat(comment.getContent()).isEqualTo("수정된 내용");
    }

    @Test
    @DisplayName("댓글 수정 시 updatedAt이 갱신된다")
    void updateSetsUpdatedAtTest() {
        // given
        Comment comment = Comment.builder()
                .content("원래 내용")
                .build();

        // when
        comment.update("수정된 내용");

        // then
        assertThat(comment.getUpdatedAt()).isNotNull();
    }
}
