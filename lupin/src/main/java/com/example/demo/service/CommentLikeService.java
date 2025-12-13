package com.example.demo.service;

import com.example.demo.domain.entity.Comment;
import com.example.demo.domain.entity.CommentLike;
import com.example.demo.domain.entity.User;
import com.example.demo.event.NotificationEvent;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.CommentLikeRepository;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.NotificationRepository;
import com.example.demo.domain.enums.NotificationType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentLikeService {

    private final CommentLikeRepository commentLikeRepository;
    private final CommentRepository commentRepository;
    private final NotificationRepository notificationRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public CommentLike likeComment(User user, Long commentId) {
        // [최적화] ID 기반 존재 확인
        if (commentLikeRepository.existsByUserIdAndCommentId(user.getId(), commentId)) {
            throw new BusinessException(ErrorCode.ALREADY_LIKED);
        }

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));

        CommentLike commentLike = CommentLike.builder()
                .user(user)
                .comment(comment)
                .build();

        CommentLike savedCommentLike = commentLikeRepository.save(commentLike);

        // [최적화] 이벤트 발행 - 트랜잭션 커밋 후 비동기 알림 처리
        eventPublisher.publishEvent(NotificationEvent.commentLike(
                comment.getWriter().getId(),
                user.getId(),
                user.getName(),
                user.getAvatar(),
                commentId,
                comment.getContent()
        ));

        return savedCommentLike;
    }

    @Transactional
    public void unlikeComment(User user, Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));

        CommentLike commentLike = commentLikeRepository.findByUserIdAndCommentId(user.getId(), commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LIKE_NOT_FOUND));

        // refId = commentId (댓글 참조)
        notificationRepository.deleteByRefIdAndType(String.valueOf(commentId), NotificationType.COMMENT_LIKE);
        commentLikeRepository.delete(commentLike);
    }

    /**
     * 댓글 좋아요 ID로 댓글 ID 조회
     */
    public Optional<Long> getCommentIdByLikeId(Long commentLikeId) {
        return commentLikeRepository.findByIdWithComment(commentLikeId)
                .map(commentLike -> commentLike.getComment().getId());
    }
}
