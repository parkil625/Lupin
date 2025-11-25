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
import static com.example.demo.domain.entity.QUser.user;

@Repository
@RequiredArgsConstructor
public class FeedRepositoryImpl implements FeedRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<FeedListResponse> searchFeeds(String keyword, String activityType, Long excludeUserId, Long excludeFeedId, Pageable pageable) {
        List<Feed> feeds = queryFactory
                .selectFrom(feed)
                .distinct()
                // [수정] fetchJoin 제거 (배치 사이즈로 해결)
                .leftJoin(feed.writer, user)
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
        List<Feed> feeds = queryFactory
                .selectFrom(feed)
                .distinct()
                // [수정] fetchJoin 제거
                .leftJoin(feed.writer, user)
                .where(feed.writer.id.eq(userId))
                .orderBy(getOrderSpecifier(pageable))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

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
                .leftJoin(feed.writer, user).fetchJoin() // 여긴 리스트 조회가 아니니 fetchJoin 유지해도 됨
                .where(feed.createdAt.between(startDate, endDate))
                .orderBy(feed.createdAt.desc())
                .fetch();
    }

    @Override
    public List<Feed> findPopularFeeds(int limit) {
        return queryFactory
                .selectFrom(feed)
                .leftJoin(feed.writer, user).fetchJoin()
                // .leftJoin(feed.likes, feedLike) // groupBy 이슈로 제거하고 배치사이즈에 맡김
                .orderBy(feed.earnedPoints.desc()) // 좋아요 수 대신 포인트나 최신순 정렬 권장 (간단화)
                .limit(limit)
                .fetch();
    }

    // ... (나머지 count 메서드들은 그대로 유지) ...

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
                        } else if (order.getProperty().equals("id")) {
                            return order.isAscending() ? feed.id.asc() : feed.id.desc();
                        }
                        return feed.id.desc();
                    })
                    .orElse(feed.id.desc());
        }
        return feed.id.desc();
    }
}