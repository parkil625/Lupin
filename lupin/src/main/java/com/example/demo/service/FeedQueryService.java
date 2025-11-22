package com.example.demo.service;

import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.QFeed;
import com.example.demo.domain.entity.QFeedImage;
import com.example.demo.domain.entity.QFeedLike;
import com.example.demo.domain.entity.QUser;
import com.example.demo.dto.response.FeedDetailResponse;
import com.example.demo.dto.response.FeedListResponse;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.FeedLikeRepository;
import com.example.demo.repository.FeedRepository;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 피드 Query 서비스 (읽기 전용)
 * CQRS 패턴 - 데이터 조회 작업 담당
 * QueryDSL을 사용하여 최적화된 DTO 직접 조회
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedQueryService {

    private final JPAQueryFactory queryFactory;
    private final FeedRepository feedRepository;
    private final FeedLikeRepository feedLikeRepository;

    private final QFeed feed = QFeed.feed;
    private final QUser user = QUser.user;
    private final QFeedLike feedLike = QFeedLike.feedLike;

    /**
     * 피드 목록 조회 (최적화된 DTO 직접 조회)
     */
    public Page<FeedListResponse> getFeeds(String keyword, String activityType,
                                           Long excludeUserId, Long excludeFeedId,
                                           Pageable pageable) {

        List<FeedListResponse> content = queryFactory
                .select(Projections.constructor(FeedListResponse.class,
                        feed.id,
                        feed.activityType,
                        feed.calories,
                        feed.content,
                        feed.earnedPoints,
                        feed.createdAt,
                        user.id,
                        user.realName,
                        feedLike.count().intValue()
                ))
                .from(feed)
                .join(feed.writer, user)
                .leftJoin(feedLike).on(feedLike.feed.eq(feed))
                .where(
                        keywordContains(keyword),
                        activityTypeEquals(activityType),
                        userIdNotEquals(excludeUserId),
                        feedIdNotEquals(excludeFeedId)
                )
                .groupBy(feed.id)
                .orderBy(feed.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(feed.count())
                .from(feed)
                .where(
                        keywordContains(keyword),
                        activityTypeEquals(activityType),
                        userIdNotEquals(excludeUserId),
                        feedIdNotEquals(excludeFeedId)
                )
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    /**
     * 특정 사용자의 피드 조회
     */
    public Page<FeedListResponse> getFeedsByUserId(Long userId, Pageable pageable) {
        List<FeedListResponse> content = queryFactory
                .select(Projections.constructor(FeedListResponse.class,
                        feed.id,
                        feed.activityType,
                        feed.calories,
                        feed.content,
                        feed.earnedPoints,
                        feed.createdAt,
                        user.id,
                        user.realName,
                        feedLike.count().intValue()
                ))
                .from(feed)
                .join(feed.writer, user)
                .leftJoin(feedLike).on(feedLike.feed.eq(feed))
                .where(user.id.eq(userId))
                .groupBy(feed.id)
                .orderBy(feed.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(feed.count())
                .from(feed)
                .where(feed.writer.id.eq(userId))
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    /**
     * 피드 상세 조회
     */
    public FeedDetailResponse getFeedDetail(Long feedId) {
        Feed feedEntity = feedRepository.findById(feedId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FEED_NOT_FOUND));

        return FeedDetailResponse.from(feedEntity);
    }

    /**
     * 인기 피드 조회
     */
    public List<Feed> getPopularFeeds(int limit) {
        return feedRepository.findPopularFeeds(limit);
    }

    /**
     * 오늘 피드 작성 가능 여부 확인
     */
    public boolean canPostToday(Long userId) {
        return !feedRepository.hasUserPostedToday(userId);
    }

    /**
     * 좋아요 여부 확인
     */
    public boolean hasUserLikedFeed(Long userId, Long feedId) {
        return feedLikeRepository.existsByUserIdAndFeedId(userId, feedId);
    }

    // === Dynamic Query Conditions ===

    private BooleanExpression keywordContains(String keyword) {
        return keyword != null ? feed.content.containsIgnoreCase(keyword) : null;
    }

    private BooleanExpression activityTypeEquals(String activityType) {
        return activityType != null ? feed.activityType.eq(activityType) : null;
    }

    private BooleanExpression userIdNotEquals(Long excludeUserId) {
        return excludeUserId != null ? feed.writer.id.ne(excludeUserId) : null;
    }

    private BooleanExpression feedIdNotEquals(Long excludeFeedId) {
        return excludeFeedId != null ? feed.id.ne(excludeFeedId) : null;
    }
}
