package com.example.demo.service;

import com.example.demo.domain.entity.Comment;
import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.User;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.FeedRepository;
import com.example.demo.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private static final List<String> COMMENT_NOTIFICATION_TYPES = List.of("COMMENT_LIKE", "REPLY");

    private final CommentRepository commentRepository;
    private final FeedRepository feedRepository;
    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;

    @Transactional
    public Comment createComment(User writer, Long feedId, String content) {
        Feed feed = feedRepository.findById(feedId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FEED_NOT_FOUND));

        Comment comment = Comment.builder()
                .writer(writer)
                .feed(feed)
                .content(content)
                .build();

        Comment savedComment = commentRepository.save(comment);
        notificationService.createCommentNotification(feed.getWriter(), writer, savedComment.getId());

        return savedComment;
    }

    @Transactional
    public Comment updateComment(User user, Long commentId, String content) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));

        validateOwnership(comment, user);
        comment.update(content);
        return comment;
    }

    @Transactional
    public void deleteComment(User user, Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));

        validateOwnership(comment, user);
        notificationRepository.deleteByRefIdAndTypeIn(String.valueOf(commentId), COMMENT_NOTIFICATION_TYPES);
        commentRepository.delete(comment);
    }

    private void validateOwnership(Comment comment, User user) {
        if (!comment.getWriter().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.COMMENT_NOT_OWNER);
        }
    }

    public Comment getComment(Long commentId) {
        return commentRepository.findByIdWithDetails(commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));
    }

    public List<Comment> getCommentsByFeed(Long feedId) {
        Feed feed = feedRepository.findById(feedId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FEED_NOT_FOUND));

        return commentRepository.findByFeedAndParentIsNullOrderByIdDesc(feed);
    }

    public List<Comment> getCommentsByFeedOrderByPopular(Long feedId) {
        Feed feed = feedRepository.findById(feedId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FEED_NOT_FOUND));

        return commentRepository.findByFeedOrderByLikeCountDesc(feed);
    }

    @Transactional
    public Comment createReply(User writer, Long feedId, Long parentId, String content) {
        Feed feed = feedRepository.findById(feedId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FEED_NOT_FOUND));

        Comment parent = commentRepository.findById(parentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));

        Comment reply = Comment.builder()
                .writer(writer)
                .feed(feed)
                .parent(parent)
                .content(content)
                .build();

        Comment savedReply = commentRepository.save(reply);
        notificationService.createReplyNotification(parent.getWriter(), writer, savedReply.getId());

        return savedReply;
    }

    public List<Comment> getReplies(Long parentId) {
        Comment parent = commentRepository.findById(parentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));

        return commentRepository.findByParentOrderByIdAsc(parent);
    }
}
