package com.example.demo.repository.custom;

import com.example.demo.domain.entity.Feed;
import com.example.demo.dto.response.FeedListResponse;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
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

import static com.example.demo.domain.entity.QFeed.feed;
import static com.example.demo.domain.entity.QFeedLike.feedLike;
import static com.example.demo.domain.entity.QUser.user;

@Repository
@RequiredArgsConstructor
public class FeedRepositoryImpl implements FeedRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<FeedListResponse> searchFeeds(String keyword, String activityType, Pageable pageable) {
        List<FeedListResponse> content = queryFactory
                .select(Projections.constructor(FeedListResponse.class,
                        feed.id,
                        feed.writer.realName,
                        feed.activityType,
                        feed.duration,
                        feed.content,
                        feed.createdAt,
                        feed.likes.size(),
                        feed.comments.size()
                ))
                .from(feed)
                .leftJoin(feed.writer, user)
                .where(
                        containsKeyword(keyword),
                        eqActivityType(activityType)
                )
                .orderBy(getOrderSpecifier(pageable))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(feed.count())
                .from(feed)
                .where(
                        containsKeyword(keyword),
                        eqActivityType(activityType)
                );

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
    public Long sumUserActivityDuration(Long userId) {
        Integer sum = queryFactory
                .select(feed.duration.sum())
                .from(feed)
                .where(feed.writer.id.eq(userId))
                .fetchOne();
        return sum != null ? sum.longValue() : 0L;
    }

    // === 동적 쿼리 조건 메서드 ===

    private BooleanExpression containsKeyword(String keyword) {
        return keyword != null ? feed.content.containsIgnoreCase(keyword) : null;
    }

    private BooleanExpression eqActivityType(String activityType) {
        return activityType != null ? feed.activityType.eq(activityType) : null;
    }

    private OrderSpecifier<?> getOrderSpecifier(Pageable pageable) {
        if (!pageable.getSort().isEmpty()) {
            // Pageable의 정렬 정보 사용
            return pageable.getSort().stream()
                    .findFirst()
                    .map(order -> {
                        if (order.getProperty().equals("createdAt")) {
                            return order.isAscending() ? feed.createdAt.asc() : feed.createdAt.desc();
                        }
                        return feed.createdAt.desc();
                    })
                    .orElse(feed.createdAt.desc());
        }
        return feed.createdAt.desc();
    }
}
