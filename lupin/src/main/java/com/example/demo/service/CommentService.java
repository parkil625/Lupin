package com.example.demo.service;

import com.example.demo.domain.entity.Comment;
import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.User;
import com.example.demo.dto.WriterActiveDays;
import com.example.demo.dto.response.CommentResponse;
import com.example.demo.event.NotificationEvent;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.CommentLikeRepository;
import com.example.demo.repository.CommentReportRepository;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.FeedRepository;
import com.example.demo.repository.NotificationRepository;
import com.example.demo.domain.enums.NotificationType;
import com.example.demo.repository.UserPenaltyRepository; // [추가]
import com.example.demo.domain.enums.PenaltyType; // [추가]
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final CommentReportRepository commentReportRepository;
    private final FeedRepository feedRepository;
    private final NotificationRepository notificationRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final UserPenaltyRepository userPenaltyRepository; // [추가] Repository 주입

    @Transactional
    public Comment createComment(User writer, Long feedId, String content) {
        // [수정] 댓글 작성 금지 패널티 확인 (3일)
        LocalDateTime threeDaysAgo = LocalDateTime.now().minusDays(3);
        // [수정] User 객체 대신 ID로 조회
        if (userPenaltyRepository.existsByUserIdAndPenaltyTypeAndCreatedAtAfter(writer.getId(), PenaltyType.COMMENT, threeDaysAgo)) {
             throw new BusinessException(ErrorCode.COMMENT_CREATION_RESTRICTED);
        }

        Feed feed = feedRepository.findById(feedId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FEED_NOT_FOUND));

        Comment comment = Comment.builder()
                .writer(writer)
                .feed(feed)
                .content(content)
                .build();

        Comment savedComment = commentRepository.save(comment);

        // 댓글 카운트 증가 (원자적 업데이트로 동시성 문제 해결)
        feedRepository.incrementCommentCount(feedId);

        // [최적화] 이벤트 발행 - 트랜잭션 커밋 후 비동기 알림 처리
        eventPublisher.publishEvent(NotificationEvent.comment(
                feed.getWriter().getId(),
                writer.getId(),
                writer.getName(),
                writer.getAvatar(),
                feedId,
                savedComment.getId(),
                content
        ));

        return savedComment;
    }

    @Transactional
    public Comment updateComment(User user, Long commentId, String content) {
        Comment comment = commentRepository.findByIdWithDetails(commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));

        comment.validateOwner(user);
        comment.update(content);
        return comment;
    }

    @Transactional
    public void deleteComment(User user, Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));

        comment.validateOwner(user);

        // 댓글 카운트 감소 (원자적 업데이트로 동시성 문제 해결)
        feedRepository.decrementCommentCount(comment.getFeed().getId());

        // COMMENT_LIKE 알림 삭제 (refId = commentId)
        notificationRepository.deleteByRefIdAndType(String.valueOf(commentId), NotificationType.COMMENT_LIKE);

        // 댓글 좋아요 삭제 (외래키 제약조건)
        commentLikeRepository.deleteByComment(comment);

        // 부모 댓글인 경우
        if (comment.getParent() == null) {
            // REPLY 알림 삭제 (refId = 부모 댓글 ID = 본인 ID)
            notificationRepository.deleteByRefIdAndType(String.valueOf(commentId), NotificationType.REPLY);
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
        notificationRepository.deleteByRefIdInAndType(replyIds, NotificationType.COMMENT_LIKE);

        // [최적화] 대댓글들의 좋아요 일괄 삭제 (N개 쿼리 → 1개 쿼리)
        commentLikeRepository.deleteByCommentIn(replies);

        // 대댓글 카운트 벌크 감소 후 일괄 삭제 (N번 쿼리 → 1번 쿼리로 최적화)
        Long feedId = parentComment.getFeed().getId();
        if (!replies.isEmpty()) {
            feedRepository.decrementCommentCountBy(feedId, replies.size());
        }
        commentRepository.deleteAll(replies);
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
        // [수정] 대댓글 작성 금지 패널티 확인 (3일) - 댓글 금지와 동일하게 처리
        LocalDateTime threeDaysAgo = LocalDateTime.now().minusDays(3);
        // [수정] User 객체 대신 ID로 조회
        if (userPenaltyRepository.existsByUserIdAndPenaltyTypeAndCreatedAtAfter(writer.getId(), PenaltyType.COMMENT, threeDaysAgo)) {
             throw new BusinessException(ErrorCode.COMMENT_CREATION_RESTRICTED);
        }

        Feed feed = feedRepository.findById(feedId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FEED_NOT_FOUND));

        Comment parent = commentRepository.findById(parentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));

        // 대댓글에 답글 불가 (depth 1 제한)
        if (parent.getParent() != null) {
            throw new BusinessException(ErrorCode.REPLY_DEPTH_EXCEEDED);
        }

        Comment reply = Comment.builder()
                .writer(writer)
                .feed(feed)
                .parent(parent)
                .content(content)
                .build();

        Comment savedReply = commentRepository.save(reply);

        // 대댓글도 댓글 카운트 증가 (원자적 업데이트로 동시성 문제 해결)
        feedRepository.incrementCommentCount(feedId);

        // [최적화] 이벤트 발행 - 트랜잭션 커밋 후 비동기 알림 처리
        eventPublisher.publishEvent(NotificationEvent.reply(
                parent.getWriter().getId(),
                writer.getId(),
                writer.getName(),
                writer.getAvatar(),
                parentId,
                savedReply.getId(),
                content
        ));

        return savedReply;
    }

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
        Set<Long> likedIds = currentUser != null
                ? getLikedCommentIds(currentUser.getId(), commentIds)
                : Collections.emptySet();
        Map<Long, Integer> activeDaysMap = getActiveDaysMap(comments);

        return comments.stream()
                .map(comment -> CommentResponse.from(
                        comment,
                        likeCounts.getOrDefault(comment.getId(), 0L),
                        likedIds.contains(comment.getId()),
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
     * 피드 삭제 시 관련 댓글 데이터 일괄 삭제
     * @return 삭제 대상 댓글 ID 정보 (부모 댓글 ID, 전체 댓글 ID)
     */
    @Transactional
    public CommentDeleteResult deleteAllByFeed(Feed feed) {
        List<Long> parentCommentIds = commentRepository.findParentCommentIdsByFeed(feed);
        List<Long> allCommentIds = commentRepository.findCommentIdsByFeed(feed);

        // 댓글 좋아요 삭제
        commentLikeRepository.deleteByFeed(feed);
        // 댓글 신고 삭제
        commentReportRepository.deleteByFeed(feed);
        // 대댓글 삭제
        commentRepository.deleteRepliesByFeed(feed);
        // 부모 댓글 삭제
        commentRepository.deleteParentCommentsByFeed(feed);

        return new CommentDeleteResult(parentCommentIds, allCommentIds);
    }

    /**
     * 댓글 삭제 결과 (알림 삭제용)
     */
    public record CommentDeleteResult(List<Long> parentCommentIds, List<Long> allCommentIds) {}
}
