package com.example.demo.domain.entity;

import com.example.demo.domain.enums.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("CommentLike 엔티티 테스트")
class CommentLikeTest {

    @Test
    @DisplayName("댓글 좋아요 생성")
    void create_Success() {
        // given
        User user = User.builder()
                .id(1L)
                .userId("user01")
                .email("user@test.com")
                .password("password")
                .realName("테스터")
                .role(Role.MEMBER)
                .build();

        Comment comment = Comment.builder()
                .id(1L)
                .content("테스트 댓글")
                .build();

        // when
        CommentLike commentLike = CommentLike.builder()
                .id(1L)
                .user(user)
                .comment(comment)
                .build();

        // then
        assertThat(commentLike.getId()).isEqualTo(1L);
        assertThat(commentLike.getUser()).isEqualTo(user);
        assertThat(commentLike.getComment()).isEqualTo(comment);
    }
}
