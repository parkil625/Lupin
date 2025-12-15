package com.example.demo.service;

import com.example.demo.domain.Reportable;
import com.example.demo.domain.entity.*;
import com.example.demo.domain.enums.PenaltyType;
import com.example.demo.event.NotificationEvent;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.BiConsumer;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
@Transactional
public class ReportService {

    private final FeedRepository feedRepository;
    private final CommentRepository commentRepository;
    private final FeedReportRepository feedReportRepository;
    private final CommentReportRepository commentReportRepository;
    private final FeedLikeRepository feedLikeRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final UserPenaltyService userPenaltyService;
    private final ApplicationEventPublisher eventPublisher;
    private final FeedDeleteFacade feedDeleteFacade;
    private final CommentDeleteFacade commentDeleteFacade;

    public void toggleFeedReport(User reporter, Long feedId) {
        Feed feed = findFeedById(feedId);
        feedReportRepository.findByReporterAndFeed(reporter, feed)
                .ifPresentOrElse(
                        feedReportRepository::delete,
                        () -> {
                            FeedReport report = FeedReport.builder().reporter(reporter).feed(feed).build();
                            feedReportRepository.save(report);
                            checkAndApplyPenalty(
                                    feed,
                                    PenaltyType.FEED,
                                    feedLikeRepository::countByFeed,
                                    feedReportRepository::countByFeed,
                                    (user, id) -> feedDeleteFacade.deleteFeed(user, id),
                                    () -> eventPublisher.publishEvent(NotificationEvent.feedDeleted(feed.getWriter().getId()))
                            );
                        }
                );
    }

    public void toggleCommentReport(User reporter, Long commentId) {
        Comment comment = findCommentById(commentId);
        commentReportRepository.findByReporterAndComment(reporter, comment)
                .ifPresentOrElse(
                        commentReportRepository::delete,
                        () -> {
                            CommentReport report = CommentReport.builder().reporter(reporter).comment(comment).build();
                            commentReportRepository.save(report);
                            checkAndApplyPenalty(
                                    comment,
                                    PenaltyType.COMMENT,
                                    commentLikeRepository::countByComment,
                                    commentReportRepository::countByComment,
                                    (user, id) -> commentDeleteFacade.deleteComment(user, id),
                                    () -> eventPublisher.publishEvent(NotificationEvent.commentDeleted(comment.getWriter().getId()))
                            );
                        }
                );
    }

    private <T extends Reportable> void checkAndApplyPenalty(
            T target,
            PenaltyType penaltyType,
            Function<T, Long> likeCounter,
            Function<T, Long> reportCounter,
            BiConsumer<User, Long> deleteAction,
            Runnable notificationAction
    ) {
        long likeCount = likeCounter.apply(target);
        long reportCount = reportCounter.apply(target);

        if (userPenaltyService.shouldApplyPenalty(likeCount, reportCount)) {
            User writer = target.getWriter();
            if (!userPenaltyService.hasActivePenalty(writer, penaltyType)) {
                userPenaltyService.addPenalty(writer, penaltyType);
                deleteAction.accept(writer, target.getId());
                notificationAction.run();
            }
        }
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
