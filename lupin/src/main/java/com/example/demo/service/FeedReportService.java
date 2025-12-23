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
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedReportService {

    private final FeedReportRepository feedReportRepository;
    private final com.example.demo.repository.UserRepository userRepository;
    private final FeedRepository feedRepository;
    private final FeedLikeRepository feedLikeRepository;
    private final FeedImageRepository feedImageRepository;
    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final NotificationRepository notificationRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final UserPenaltyService userPenaltyService;
    private final EntityManager entityManager; // [추가]

    @Transactional
    public void toggleReport(User reporter, Long feedId) {
        // 1. 신고자(User) 조회
        User managedReporter = userRepository.findById(reporter.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 2. [핵심 수정] Feed를 프록시 객체로 조회하여 불필요한 Cascade 체크 방지
        Feed feedProxy = feedRepository.getReferenceById(feedId);

        // 3. 중복 신고 체크 (존재 여부 확인은 findById로 확실하게)
        Feed feedCheck = feedRepository.findById(feedId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FEED_NOT_FOUND));

        if (feedReportRepository.existsByReporterAndFeed(managedReporter, feedProxy)) {
            feedReportRepository.deleteByReporterAndFeed(managedReporter, feedProxy);
        } else {
            // 4. 프록시 객체를 사용하여 신고 생성
            FeedReport feedReport = FeedReport.builder()
                    .reporter(managedReporter)
                    .feed(feedProxy)
                    .build();
            
            // 5. 저장 및 즉시 반영 (saveAndFlush 사용)
            feedReportRepository.saveAndFlush(feedReport);

            // 6. 패널티 체크에는 실제 엔티티 사용
            checkAndApplyPenalty(feedCheck);
        }
    }

    private void checkAndApplyPenalty(Feed feed) {
        long likeCount = feedLikeRepository.countByFeed(feed);
        long reportCount = feedReportRepository.countByFeed(feed);

        if (userPenaltyService.shouldApplyPenalty(likeCount, reportCount)) {
            User writer = feed.getWriter();
            if (!userPenaltyService.hasActivePenalty(writer, PenaltyType.FEED)) {
                userPenaltyService.addPenalty(writer, PenaltyType.FEED);
                deleteFeedByReport(feed);
                eventPublisher.publishEvent(NotificationEvent.feedDeleted(writer.getId()));
            }
        }
    }

    /**
     * 신고로 인한 피드 삭제 시 관련 알림 삭제
     * - FEED_LIKE: refId = feedId
     * - COMMENT: refId = feedId
     * - COMMENT_LIKE: refId = commentId
     * - REPLY: refId = parentCommentId (부모 댓글 ID)
     */
    private void deleteFeedByReport(Feed feed) {
        String feedIdStr = String.valueOf(feed.getId());

        // 1. 관련 알림 삭제
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

        // 2. 연관 데이터 삭제
        feedLikeRepository.deleteByFeed(feed);
        feedReportRepository.deleteByFeed(feed); // 이미지 삭제 전 신고 먼저 삭제

        // 3. [핵심 수정] 영속성 컨텍스트를 비워서 충돌 방지
        entityManager.flush();
        entityManager.clear();

        // 4. 다시 조회 후 '이미지 관계'를 명시적으로 끊음
        Feed targetFeed = feedRepository.findById(Long.parseLong(feedIdStr))
                .orElseThrow(() -> new BusinessException(ErrorCode.FEED_NOT_FOUND));
        
        // 이미지를 먼저 비우고 DB에 반영 (CascadeType.ALL 문제 해결)
        targetFeed.getImages().clear();
        feedRepository.saveAndFlush(targetFeed);

        // 5. 최종 삭제
        feedRepository.delete(targetFeed);
    }

    /**
     * 피드 삭제 시 신고 일괄 삭제
     */
    @Transactional
    public void deleteAllByFeed(Feed feed) {
        feedReportRepository.deleteByFeed(feed);
    }
}
