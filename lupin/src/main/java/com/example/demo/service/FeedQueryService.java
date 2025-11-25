package com.example.demo.service;

import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.QFeed;
import com.example.demo.domain.entity.QFeedLike;
import com.example.demo.domain.entity.QUser;
import com.example.demo.dto.response.FeedDetailResponse;
import com.example.demo.dto.response.FeedListResponse;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.FeedLikeRepository;
import com.example.demo.repository.FeedRepository;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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
     * 피드 목록 조회
     * Pageable의 Sort를 존중하여 동적 정렬 적용
     */
    public Page<FeedListResponse> getFeeds(String keyword, String activityType,
                                           Long excludeUserId, Long excludeFeedId,
                                           Pageable pageable) {

        List<Feed> feeds = queryFactory
                .selectFrom(feed)
                .distinct()
                .leftJoin(feed.writer, user).fetchJoin()
                .where(
                        keywordContains(keyword),
                        activityTypeEquals(activityType),
                        userIdNotEquals(excludeUserId),
                        feedIdNotEquals(excludeFeedId)
                )
                .orderBy(getOrderSpecifiers(pageable))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        List<FeedListResponse> content = feeds.stream()
                .map(FeedListResponse::from)
                .collect(java.util.stream.Collectors.toList());

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
     * Pageable의 Sort를 존중하여 동적 정렬 적용
     */
    public Page<FeedListResponse> getFeedsByUserId(Long userId, Pageable pageable) {
        List<Feed> feeds = queryFactory
                .selectFrom(feed)
                .distinct()
                .leftJoin(feed.writer, user).fetchJoin()
                .where(user.id.eq(userId))
                .orderBy(getOrderSpecifiers(pageable))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        List<FeedListResponse> content = feeds.stream()
                .map(FeedListResponse::from)
                .collect(java.util.stream.Collectors.toList());

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

    /**
     * Pageable의 Sort를 QueryDSL OrderSpecifier로 변환
     * 기본 정렬: createdAt DESC
     *
     * 지원되는 정렬 필드: id, createdAt, calories, earnedPoints
     */
    private OrderSpecifier<?>[] getOrderSpecifiers(Pageable pageable) {
        List<OrderSpecifier<?>> orders = new ArrayList<>();

        if (pageable.getSort().isSorted()) {
            for (Sort.Order order : pageable.getSort()) {
                String property = order.getProperty();
                boolean isAscending = order.isAscending();

                switch (property) {
                    case "id":
                        orders.add(isAscending ? feed.id.asc() : feed.id.desc());
                        break;
                    case "createdAt":
                        orders.add(isAscending ? feed.createdAt.asc() : feed.createdAt.desc());
                        break;
                    case "calories":
                        orders.add(isAscending ? feed.calories.asc() : feed.calories.desc());
                        break;
                    case "earnedPoints":
                        orders.add(isAscending ? feed.earnedPoints.asc() : feed.earnedPoints.desc());
                        break;
                    default:
                        // 기본 정렬
                        orders.add(feed.createdAt.desc());
                        break;
                }
            }
        } else {
            // Sort 미지정 시 기본 정렬
            orders.add(feed.createdAt.desc());
        }

        return orders.toArray(new OrderSpecifier[0]);
    }
}
