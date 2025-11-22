package com.example.demo.service;

import com.example.demo.domain.entity.Comment;
import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.Report;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.entity.UserPenalty;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.FeedRepository;
import com.example.demo.repository.ReportRepository;
import com.example.demo.repository.UserPenaltyRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final ReportRepository reportRepository;
    private final FeedRepository feedRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final UserPenaltyRepository userPenaltyRepository;
    private final FeedCommandService feedCommandService;
    private final CommentService commentService;

    private static final String TARGET_TYPE_FEED = "FEED";
    private static final String TARGET_TYPE_COMMENT = "COMMENT";

    /**
     * 피드 신고
     */
    @Transactional
    public void reportFeed(Long feedId, Long reporterId) {
        Feed feed = feedRepository.findById(feedId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FEED_NOT_FOUND));
        User reporter = findUserById(reporterId);

        // 자신의 피드는 신고 불가
        if (feed.getWriter().getId().equals(reporterId)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "자신의 피드는 신고할 수 없습니다.");
        }

        // 기존 신고 기록 삭제 (같은 사용자의 동일 대상 신고)
        reportRepository.findByReporterIdAndTargetTypeAndTargetId(reporterId, TARGET_TYPE_FEED, feedId)
                .ifPresent(reportRepository::delete);

        // 새 신고 생성
        Report report = Report.builder()
                .targetType(TARGET_TYPE_FEED)
                .targetId(feedId)
                .reporter(reporter)
                .build();

        reportRepository.save(report);

        log.info("피드 신고 - feedId: {}, reporterId: {}", feedId, reporterId);

        // 자동 삭제 체크 (신고 수 >= 좋아요 수 * 5)
        checkAndDeleteFeed(feed);
    }

    /**
     * 댓글 신고
     */
    @Transactional
    public void reportComment(Long commentId, Long reporterId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));
        User reporter = findUserById(reporterId);

        // 자신의 댓글은 신고 불가
        if (comment.getWriter().getId().equals(reporterId)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "자신의 댓글은 신고할 수 없습니다.");
        }

        // 기존 신고 기록 삭제 (같은 사용자의 동일 대상 신고)
        reportRepository.findByReporterIdAndTargetTypeAndTargetId(reporterId, TARGET_TYPE_COMMENT, commentId)
                .ifPresent(reportRepository::delete);

        // 새 신고 생성
        Report report = Report.builder()
                .targetType(TARGET_TYPE_COMMENT)
                .targetId(commentId)
                .reporter(reporter)
                .build();

        reportRepository.save(report);

        log.info("댓글 신고 - commentId: {}, reporterId: {}", commentId, reporterId);

        // 자동 삭제 체크 (신고 수 >= 좋아요 수 * 5)
        checkAndDeleteComment(comment);
    }


    /**
     * 피드 자동 삭제 체크
     * 신고 수 >= 좋아요 수 * 5 (좋아요 0개는 1개로 계산)
     */
    private void checkAndDeleteFeed(Feed feed) {
        Long reportCount = reportRepository.countByTargetTypeAndTargetId(TARGET_TYPE_FEED, feed.getId());
        int likesCount = Math.max(1, feed.getLikesCount()); // 0이면 1로 계산

        if (reportCount >= likesCount * 5) {
            log.info("피드 자동 삭제 - feedId: {}, 신고: {}, 좋아요: {}",
                    feed.getId(), reportCount, feed.getLikesCount());

            // 신고 기록 삭제
            reportRepository.deleteByTargetTypeAndTargetId(TARGET_TYPE_FEED, feed.getId());

            // 피드 삭제 (기존 로직 재활용)
            feedCommandService.deleteFeed(feed.getId(), feed.getWriter().getId());

            // 패널티 생성/갱신 (UPSERT)
            User writer = feed.getWriter();
            UserPenalty penalty = userPenaltyRepository.findByUserIdAndPenaltyType(writer.getId(), TARGET_TYPE_FEED)
                    .orElse(UserPenalty.builder().user(writer).penaltyType(TARGET_TYPE_FEED).build());
            penalty.refresh();
            userPenaltyRepository.save(penalty);

            log.info("패널티 부여 - userId: {}, type: FEED", writer.getId());
        }
    }

    /**
     * 댓글 자동 삭제 체크
     * 신고 수 >= 좋아요 수 * 5 (좋아요 0개는 1개로 계산)
     */
    private void checkAndDeleteComment(Comment comment) {
        Long reportCount = reportRepository.countByTargetTypeAndTargetId(TARGET_TYPE_COMMENT, comment.getId());
        int likesCount = Math.max(1, comment.getLikes().size()); // 0이면 1로 계산

        if (reportCount >= likesCount * 5) {
            log.info("댓글 자동 삭제 - commentId: {}, 신고: {}, 좋아요: {}",
                    comment.getId(), reportCount, comment.getLikes().size());

            // 신고 기록 삭제
            reportRepository.deleteByTargetTypeAndTargetId(TARGET_TYPE_COMMENT, comment.getId());

            // 댓글 삭제 (기존 로직 재활용)
            commentService.deleteComment(comment.getId(), comment.getWriter().getId());

            // 패널티 생성/갱신 (UPSERT)
            User writer = comment.getWriter();
            UserPenalty penalty = userPenaltyRepository.findByUserIdAndPenaltyType(writer.getId(), TARGET_TYPE_COMMENT)
                    .orElse(UserPenalty.builder().user(writer).penaltyType(TARGET_TYPE_COMMENT).build());
            penalty.refresh();
            userPenaltyRepository.save(penalty);

            log.info("패널티 부여 - userId: {}, type: COMMENT", writer.getId());
        }
    }

    /**
     * 특정 피드의 신고 수 조회
     */
    public Long getFeedReportCount(Long feedId) {
        return reportRepository.countByTargetTypeAndTargetId(TARGET_TYPE_FEED, feedId);
    }

    /**
     * 특정 댓글의 신고 수 조회
     */
    public Long getCommentReportCount(Long commentId) {
        return reportRepository.countByTargetTypeAndTargetId(TARGET_TYPE_COMMENT, commentId);
    }

    /**
     * 사용자가 특정 피드를 신고했는지 확인
     */
    public boolean hasUserReportedFeed(Long userId, Long feedId) {
        return reportRepository.findByReporterIdAndTargetTypeAndTargetId(userId, TARGET_TYPE_FEED, feedId).isPresent();
    }

    /**
     * 사용자가 특정 댓글을 신고했는지 확인
     */
    public boolean hasUserReportedComment(Long userId, Long commentId) {
        return reportRepository.findByReporterIdAndTargetTypeAndTargetId(userId, TARGET_TYPE_COMMENT, commentId).isPresent();
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
