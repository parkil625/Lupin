package com.example.demo.repository.custom;

import com.example.demo.domain.entity.Feed;
import com.example.demo.dto.response.FeedListResponse;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static com.example.demo.domain.entity.QFeed.feed;
import static com.example.demo.domain.entity.QFeedImage.feedImage;
import static com.example.demo.domain.entity.QFeedLike.feedLike;
import static com.example.demo.domain.entity.QUser.user;

@Repository
@RequiredArgsConstructor
public class FeedRepositoryImpl implements FeedRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<FeedListResponse> searchFeeds(String keyword, String activityType, Long excludeUserId, Long excludeFeedId, Pageable pageable) {
        // Feed 엔티티를 이미지와 함께 조회
        List<Feed> feeds = queryFactory
                .selectFrom(feed)
                .distinct()
                .leftJoin(feed.writer, user).fetchJoin()
                .leftJoin(feed.images, feedImage).fetchJoin()
                .where(
                        containsKeyword(keyword),
                        eqActivityType(activityType),
                        excludeWriter(excludeUserId),
                        excludeFeed(excludeFeedId)
                )
                .orderBy(getOrderSpecifier(pageable))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // [수정] FeedListResponse.from() 메서드를 사용하여 DTO 변환 (변수명 불일치 해결)
        List<FeedListResponse> content = feeds.stream()
                .map(FeedListResponse::from)
                .collect(Collectors.toList());

        JPAQuery<Long> countQuery = queryFactory
                .select(feed.count())
                .from(feed)
                .where(
                        containsKeyword(keyword),
                        eqActivityType(activityType),
                        excludeWriter(excludeUserId),
                        excludeFeed(excludeFeedId)
                );

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    @Override
    public Page<FeedListResponse> findByWriterId(Long userId, Pageable pageable) {
        // Feed 엔티티를 이미지와 함께 조회
        List<Feed> feeds = queryFactory
                .selectFrom(feed)
                .distinct()
                .leftJoin(feed.writer, user).fetchJoin()
                .leftJoin(feed.images, feedImage).fetchJoin()
                .where(feed.writer.id.eq(userId))
                .orderBy(getOrderSpecifier(pageable))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // [수정] FeedListResponse.from() 메서드를 사용하여 DTO 변환
        List<FeedListResponse> content = feeds.stream()
                .map(FeedListResponse::from)
                .collect(Collectors.toList());

        JPAQuery<Long> countQuery = queryFactory
                .select(feed.count())
                .from(feed)
                .where(feed.writer.id.eq(userId));

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    @Override
    public List<Feed> findFeedsBetween(LocalDateTime startDate, LocalDateTime endDate) {
        return queryFactory
                .selectFrom(feed)
                .leftJoin(feed.writer, user).fetchJoin()
                .where(feed.createdAt.between(startDate, endDate))
                .orderBy(feed.createdAt.desc())
                .fetch();
    }

    @Override
    public List<Feed> findPopularFeeds(int limit) {
        return queryFactory
                .selectFrom(feed)
                .leftJoin(feed.writer, user).fetchJoin()
                .leftJoin(feed.likes, feedLike)
                .groupBy(feed.id)
                .orderBy(feedLike.count().desc(), feed.createdAt.desc())
                .limit(limit)
                .fetch();
    }

    @Override
    public Long countUserFeeds(Long userId) {
        return queryFactory
                .select(feed.count())
                .from(feed)
                .where(feed.writer.id.eq(userId))
                .fetchOne();
    }

    @Override
    public Integer countUserActiveDaysInCurrentMonth(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime endOfMonth = now.withDayOfMonth(now.toLocalDate().lengthOfMonth()).withHour(23).withMinute(59).withSecond(59).withNano(999999999);

        List<Feed> feeds = queryFactory
                .selectFrom(feed)
                .where(
                        feed.writer.id.eq(userId),
                        feed.createdAt.between(startOfMonth, endOfMonth)
                )
                .fetch();

        long distinctDays = feeds.stream()
                .map(f -> f.getCreatedAt().toLocalDate())
                .distinct()
                .count();

        return (int) distinctDays;
    }

    @Override
    public boolean hasUserPostedToday(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = now.withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime endOfDay = now.withHour(23).withMinute(59).withSecond(59).withNano(999999999);

        Long count = queryFactory
                .select(feed.count())
                .from(feed)
                .where(
                        feed.writer.id.eq(userId),
                        feed.createdAt.between(startOfDay, endOfDay)
                )
                .fetchOne();

        return count != null && count > 0;
    }

    // === 동적 쿼리 조건 메서드 ===

    private BooleanExpression containsKeyword(String keyword) {
        return keyword != null ? feed.content.containsIgnoreCase(keyword) : null;
    }

    private BooleanExpression eqActivityType(String activityType) {
        return activityType != null ? feed.activityType.eq(activityType) : null;
    }

    private BooleanExpression excludeWriter(Long excludeUserId) {
        return excludeUserId != null ? feed.writer.id.ne(excludeUserId) : null;
    }

    private BooleanExpression excludeFeed(Long excludeFeedId) {
        return excludeFeedId != null ? feed.id.ne(excludeFeedId) : null;
    }

    private OrderSpecifier<?> getOrderSpecifier(Pageable pageable) {
        if (!pageable.getSort().isEmpty()) {
            return pageable.getSort().stream()
                    .findFirst()
                    .<OrderSpecifier<?>>map(order -> {
                        if (order.getProperty().equals("createdAt")) {
                            return order.isAscending() ? feed.createdAt.asc() : feed.createdAt.desc();
                        }
                        return feed.id.desc();
                    })
                    .orElse(feed.id.desc());
        }
        return feed.id.desc();
    }
}