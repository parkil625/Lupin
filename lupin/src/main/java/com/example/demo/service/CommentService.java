package com.example.demo.service;

import com.example.demo.domain.entity.Comment;
import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.User;
import com.example.demo.event.NotificationEvent;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.CommentLikeRepository;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.FeedRepository;
import com.example.demo.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
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
    private final NotificationRepository notificationRepository;
    private final ApplicationEventPublisher eventPublisher;

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

        // 댓글 카운트 증가 (반정규화)
        feed.incrementCommentCount();

        // [최적화] 이벤트 발행 - 트랜잭션 커밋 후 비동기 알림 처리
        eventPublisher.publishEvent(NotificationEvent.comment(
                feed.getWriter().getId(),
                writer.getId(),
                writer.getName(),
                feedId
        ));

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

        // 댓글 카운트 감소 (반정규화)
        comment.getFeed().decrementCommentCount();

        // COMMENT_LIKE 알림 삭제 (refId = commentId)
        notificationRepository.deleteByRefIdAndType(String.valueOf(commentId), "COMMENT_LIKE");

        // 댓글 좋아요 삭제 (외래키 제약조건)
        commentLikeRepository.deleteByComment(comment);

        // 부모 댓글인 경우
        if (comment.getParent() == null) {
            // REPLY 알림 삭제 (refId = 부모 댓글 ID = 본인 ID)
            notificationRepository.deleteByRefIdAndType(String.valueOf(commentId), "REPLY");
            // 대댓글들의 좋아요 및 알림 삭제
            deleteRepliesData(comment);
        }

        commentRepository.delete(comment);
    }

    /**
     * 부모 댓글 삭제 시 대댓글들의 좋아요, 알림, 대댓글 자체 삭제
     */
    private void deleteRepliesData(Comment parentComment) {
        List<Comment> replies = commentRepository.findByParentOrderByIdAsc(parentComment);
        if (replies.isEmpty()) {
            return;
        }

        // 대댓글 ID 수집 후 COMMENT_LIKE 알림 삭제 (refId = commentId)
        List<String> replyIds = replies.stream()
                .map(r -> String.valueOf(r.getId()))
                .toList();
        notificationRepository.deleteByRefIdInAndType(replyIds, "COMMENT_LIKE");

        // 대댓글들의 좋아요 삭제 후 대댓글 삭제 (외래키 제약조건)
        for (Comment reply : replies) {
            commentLikeRepository.deleteByComment(reply);
            parentComment.getFeed().decrementCommentCount(); // 대댓글 카운트 감소
            commentRepository.delete(reply);
        }
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

        // 대댓글도 댓글 카운트 증가 (반정규화)
        feed.incrementCommentCount();

        // [최적화] 이벤트 발행 - 트랜잭션 커밋 후 비동기 알림 처리
        eventPublisher.publishEvent(NotificationEvent.reply(
                parent.getWriter().getId(),
                writer.getId(),
                writer.getName(),
                parentId
        ));

        return savedReply;
    }

    public List<Comment> getReplies(Long parentId) {
        Comment parent = commentRepository.findById(parentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));

        return commentRepository.findByParentOrderByIdAsc(parent);
    }
}
