package com.example.demo.scheduler;

import com.example.demo.domain.entity.*;
import com.example.demo.repository.*;
import com.example.demo.service.RedisCounterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * Redis -> DB 동기화 스케줄러
 * - Redis는 빠르지만 휘발성
 * - 주기적으로 DB에 백업하여 데이터 영속성 보장
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisSyncScheduler {

    private final RedisCounterService redisCounterService;
    private final FeedRepository feedRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final DrawPrizeRepository drawPrizeRepository;

    /**
     * 피드 카운터 동기화 (5분마다)
     */
    @Scheduled(fixedRate = 300000)
    @Transactional
    public void syncFeedCounters() {
        log.debug("피드 카운터 동기화 시작");

        List<Feed> feeds = feedRepository.findAll();
        int updated = 0;

        for (Feed feed : feeds) {
            Long redisLikes = redisCounterService.getFeedLikes(feed.getId());
            Long redisComments = redisCounterService.getFeedComments(feed.getId());

            boolean needsUpdate = false;

            if (redisLikes > 0 && !redisLikes.equals((long) feed.getLikesCount())) {
                // Redis 값이 있고 DB와 다르면 업데이트
                needsUpdate = true;
            }
            if (redisComments > 0 && !redisComments.equals((long) feed.getCommentsCount())) {
                needsUpdate = true;
            }

            if (needsUpdate) {
                feedRepository.updateCounters(
                    feed.getId(),
                    redisLikes.intValue(),
                    redisComments.intValue()
                );
                updated++;
            }
        }

        if (updated > 0) {
            log.info("피드 카운터 동기화 완료: {}개 업데이트", updated);
        }
    }

    /**
     * 댓글 카운터 동기화 (5분마다)
     */
    @Scheduled(fixedRate = 300000)
    @Transactional
    public void syncCommentCounters() {
        log.debug("댓글 카운터 동기화 시작");

        List<Comment> comments = commentRepository.findAll();
        int updated = 0;

        for (Comment comment : comments) {
            Long redisLikes = redisCounterService.getFeedLikes(comment.getId());

            if (redisLikes > 0 && !redisLikes.equals((long) comment.getLikesCount())) {
                commentRepository.updateLikesCount(comment.getId(), redisLikes.intValue());
                updated++;
            }
        }

        if (updated > 0) {
            log.info("댓글 카운터 동기화 완료: {}개 업데이트", updated);
        }
    }

    /**
     * 사용자 포인트 동기화 (10분마다)
     */
    @Scheduled(fixedRate = 600000)
    @Transactional
    public void syncUserPoints() {
        log.debug("사용자 포인트 동기화 시작");

        List<User> users = userRepository.findAll();
        int updated = 0;

        for (User user : users) {
            Long redisPoints = redisCounterService.getUserPoints(user.getId());
            Long redisMonthly = redisCounterService.getUserMonthlyPoints(user.getId());

            if (redisPoints > 0 || redisMonthly > 0) {
                userRepository.updatePoints(
                    user.getId(),
                    redisPoints > 0 ? redisPoints : user.getCurrentPoints(),
                    redisMonthly > 0 ? redisMonthly : user.getMonthlyPoints()
                );
                updated++;
            }
        }

        if (updated > 0) {
            log.info("사용자 포인트 동기화 완료: {}개 업데이트", updated);
        }
    }

    /**
     * 상품 재고 동기화 (1분마다)
     * - 재고는 중요하므로 더 자주 동기화
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void syncPrizeStock() {
        log.debug("상품 재고 동기화 시작");

        List<DrawPrize> prizes = drawPrizeRepository.findAll();
        int updated = 0;

        for (DrawPrize prize : prizes) {
            Long redisStock = redisCounterService.getPrizeStock(prize.getId());

            if (redisStock != null && redisStock >= 0) {
                if (!redisStock.equals((long) prize.getRemainingQuantity())) {
                    drawPrizeRepository.updateRemainingQuantity(prize.getId(), redisStock.intValue());
                    updated++;
                }
            }
        }

        if (updated > 0) {
            log.info("상품 재고 동기화 완료: {}개 업데이트", updated);
        }
    }

    /**
     * 랭킹 동기화 (30분마다)
     */
    @Scheduled(fixedRate = 1800000)
    @Transactional
    public void syncRankings() {
        log.debug("랭킹 동기화 시작");

        List<User> users = userRepository.findAll();

        for (User user : users) {
            redisCounterService.updatePointsRanking(user.getId(), user.getMonthlyPoints());
            redisCounterService.updateLikesRanking(user.getId(), user.getMonthlyLikes());
        }

        log.info("랭킹 동기화 완료: {}명", users.size());
    }

    /**
     * Redis 캐시 워밍업 (서버 시작 시)
     */
    @Scheduled(initialDelay = 5000, fixedDelay = Long.MAX_VALUE)
    @Transactional(readOnly = true)
    public void warmUpCache() {
        log.info("Redis 캐시 워밍업 시작");

        // 피드 카운터 로드
        List<Feed> feeds = feedRepository.findAll();
        for (Feed feed : feeds) {
            redisCounterService.setFeedLikes(feed.getId(), (long) feed.getLikesCount());
        }

        // 사용자 포인트 로드
        List<User> users = userRepository.findAll();
        for (User user : users) {
            redisCounterService.setUserPoints(
                user.getId(),
                user.getCurrentPoints(),
                user.getMonthlyPoints()
            );
            redisCounterService.updatePointsRanking(user.getId(), user.getMonthlyPoints());
            redisCounterService.updateLikesRanking(user.getId(), user.getMonthlyLikes());
        }

        // 상품 재고 로드
        List<DrawPrize> prizes = drawPrizeRepository.findAll();
        for (DrawPrize prize : prizes) {
            redisCounterService.setPrizeStock(prize.getId(), (long) prize.getRemainingQuantity());
        }

        log.info("Redis 캐시 워밍업 완료 - Feed: {}, User: {}, Prize: {}",
            feeds.size(), users.size(), prizes.size());
    }
}
