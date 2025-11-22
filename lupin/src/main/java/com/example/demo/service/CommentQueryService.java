package com.example.demo.service;

import com.example.demo.domain.entity.Comment;
import com.example.demo.domain.entity.QComment;
import com.example.demo.domain.entity.QCommentLike;
import com.example.demo.domain.entity.QUser;
import com.example.demo.dto.response.CommentResponse;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.CommentLikeRepository;
import com.example.demo.repository.CommentRepository;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 댓글 Query 서비스 (읽기 전용)
 * CQRS 패턴 - 데이터 조회 작업 담당
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentQueryService {

    private final JPAQueryFactory queryFactory;
    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;

    private final QComment comment = QComment.comment;
    private final QUser user = QUser.user;
    private final QCommentLike commentLike = QCommentLike.commentLike;

    /**
     * 특정 피드의 댓글 목록 조회 (페이징)
     */
    public Page<CommentResponse> getCommentsByFeedId(Long feedId, Pageable pageable) {
        return commentRepository.findTopLevelCommentsByFeedId(feedId, pageable)
                .map(CommentResponse::from);
    }

    /**
     * 특정 피드의 댓글 목록 조회 (전체)
     */
    public List<CommentResponse> getAllCommentsByFeedId(Long feedId) {
        return commentRepository.findTopLevelCommentsByFeedId(feedId)
                .stream()
                .map(CommentResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 특정 댓글의 답글 조회
     */
    public List<CommentResponse> getRepliesByCommentId(Long commentId) {
        return commentRepository.findRepliesByParentId(commentId)
                .stream()
                .map(CommentResponse::fromWithoutReplies)
                .collect(Collectors.toList());
    }

    /**
     * 댓글 상세 조회
     */
    public CommentResponse getCommentDetail(Long commentId) {
        Comment commentEntity = commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));
        return CommentResponse.from(commentEntity);
    }

    /**
     * 특정 피드의 댓글 수 조회
     */
    public Long getCommentCountByFeedId(Long feedId) {
        return commentRepository.countByFeedId(feedId);
    }

    /**
     * 특정 사용자의 댓글 조회
     */
    public Page<CommentResponse> getCommentsByUserId(Long userId, Pageable pageable) {
        return commentRepository.findByWriterId(userId, pageable)
                .map(CommentResponse::from);
    }

    /**
     * 댓글 좋아요 여부 확인
     */
    public boolean hasUserLikedComment(Long commentId, Long userId) {
        return commentLikeRepository.existsByUserIdAndCommentId(userId, commentId);
    }

    /**
     * 댓글 좋아요 수 조회
     */
    public Long getCommentLikeCount(Long commentId) {
        return commentLikeRepository.countByCommentId(commentId);
    }
}
