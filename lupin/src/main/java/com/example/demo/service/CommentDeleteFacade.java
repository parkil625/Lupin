package com.example.demo.service;

import com.example.demo.domain.entity.Comment;
import com.example.demo.domain.entity.User;
import com.example.demo.event.CommentDeletedEvent;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.FeedRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentDeleteFacade {

    private final CommentRepository commentRepository;
    private final FeedRepository feedRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void deleteComment(User user, Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));

        comment.validateOwner(user);

        long feedId = comment.getFeed().getId();
        long writerId = comment.getWriter().getId();
        boolean isParent = comment.getParent() == null;

        List<Long> replyIds = List.of();
        if (isParent) {
            replyIds = commentRepository.findByParentOrderByIdAsc(comment).stream()
                    .map(Comment::getId)
                    .collect(Collectors.toList());
        }

        // 댓글 및 대댓글 수만큼 피드 댓글 수 감소
        long decrementCount = 1 + replyIds.size();
        feedRepository.decrementCommentCountBy(feedId, (int) decrementCount);

        // Soft Delete (실제 삭제는 이벤트 리스너에서 처리)
        commentRepository.delete(comment);

        eventPublisher.publishEvent(CommentDeletedEvent.of(commentId, feedId, writerId, isParent, replyIds));
        log.info("Comment soft deleted, event published: commentId={}, userId={}", commentId, user.getId());
    }
}
