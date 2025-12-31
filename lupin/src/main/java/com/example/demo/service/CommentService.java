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
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
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
    private final NotificationSseService notificationSseService; // [추가] SSE 서비스 주입

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

        // [삭제] 단순 카운트 증가는 정확하지 않을 수 있어 제거합니다.
        // feedRepository.incrementCommentCount(feedId);
        
        // [추가] 실제 DB에 저장된 댓글 수를 조회하여 피드 정보를 동기화합니다.
        syncFeedCommentCount(feed);

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

        log.info(">>> [Comment Service] Deleting commentId: {}, ownerId: {}", commentId, user.getId());

        // [수정] 삭제할 알림 수집 (SSE 전송을 위해 조회 먼저 수행)
        java.util.List<com.example.demo.domain.entity.Notification> notificationsToDelete = new java.util.ArrayList<>();

        // 1. 해당 댓글에 달린 좋아요 알림 (COMMENT_LIKE)
        notificationsToDelete.addAll(
                notificationRepository.findByRefIdAndType(String.valueOf(commentId), NotificationType.COMMENT_LIKE)
        );

        // 2. 작성 알림 (COMMENT or REPLY)
        if (comment.getParent() == null) {
            // 원댓글: 피드 주인에게 간 COMMENT 알림 (targetId가 commentId)
            notificationsToDelete.addAll(
                    notificationRepository.findByTargetIdAndType(commentId, NotificationType.COMMENT)
            );

            // 원댓글 삭제 시, 대댓글들에 대한 알림(REPLY)들도 모두 삭제해야 함 (refId가 부모댓글ID)
            notificationsToDelete.addAll(
                    notificationRepository.findByRefIdAndType(String.valueOf(commentId), NotificationType.REPLY)
            );
        } else {
            // 대댓글: 원댓글 주인에게 간 REPLY 알림 (targetId가 commentId)
            notificationsToDelete.addAll(
                    notificationRepository.findByTargetIdAndType(commentId, NotificationType.REPLY)
            );
        }

        // 3. SSE 전송 (수신자별로 그룹핑하여 전송)
        Map<Long, List<Long>> userNotifMap = notificationsToDelete.stream()
                .collect(Collectors.groupingBy(n -> n.getUser().getId(),
                        Collectors.mapping(com.example.demo.domain.entity.Notification::getId, Collectors.toList())));

        userNotifMap.forEach((userId, ids) -> {
            notificationSseService.sendNotificationDelete(userId, ids);
            log.debug(">>> Sending delete event to userId: {}, ids: {}", userId, ids);
        });

        // 4. DB 알림 삭제
        if (!notificationsToDelete.isEmpty()) {
            notificationRepository.deleteAll(notificationsToDelete);
        }

        // 5. 댓글 좋아요 데이터 삭제
        commentLikeRepository.deleteByComment(comment);

        // 6. 부모 댓글인 경우 하위 데이터 처리 (대댓글 삭제 포함)
        if (comment.getParent() == null) {
            // 대댓글들의 데이터 및 관련 알림 삭제 (위에서 알림은 이미 처리했으나, 안전하게 내부 로직 호출)
            deleteRepliesData(comment);
        }

        // 5. 댓글 삭제
        commentRepository.delete(comment);

        // 6. 피드 댓글 수 동기화
        // [삭제] 중복 코드를 제거하고 공통 메서드를 사용합니다.
        // long realCount = commentRepository.countByFeed(comment.getFeed());
        // feedRepository.updateCommentCount(comment.getFeed().getId(), (int) realCount);
        
        // [추가] 삭제가 반영된 후의 실제 댓글 수를 카운트하여 업데이트합니다.
        syncFeedCommentCount(comment.getFeed());
    }

    /**
     * 부모 댓글 삭제 시 대댓글들의 좋아요, 알림, 대댓글 자체 삭제
     */
    private void deleteRepliesData(Comment parentComment) {
        List<Comment> replies = commentRepository.findByParentOrderByIdAsc(parentComment);
        if (replies.isEmpty()) {
            return;
        }

        List<String> replyIds = replies.stream()
                .map(r -> String.valueOf(r.getId()))
                .toList();

        // [수정] 대댓글 좋아요 알림은 위에서 처리 안 되었을 수 있으므로 여기서 처리 (SSE + 삭제)
        // 대댓글 삭제 시, 그 대댓글에 달린 좋아요 알림도 지워야 함.
        java.util.List<com.example.demo.domain.entity.Notification> replyLikeNotifications = 
                notificationRepository.findByRefIdInAndType(replyIds, NotificationType.COMMENT_LIKE);
        
        if (!replyLikeNotifications.isEmpty()) {
            // SSE 전송
             Map<Long, List<Long>> userNotifMap = replyLikeNotifications.stream()
                .collect(Collectors.groupingBy(n -> n.getUser().getId(),
                        Collectors.mapping(com.example.demo.domain.entity.Notification::getId, Collectors.toList())));

            userNotifMap.forEach((userId, ids) -> {
                notificationSseService.sendNotificationDelete(userId, ids);
            });
            
            // DB 삭제
            notificationRepository.deleteAll(replyLikeNotifications);
        }

        // [참고] 대댓글 작성 알림(REPLY)은 deleteComment 메인 로직에서 이미 처리됨

        // [최적화] 대댓글들의 좋아요 일괄 삭제 (N개 쿼리 → 1개 쿼리)
        commentLikeRepository.deleteByCommentIn(replies);

        // [수정] 단순 감소 로직 제거 (상위 메서드에서 일괄 동기화)
        log.info(">>> [Comment Service] Soft deleting {} replies for parent comment ID: {}", replies.size(), parentComment.getId());
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

        // [삭제] 단순 카운트 증가는 정확하지 않을 수 있어 제거합니다.
        // feedRepository.incrementCommentCount(feedId);

        // [추가] 실제 DB에 저장된 댓글 수를 조회하여 피드 정보를 동기화합니다.
        syncFeedCommentCount(feed);

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
        // [추가] 신고 여부 조회
        Set<Long> reportedIds = currentUser != null
                ? getReportedCommentIds(currentUser.getId(), commentIds)
                : Collections.emptySet();
        Map<Long, Integer> activeDaysMap = getActiveDaysMap(comments);

        return comments.stream()
                .map(comment -> CommentResponse.from(
                        comment,
                        likeCounts.getOrDefault(comment.getId(), 0L),
                        likedIds.contains(comment.getId()),
                        reportedIds.contains(comment.getId()), // [추가] 신고 여부 전달
                        activeDaysMap.getOrDefault(comment.getWriter().getId(), 0)
                ))
                .toList();
    }

    /**
     * [추가] 사용자가 신고한 댓글 ID 조회
     */
    private Set<Long> getReportedCommentIds(Long userId, List<Long> commentIds) {
        if (commentIds.isEmpty()) {
            return Collections.emptySet();
        }
        log.debug(">>> [CommentService] Fetching reported comments for userId: {}, commentIds count: {}", userId, commentIds.size());
        // [Fix] Repository method name match: findCommentIdsByReporterIdAndCommentIdIn
        return Set.copyOf(commentReportRepository.findCommentIdsByReporterIdAndCommentIdIn(userId, commentIds));
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

    // [추가]
    /**
     * [추가] 피드 댓글 수 DB 동기화
     * delete_at이 없는(삭제되지 않은) 실존 댓글만 카운트하여 feed 테이블에 업데이트합니다.
     * 트랜잭션 내에서 flush()를 호출하여 변경사항(INSERT/DELETE)을 즉시 반영한 후 조회합니다.
     */
    private void syncFeedCommentCount(Feed feed) {
        // 쿼리 실행 전 변경사항 반영
        commentRepository.flush();
        
        // 실제 댓글 수 조회 (@SQLRestriction으로 인해 삭제된 댓글은 자동 제외됨)
        long realCount = commentRepository.countByFeed(feed);
        
        log.info(">>> [Comment Service] Syncing comment count for feedId: {}. DB Real Count: {}", feed.getId(), realCount);
        
        // 피드 테이블 업데이트
        feedRepository.updateCommentCount(feed.getId(), (int) realCount);
    }
}
