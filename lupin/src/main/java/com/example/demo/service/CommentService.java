package com.example.demo.service;

import com.example.demo.domain.entity.Comment;
import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.User;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.CommentLikeRepository;
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

    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
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
        // refId = feedId (피드 참조)
        notificationService.createCommentNotification(feed.getWriter(), writer, feedId);

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

        // COMMENT_LIKE 알림 삭제 (refId = commentId)
        notificationRepository.deleteByRefIdAndType(String.valueOf(commentId), "COMMENT_LIKE");

        // 부모 댓글인 경우
        if (comment.getParent() == null) {
            // REPLY 알림 삭제 (refId = 부모 댓글 ID = 본인 ID)
            notificationRepository.deleteByRefIdAndType(String.valueOf(commentId), "REPLY");
            // 대댓글들의 COMMENT_LIKE 알림 삭제
            deleteRepliesNotifications(comment);
        }

        commentRepository.delete(comment);
    }

    /**
     * 부모 댓글 삭제 시 대댓글들의 COMMENT_LIKE 알림 삭제
     * COMMENT_LIKE 알림의 refId = commentId (대댓글 ID)
     */
    private void deleteRepliesNotifications(Comment parentComment) {
        List<Comment> replies = commentRepository.findByParentOrderByIdAsc(parentComment);
        if (replies.isEmpty()) {
            return;
        }

        // 대댓글 ID 수집 후 COMMENT_LIKE 알림 삭제 (refId = commentId)
        List<String> replyIds = replies.stream()
                .map(r -> String.valueOf(r.getId()))
                .toList();
        notificationRepository.deleteByRefIdInAndType(replyIds, "COMMENT_LIKE");
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
        // refId = parentId (부모 댓글 참조)
        notificationService.createReplyNotification(parent.getWriter(), writer, parentId);

        return savedReply;
    }

    public List<Comment> getReplies(Long parentId) {
        Comment parent = commentRepository.findById(parentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));

        return commentRepository.findByParentOrderByIdAsc(parent);
    }
}
