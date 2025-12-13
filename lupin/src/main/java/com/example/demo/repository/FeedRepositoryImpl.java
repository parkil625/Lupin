package com.example.demo.repository;

import com.example.demo.domain.entity.QFeed;
import com.example.demo.dto.WriterActiveDays;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * QueryDSL을 사용한 Feed Repository 구현체
 */
@RequiredArgsConstructor
public class FeedRepositoryImpl implements FeedRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<WriterActiveDays> findActiveDaysByWriterIds(
            List<Long> writerIds, LocalDateTime start, LocalDateTime end) {

        QFeed feed = QFeed.feed;

        return queryFactory
                .select(Projections.constructor(WriterActiveDays.class,
                        feed.writer.id,
                        feed.createdAt.dayOfMonth().countDistinct()))
                .from(feed)
                .where(
                        feed.writer.id.in(writerIds),
                        feed.createdAt.between(start, end)
                )
                .groupBy(feed.writer.id)
                .fetch();
    }
}
