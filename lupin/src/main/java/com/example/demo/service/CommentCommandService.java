package com.example.demo.service;

import com.example.demo.domain.entity.Comment;
import com.example.demo.domain.entity.CommentLike;
import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.PenaltyType;
import com.example.demo.dto.request.CommentCreateRequest;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.CommentLikeRepository;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.FeedRepository;
import com.example.demo.repository.UserPenaltyRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 댓글 Command 서비스 (쓰기 전용)
 * CQRS 패턴 - 데이터 변경 작업 담당
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CommentCommandService {

    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final FeedRepository feedRepository;
    private final UserRepository userRepository;
    private final UserPenaltyRepository userPenaltyRepository;
    private final NotificationService notificationService;

    /**
     * 댓글 생성
     */
    public Long createComment(Long feedId, Long userId, CommentCreateRequest request) {
        User user = findUserById(userId);
        Feed feed = findFeedById(feedId);

        // 패널티 확인
        LocalDateTime threeDaysAgo = LocalDateTime.now().minusDays(3);
        if (userPenaltyRepository.hasActivePenalty(userId, PenaltyType.COMMENT, threeDaysAgo)) {
            throw new BusinessException(ErrorCode.PENALTY_ACTIVE, "신고로 인해 3일간 댓글 작성이 제한됩니다.");
        }

        Comment comment = Comment.builder()
                .content(request.getContent())
                .writer(user)
                .build();

        comment.setFeed(feed);

        // 답글인 경우 부모 댓글 설정
        if (request.getParentId() != null) {
            Comment parent = findCommentById(request.getParentId());
            if (parent.isReply()) {
                throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "답글에는 답글을 달 수 없습니다.");
            }
            parent.addReply(comment);
        }

        Comment savedComment = commentRepository.save(comment);

        // 알림 처리
        if (!feed.getWriter().getId().equals(userId)) {
            notificationService.createCommentNotification(
                feed.getWriter().getId(), userId, feedId, savedComment.getId()
            );
        }

        if (request.getParentId() != null) {
            Comment parentComment = findCommentById(request.getParentId());
            if (!parentComment.getWriter().getId().equals(userId)) {
                notificationService.createReplyNotification(
                    parentComment.getWriter().getId(), userId, feedId, savedComment.getId()
                );
            }
        }

        log.info("댓글 생성 완료 - commentId: {}, feedId: {}, userId: {}", savedComment.getId(), feedId, userId);
        return savedComment.getId();
    }

    /**
     * 댓글 수정
     */
    public void updateComment(Long commentId, Long userId, String content) {
        Comment comment = findCommentById(commentId);

        if (!comment.getWriter().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "댓글을 수정할 권한이 없습니다.");
        }

        if (content == null || content.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "댓글 내용은 필수입니다.");
        }

        comment.updateContent(content);
        log.info("댓글 수정 완료 - commentId: {}, userId: {}", commentId, userId);
    }

    /**
     * 댓글 삭제
     */
    public void deleteComment(Long commentId, Long userId) {
        Comment comment = findCommentById(commentId);
        User writer = comment.getWriter();

        if (!writer.getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "댓글을 삭제할 권한이 없습니다.");
        }

        // 7일 이내 삭제 시 월별 좋아요 회수
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        if (comment.getCreatedAt().isAfter(sevenDaysAgo)) {
            int likesCount = comment.getLikes().size();
            if (likesCount > 0) {
                Long currentMonthlyLikes = writer.getMonthlyLikes();
                writer.setMonthlyLikes(Math.max(0L, currentMonthlyLikes - likesCount));
            }
        }

        commentRepository.delete(comment);
        log.info("댓글 삭제 완료 - commentId: {}, userId: {}", commentId, userId);
    }

    /**
     * 댓글 좋아요
     */
    public void likeComment(Long commentId, Long userId) {
        Comment comment = findCommentById(commentId);
        User user = findUserById(userId);

        if (comment.getWriter().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "자신의 댓글에는 좋아요를 누를 수 없습니다.");
        }

        if (commentLikeRepository.existsByUserIdAndCommentId(userId, commentId)) {
            throw new BusinessException(ErrorCode.ALREADY_LIKED, "이미 좋아요를 눌렀습니다.");
        }

        CommentLike commentLike = CommentLike.builder()
                .user(user)
                .comment(comment)
                .build();

        commentLikeRepository.save(commentLike);
        comment.getWriter().incrementMonthlyLikes();

        notificationService.createCommentLikeNotification(
            comment.getWriter().getId(), userId, comment.getFeed().getId(), commentId
        );

        log.info("댓글 좋아요 완료 - commentId: {}, userId: {}", commentId, userId);
    }

    /**
     * 댓글 좋아요 취소
     */
    public void unlikeComment(Long commentId, Long userId) {
        Comment comment = findCommentById(commentId);
        CommentLike commentLike = commentLikeRepository.findByUserIdAndCommentId(userId, commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "좋아요를 누르지 않았습니다."));

        commentLikeRepository.delete(commentLike);
        comment.getWriter().decrementMonthlyLikes();

        log.info("댓글 좋아요 취소 완료 - commentId: {}, userId: {}", commentId, userId);
    }

    // === Helper Methods ===

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
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
