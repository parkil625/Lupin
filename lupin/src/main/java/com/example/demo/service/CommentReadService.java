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
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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
     * [수정] 조회 시점에 카운트 불일치를 감지하면 자동으로 복구합니다. (Read-Repair)
     */
    @Transactional // DB 업데이트가 발생할 수 있으므로 읽기 전용 해제
    public List<CommentResponse> getCommentResponsesByFeed(Long feedId, User currentUser) {
        // 1. 피드 조회
        Feed feed = feedRepository.findById(feedId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FEED_NOT_FOUND));

        // 2. 댓글 수 동기화 (Read-Repair)
        syncFeedCommentCount(feed);

        // 3. 댓글 목록 조회
        List<Comment> comments = commentRepository.findByFeedAndParentIsNullOrderByIdDesc(feed);
        
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
        // [벤치마킹] FeedQueryFacade와 동일한 로직 구조 (JPQL Bulk 결과 사용)
        java.util.List<Long> reportedList = commentReportRepository.findCommentIdsByReporterIdAndCommentIdIn(userId, commentIds);
        return new java.util.HashSet<>(reportedList);
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

    /**
     * 댓글 단건 조회 (좋아요, 신고, 활동일 정보 포함)
     */
    public CommentResponse getCommentResponse(Long commentId, User user) {
        Comment comment = getComment(commentId);
        long likeCount = commentLikeRepository.countByComment(comment);
        boolean isLiked = false;
        boolean isReported = false;
        
        if (user != null) {
            isLiked = !commentLikeRepository.findLikedCommentIdsByUserId(user.getId(), List.of(commentId)).isEmpty();
            // [수정] count > 0 사용
            isReported = commentReportRepository.countByReporterIdAndCommentId(user.getId(), commentId) > 0;
        }
        
        Map<Long, Integer> activeDaysMap = getActiveDaysMap(List.of(comment));
        return CommentResponse.from(comment, likeCount, isLiked, isReported, activeDaysMap.getOrDefault(comment.getWriter().getId(), 0));
    }

    /**
     * [Self-Healing] 피드 댓글 수 DB 동기화
     * 실제 개수와 기록된 개수가 다를 때만 업데이트 쿼리를 날립니다.
     */
    private void syncFeedCommentCount(Feed feed) {
        long realCount = commentRepository.countByFeed(feed);
        
        if (feed.getCommentCount() != realCount) {
            log.warn(">>> [Comment Sync - Read] Feed ID {} count mismatch! DB: {}, Real: {}. Fixing...", 
                    feed.getId(), feed.getCommentCount(), realCount);
            feedRepository.updateCommentCount(feed.getId(), (int) realCount);
        }
    }
}
