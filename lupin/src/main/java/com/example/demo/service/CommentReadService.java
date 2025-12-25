package com.example.demo.service;

import com.example.demo.domain.entity.Comment;
import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.User;
import com.example.demo.dto.WriterActiveDays;
import com.example.demo.dto.response.CommentResponse;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.CommentLikeRepository;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.FeedRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 댓글 조회 서비스 - CQRS 패턴
 * 읽기 작업을 분리하여 조회 성능 최적화 및 책임 분리
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentReadService {

    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final com.example.demo.repository.CommentReportRepository commentReportRepository;
    private final FeedRepository feedRepository;

    /**
     * 댓글 단건 조회
     */
    public Comment getComment(Long commentId) {
        return commentRepository.findByIdWithDetails(commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));
    }

    /**
     * 피드의 댓글 목록 조회 (최신순)
     */
    public List<Comment> getCommentsByFeed(Long feedId) {
        Feed feed = feedRepository.findById(feedId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FEED_NOT_FOUND));

        return commentRepository.findByFeedAndParentIsNullOrderByIdDesc(feed);
    }

    /**
     * 피드의 댓글 목록 조회 (인기순)
     */
    public List<Comment> getCommentsByFeedOrderByPopular(Long feedId) {
        Feed feed = feedRepository.findById(feedId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FEED_NOT_FOUND));

        return commentRepository.findByFeedOrderByLikeCountDesc(feed);
    }

    /**
     * 답글 목록 조회
     */
    public List<Comment> getReplies(Long parentId) {
        Comment parent = commentRepository.findById(parentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));

        return commentRepository.findByParentOrderByIdAsc(parent);
    }

    /**
     * 피드 댓글 목록 조회 (좋아요, 활동일 정보 포함)
     */
    public List<CommentResponse> getCommentResponsesByFeed(Long feedId, User currentUser) {
        List<Comment> comments = getCommentsByFeed(feedId);
        return assembleCommentResponses(comments, currentUser);
    }

    /**
     * 답글 목록 조회 (좋아요, 활동일 정보 포함)
     */
    public List<CommentResponse> getReplyResponses(Long parentId, User currentUser) {
        List<Comment> replies = getReplies(parentId);
        return assembleCommentResponses(replies, currentUser);
    }

    /**
     * 댓글 목록을 CommentResponse로 조립
     */
    private List<CommentResponse> assembleCommentResponses(List<Comment> comments, User currentUser) {
        if (comments.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> commentIds = comments.stream().map(Comment::getId).toList();
        Map<Long, Long> likeCounts = getLikeCountMap(commentIds);

        Set<Long> likedIds = Collections.emptySet();
        Set<Long> reportedIds = Collections.emptySet();

        if (currentUser != null) {
            likedIds = getLikedCommentIds(currentUser.getId(), commentIds);
            reportedIds = getReportedCommentIds(currentUser.getId(), commentIds);
        }

        Map<Long, Integer> activeDaysMap = getActiveDaysMap(comments);

        final Set<Long> finalLikedIds = likedIds;
        final Set<Long> finalReportedIds = reportedIds;

        return comments.stream()
                .map(comment -> CommentResponse.from(
                        comment,
                        likeCounts.getOrDefault(comment.getId(), 0L),
                        finalLikedIds.contains(comment.getId()),
                        finalReportedIds.contains(comment.getId()),
                        activeDaysMap.getOrDefault(comment.getWriter().getId(), 0)
                ))
                .toList();
    }

    /**
     * 댓글 좋아요 수 배치 조회
     */
    private Map<Long, Long> getLikeCountMap(List<Long> commentIds) {
        if (commentIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return commentLikeRepository.countByCommentIds(commentIds).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));
    }

    /**
     * 사용자가 좋아요한 댓글 ID 조회
     */
    private Set<Long> getLikedCommentIds(Long userId, List<Long> commentIds) {
        if (commentIds.isEmpty()) {
            return Collections.emptySet();
        }
        return Set.copyOf(commentLikeRepository.findLikedCommentIdsByUserId(userId, commentIds));
    }

    private Set<Long> getReportedCommentIds(Long userId, List<Long> commentIds) {
        if (commentIds.isEmpty()) {
            return Collections.emptySet();
        }
        return Set.copyOf(commentReportRepository.findReportedCommentIdsByReporterId(userId, commentIds));
    }

    /**
     * 작성자별 이번 달 활동일 배치 조회
     */
    private Map<Long, Integer> getActiveDaysMap(List<Comment> comments) {
        if (comments.isEmpty()) {
            return Map.of();
        }

        List<Long> writerIds = comments.stream()
                .map(comment -> comment.getWriter().getId())
                .distinct()
                .toList();

        YearMonth currentMonth = YearMonth.now();
        LocalDateTime startOfMonth = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = currentMonth.atEndOfMonth().atTime(23, 59, 59);

        List<WriterActiveDays> results = feedRepository.findActiveDaysByWriterIds(writerIds, startOfMonth, endOfMonth);

        Map<Long, Integer> activeDaysMap = new HashMap<>();
        for (WriterActiveDays row : results) {
            activeDaysMap.put(row.writerId(), row.activeDays().intValue());
        }
        return activeDaysMap;
    }
}
