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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

    @Transactional
    public void toggleReport(User reporter, Long feedId) {
        // 1. 신고자(User) 조회 - 영속성 컨텍스트로 불러오기
        User managedReporter = userRepository.findById(reporter.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 2. 게시글 존재 여부 확인 (단순 조회)
        if (!feedRepository.existsById(feedId)) {
            throw new BusinessException(ErrorCode.FEED_NOT_FOUND);
        }

        // 3. [핵심] Feed를 '프록시(Proxy)'로 조회
        // findById 대신 getReferenceById를 사용하여 불필요한 연관관계(이미지 등) 로딩을 방지합니다.
        Feed feedProxy = feedRepository.getReferenceById(feedId);

        if (feedReportRepository.existsByReporterAndFeed(managedReporter, feedProxy)) {
            feedReportRepository.deleteByReporterAndFeed(managedReporter, feedProxy);
        } else {
            // 4. 프록시 객체를 사용하여 신고 생성 (DB 에러 방지)
            FeedReport feedReport = FeedReport.builder()
                    .reporter(managedReporter)
                    .feed(feedProxy)
                    .build();
            
            // 5. 즉시 DB 반영 (트랜잭션 커밋 시점의 충돌 방지)
            feedReportRepository.saveAndFlush(feedReport);

            // 6. 패널티 체크 (여기서는 실제 엔티티가 필요할 수 있으므로 다시 조회하거나 프록시 사용)
            checkAndApplyPenalty(feedProxy);
        }
    }

    private void checkAndApplyPenalty(Feed feed) {
        long likeCount = feedLikeRepository.countByFeed(feed);
        long reportCount = feedReportRepository.countByFeed(feed);

        if (userPenaltyService.shouldApplyPenalty(likeCount, reportCount)) {
            // 작성자 정보가 필요하므로 여기서 초기화될 수 있음
            User writer = feed.getWriter();
            if (!userPenaltyService.hasActivePenalty(writer, PenaltyType.FEED)) {
                userPenaltyService.addPenalty(writer, PenaltyType.FEED);
                deleteFeedByReport(feed);
                eventPublisher.publishEvent(NotificationEvent.feedDeleted(writer.getId()));
            }
        }
    }

    /**
     * 신고로 인한 피드 삭제 로직
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
        feedImageRepository.deleteByFeed(feed);
        feedReportRepository.deleteByFeed(feed); // 이미지 삭제 전 신고 먼저 삭제

        // 3. [핵심] 영속성 컨텍스트 초기화 (삭제 충돌 방지)
        entityManager.flush();
        entityManager.clear();

        // 4. 다시 조회 후 삭제 (안전하게 Soft Delete 수행)
        Feed targetFeed = feedRepository.findById(Long.parseLong(feedIdStr))
                .orElseThrow(() -> new BusinessException(ErrorCode.FEED_NOT_FOUND));
        
        // [중요] 삭제 전 이미지 컬렉션을 비워서 Cascade 충돌 방지
        targetFeed.getImages().clear();
        feedRepository.saveAndFlush(targetFeed);

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