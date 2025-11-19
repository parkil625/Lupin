package com.example.demo.service;

import com.example.demo.domain.entity.Comment;
import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.User;
import com.example.demo.dto.request.CommentCreateRequest;
import com.example.demo.dto.response.CommentResponse;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.FeedRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 댓글 관련 비즈니스 로직을 처리하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final FeedRepository feedRepository;
    private final UserRepository userRepository;

    /**
     * 댓글 생성
     */
    @Transactional
    public CommentResponse createComment(Long feedId, Long userId, CommentCreateRequest request) {
        User user = findUserById(userId);
        Feed feed = findFeedById(feedId);

        Comment comment = Comment.builder()
                .content(request.getContent())
                .writer(user)
                .build();

        comment.setFeed(feed);

        // 답글인 경우 부모 댓글 설정
        if (request.getParentId() != null) {
            Comment parent = findCommentById(request.getParentId());

            // 답글의 답글은 허용하지 않음
            if (parent.isReply()) {
                throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "답글에는 답글을 달 수 없습니다.");
            }

            parent.addReply(comment);
        }

        Comment savedComment = commentRepository.save(comment);

        log.info("댓글 생성 완료 - commentId: {}, feedId: {}, userId: {}",
                savedComment.getId(), feedId, userId);

        return CommentResponse.from(savedComment);
    }

    /**
     * 특정 피드의 댓글 목록 조회 (최상위 댓글만, 페이징)
     */
    public Page<CommentResponse> getCommentsByFeedId(Long feedId, Pageable pageable) {
        return commentRepository.findTopLevelCommentsByFeedId(feedId, pageable)
                .map(CommentResponse::from);
    }

    /**
     * 특정 피드의 댓글 목록 조회 (최상위 댓글만, 전체)
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
        Comment comment = findCommentById(commentId);
        return CommentResponse.from(comment);
    }

    /**
     * 댓글 수정
     */
    @Transactional
    public CommentResponse updateComment(Long commentId, Long userId, String content) {
        Comment comment = findCommentById(commentId);

        // 작성자 확인
        if (!comment.getWriter().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "댓글을 수정할 권한이 없습니다.");
        }

        // 내용이 비어있는지 확인
        if (content == null || content.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "댓글 내용은 필수입니다.");
        }

        // 댓글 내용 수정
        comment.updateContent(content);

        log.info("댓글 수정 완료 - commentId: {}, userId: {}", commentId, userId);

        return CommentResponse.from(comment);
    }

    /**
     * 댓글 삭제
     */
    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        Comment comment = findCommentById(commentId);

        // 작성자 확인
        if (!comment.getWriter().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "댓글을 삭제할 권한이 없습니다.");
        }

        commentRepository.delete(comment);

        log.info("댓글 삭제 완료 - commentId: {}, userId: {}", commentId, userId);
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

    // === 헬퍼 메서드 ===

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
