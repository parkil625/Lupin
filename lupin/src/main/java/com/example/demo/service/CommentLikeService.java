package com.example.demo.service;

import com.example.demo.domain.entity.Comment;
import com.example.demo.domain.entity.CommentLike;
import com.example.demo.domain.entity.User;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.CommentLikeRepository;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentLikeService {

    private final CommentLikeRepository commentLikeRepository;
    private final CommentRepository commentRepository;
    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;

    @Transactional
    public CommentLike likeComment(User user, Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));

        if (commentLikeRepository.existsByUserIdAndCommentId(user.getId(), commentId)) {
            throw new BusinessException(ErrorCode.ALREADY_LIKED);
        }

        CommentLike commentLike = CommentLike.builder()
                .user(user)
                .comment(comment)
                .build();

        CommentLike savedCommentLike = commentLikeRepository.save(commentLike);
        notificationService.createCommentLikeNotification(comment.getWriter(), user, savedCommentLike.getId());

        return savedCommentLike;
    }

    @Transactional
    public void unlikeComment(User user, Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));

        CommentLike commentLike = commentLikeRepository.findByUserIdAndCommentId(user.getId(), commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LIKE_NOT_FOUND));

        notificationRepository.deleteByRefIdAndType(String.valueOf(commentLike.getId()), "COMMENT_LIKE");
        commentLikeRepository.delete(commentLike);
    }
}
