package com.example.demo.service;

import com.example.demo.domain.entity.Comment;
import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.User;
import com.example.demo.event.NotificationEvent;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.FeedRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @InjectMocks
    private CommentService commentService;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private FeedRepository feedRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private User user;

    @Mock
    private Feed feed;

    @Mock
    private Comment comment;

    @Test
    @DisplayName("댓글 생성 성공")
    void createCommentTest() {
        // given
        Long feedId = 1L;
        String content = "content";

        given(feedRepository.findById(feedId)).willReturn(Optional.of(feed));
        given(feed.getWriter()).willReturn(user);
        given(feed.getId()).willReturn(feedId);

        given(commentRepository.save(any(Comment.class))).willReturn(comment);
        given(comment.getId()).willReturn(1L);
        given(comment.getWriter()).willReturn(user);
        given(comment.getFeed()).willReturn(feed);
        given(comment.getContent()).willReturn(content);

        // when
        commentService.create(user, feedId, Optional.empty(), content);

        // then
        verify(commentRepository).save(any(Comment.class));
        verify(feedRepository).incrementCommentCount(feedId);
        verify(eventPublisher).publishEvent(any(NotificationEvent.class));
    }

    @Test
    @DisplayName("답글 생성 성공")
    void createReplyTest() {
        // given
        Long feedId = 1L;
        Long parentId = 2L;
        String content = "reply content";
        Comment parent = Comment.builder().id(parentId).writer(user).build();

        given(feedRepository.findById(feedId)).willReturn(Optional.of(feed));
        given(feed.getId()).willReturn(feedId); // ★ 이 부분이 누락되어 에러가 났었음
        given(commentRepository.findById(parentId)).willReturn(Optional.of(parent));

        given(commentRepository.save(any(Comment.class))).willReturn(comment);
        given(comment.getId()).willReturn(3L);
        given(comment.getWriter()).willReturn(user);
        given(comment.getParent()).willReturn(parent);
        given(comment.getContent()).willReturn(content);

        // when
        commentService.create(user, feedId, Optional.of(parentId), content);

        // then
        verify(commentRepository).save(any(Comment.class));
        verify(feedRepository).incrementCommentCount(feedId);
        verify(eventPublisher).publishEvent(any(NotificationEvent.class));
    }

    @Test
    @DisplayName("대댓글에 답글 생성 시 예외 발생")
    void createReplyOnReplyTest() {
        // given
        Long feedId = 1L;
        Long parentId = 2L;
        String content = "reply content";

        Comment grandParent = Comment.builder().id(3L).build();
        Comment parent = Comment.builder().id(parentId).parent(grandParent).build();

        given(feedRepository.findById(feedId)).willReturn(Optional.of(feed));
        given(commentRepository.findById(parentId)).willReturn(Optional.of(parent));

        // when & then
        assertThatThrownBy(() -> commentService.create(user, feedId, Optional.of(parentId), content))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REPLY_DEPTH_EXCEEDED);
    }

    @Test
    @DisplayName("댓글 수정 성공")
    void updateCommentTest() {
        // given
        Long commentId = 1L;
        String newContent = "new content";

        Comment comment = Comment.builder().id(commentId).writer(user).content("old").build();

        given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));
        given(user.getId()).willReturn(1L);

        // when
        commentService.updateComment(user, commentId, newContent);

        // then
        assertThat(comment.getContent()).isEqualTo(newContent);
    }
}