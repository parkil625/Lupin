package com.example.demo.service;

import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.FeedReport;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.PenaltyType;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.CommentLikeRepository;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.FeedImageRepository;
import com.example.demo.repository.FeedLikeRepository;
import com.example.demo.repository.FeedReportRepository;
import com.example.demo.repository.FeedRepository;
import com.example.demo.repository.NotificationRepository;
import com.example.demo.domain.enums.NotificationType;
import com.example.demo.event.NotificationEvent;
import com.example.demo.repository.UserRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // [추가]
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedReportService {
    
    private final FeedReportRepository feedReportRepository;
    private final UserRepository userRepository;
    private final FeedRepository feedRepository;
    private final FeedLikeRepository feedLikeRepository;
    private final FeedImageRepository feedImageRepository;
    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final NotificationRepository notificationRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final UserPenaltyService userPenaltyService;
    private final EntityManager entityManager;
    private final PointService pointService;

    @Transactional
    public boolean toggleReport(User reporter, Long feedId) {
        User managedReporter = userRepository.findById(reporter.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!feedRepository.existsById(feedId)) {
            throw new BusinessException(ErrorCode.FEED_NOT_FOUND);
        }

        boolean isReported;
        // 이미 신고 내역이 있으면 -> 삭제(취소) 및 false 설정
        if (feedReportRepository.existsByReporter_IdAndFeed_Id(managedReporter.getId(), feedId)) {
            feedReportRepository.deleteByReporter_IdAndFeed_Id(managedReporter.getId(), feedId);
            isReported = false;
        } else {
            // 신고 내역이 없으면 -> 저장(신고) 및 true 설정
            Feed feedProxy = feedRepository.getReferenceById(feedId);
            
            FeedReport feedReport = FeedReport.builder()
                    .reporter(managedReporter)
                    .feed(feedProxy)
                    .build();
            
            feedReportRepository.save(feedReport);
            isReported = true;

            checkAndApplyPenalty(feedProxy);
        }
        return isReported; // 최종 상태 반환
    }

    private void checkAndApplyPenalty(Feed feed) {
        long likeCount = feedLikeRepository.countByFeedId(feed.getId());
        long reportCount = feedReportRepository.countByFeedId(feed.getId());
        
        log.info(">>> [신고 디버깅] Feed ID: {}, 좋아요 수: {}, 신고 수: {}", feed.getId(), likeCount, reportCount);

        boolean shouldPenalty = userPenaltyService.shouldApplyPenalty(likeCount, reportCount);
        log.info(">>> [신고 디버깅] 패널티 적용 대상인가? {}", shouldPenalty);

        if (shouldPenalty) {
            User writer = feed.getWriter();
            boolean hasActivePenalty = userPenaltyService.hasActivePenalty(writer, PenaltyType.FEED);
            log.info(">>> [신고 디버깅] 작성자(ID:{})가 이미 패널티 중인가? {}", writer.getId(), hasActivePenalty);

            if (!hasActivePenalty) {
                log.info(">>> [신고 디버깅] 신규 패널티 부여");
                userPenaltyService.addPenalty(writer, PenaltyType.FEED);
            }

            log.info(">>> [신고 디버깅] 피드 삭제 시작");
            deleteFeedByReport(feed);
            eventPublisher.publishEvent(NotificationEvent.feedDeleted(writer.getId()));
            log.info(">>> [신고 디버깅] 처리 완료");
        }
    }

    private void deleteFeedByReport(Feed feed) {
        String feedIdStr = String.valueOf(feed.getId());

        notificationRepository.deleteByRefIdAndType(feedIdStr, NotificationType.FEED_LIKE);
        notificationRepository.deleteByRefIdAndType(feedIdStr, NotificationType.COMMENT);

        List<String> parentCommentIds = commentRepository.findByFeed(feed).stream()
                .filter(c -> c.getParent() == null)
                .map(c -> String.valueOf(c.getId()))
                .toList();

        if (!parentCommentIds.isEmpty()) {
            notificationRepository.deleteByRefIdInAndType(parentCommentIds, NotificationType.REPLY);
        }

        List<String> allCommentIds = commentRepository.findByFeed(feed).stream()
                .map(c -> String.valueOf(c.getId()))
                .toList();

        if (!allCommentIds.isEmpty()) {
            notificationRepository.deleteByRefIdInAndType(allCommentIds, NotificationType.COMMENT_LIKE);
        }

        entityManager.flush();
        entityManager.clear();

        Feed targetFeed = feedRepository.findById(Long.parseLong(feedIdStr))
                .orElseThrow(() -> new BusinessException(ErrorCode.FEED_NOT_FOUND));
        
        // [추가] 신고 삭제 시 포인트 회수 로직
        if (targetFeed.getPoints() > 0) {
            log.info(">>> [신고 디버깅] 신고 삭제로 인한 포인트 회수: userId={}, points={}", targetFeed.getWriter().getId(), targetFeed.getPoints());
            pointService.deductPoints(targetFeed.getWriter(), targetFeed.getPoints());
        }

        feedRepository.delete(targetFeed);
    }

    @Transactional
    public void deleteAllByFeed(Feed feed) {
        feedReportRepository.deleteByFeed(feed);
    }
}