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
        // 1. [중요] 컨트롤러에서 넘어온 reporter 대신, DB에서 진짜 유저 정보를 다시 가져옵니다.
        User managedReporter = userRepository.findById(reporter.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 2. 게시글 찾기 (이미 잘하셨습니다)
        Feed feed = feedRepository.findById(feedId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FEED_NOT_FOUND));

        if (feedReportRepository.existsByReporterAndFeed(managedReporter, feed)) {
            feedReportRepository.deleteByReporterAndFeed(managedReporter, feed);
        } else {
            // 3. 진짜 유저(managedReporter)와 진짜 피드(feed)로 신고장 작성
            FeedReport feedReport = FeedReport.builder()
                    .reporter(managedReporter) // 여기가 핵심입니다!
                    .feed(feed)
                    .build();
            
            feedReportRepository.save(feedReport);
            
            // [중요] 변경 사항을 먼저 DB에 다 털어넣습니다.
            feedReportRepository.flush(); 

            // 신고 누적 체크 및 삭제 로직 실행
            checkAndApplyPenalty(feed);
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

        // FEED_LIKE, COMMENT 알림 삭제 (refId = feedId)
        notificationRepository.deleteByRefIdAndType(feedIdStr, NotificationType.FEED_LIKE);
        notificationRepository.deleteByRefIdAndType(feedIdStr, NotificationType.COMMENT);

        // 댓글 ID 수집 (부모 댓글만)
        List<String> parentCommentIds = commentRepository.findByFeed(feed).stream()
                .filter(c -> c.getParent() == null)
                .map(c -> String.valueOf(c.getId()))
                .toList();

        // REPLY 알림 삭제 (refId = 부모 댓글 ID)
        if (!parentCommentIds.isEmpty()) {
            notificationRepository.deleteByRefIdInAndType(parentCommentIds, NotificationType.REPLY);
        }

        // 모든 댓글 ID 수집 (COMMENT_LIKE 삭제용)
        List<String> allCommentIds = commentRepository.findByFeed(feed).stream()
                .map(c -> String.valueOf(c.getId()))
                .toList();

        // COMMENT_LIKE 알림 삭제 (refId = commentId)
        if (!allCommentIds.isEmpty()) {
            notificationRepository.deleteByRefIdInAndType(allCommentIds, NotificationType.COMMENT_LIKE);
        }

        feedLikeRepository.deleteByFeed(feed);
        feedImageRepository.deleteByFeed(feed);
        feedReportRepository.deleteByFeed(feed);

        // [수정] 강제로 영속성 컨텍스트를 비워서 '유령 이미지' 문제를 원천 차단합니다.
        entityManager.flush();
        entityManager.clear();

        // 다시 조회해서 삭제 (이제 깨끗한 상태에서 삭제하므로 에러 안 남)
        Feed targetFeed = feedRepository.findById(Long.parseLong(feedIdStr))
                .orElseThrow(() -> new BusinessException(ErrorCode.FEED_NOT_FOUND));
        
        // [핵심] 삭제하기 전에 연관된 이미지들을 먼저 끊어줍니다 (Cascade 문제 방지)
        targetFeed.getImages().clear();
        feedRepository.saveAndFlush(targetFeed); // 관계 끊은거 반영

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
