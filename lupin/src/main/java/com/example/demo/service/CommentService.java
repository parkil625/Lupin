package com.example.demo.service;

import com.example.demo.domain.entity.Comment;
import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.User;
import com.example.demo.event.NotificationEvent;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.FeedRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final FeedRepository feedRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Comment create(User writer, Long feedId, Optional<Long> parentIdOpt, String content) {
        Feed feed = findFeedById(feedId);
        Comment parent = parentIdOpt.map(this::findAndValidateParent).orElse(null);

        Comment comment = createAndSaveComment(writer, feed, parent, content);

        publishNotificationEvent(comment);

        return comment;
    }

    @Transactional
    public Comment updateComment(User user, Long commentId, String content) {
        Comment comment = findCommentById(commentId);
        comment.validateOwner(user);
        comment.update(content);
        return comment;
    }

    private Comment createAndSaveComment(User writer, Feed feed, Comment parent, String content) {
        Comment comment = Comment.builder()
                .writer(writer)
                .feed(feed)
                .parent(parent)
                .content(content)
                .build();
        Comment savedComment = commentRepository.save(comment);
        feedRepository.incrementCommentCount(feed.getId());
        return savedComment;
    }

    private void publishNotificationEvent(Comment comment) {
        User writer = comment.getWriter();
        Feed feed = comment.getFeed();
        Comment parent = comment.getParent();

        if (parent != null) {
            // 답글 알림
            eventPublisher.publishEvent(NotificationEvent.reply(
                    parent.getWriter().getId(), writer.getId(), writer.getName(), writer.getAvatar(),
                    parent.getId(), comment.getId(), comment.getContent()
            ));
        } else {
            // 댓글 알림
            eventPublisher.publishEvent(NotificationEvent.comment(
                    feed.getWriter().getId(), writer.getId(), writer.getName(), writer.getAvatar(),
                    feed.getId(), comment.getId(), comment.getContent()
            ));
        }
    }

    private Comment findAndValidateParent(Long parentId) {
        Comment parent = findCommentById(parentId);
        if (parent.getParent() != null) {
            throw new BusinessException(ErrorCode.REPLY_DEPTH_EXCEEDED);
        }
        return parent;
    }

    private Feed findFeedById(Long feedId) {
        return feedRepository.findById(feedId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FEED_NOT_FOUND));
    }

    private Comment findCommentById(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));
    }
}
