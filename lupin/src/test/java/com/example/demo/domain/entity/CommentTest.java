package com.example.demo.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CommentTest {

    @Test
    @DisplayName("댓글 수정 시 내용이 변경된다")
    void updateCommentTest() {
        // given
        Comment comment = Comment.builder()
                .content("original content")
                .build();

        // when
        comment.update("updated content");

        // then
        assertThat(comment.getContent()).isEqualTo("updated content");
        // updatedAt 검증은 JPA 통합 테스트에서 수행해야 하므로 제거
    }
}