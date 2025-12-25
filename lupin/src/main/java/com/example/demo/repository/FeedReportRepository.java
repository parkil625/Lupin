package com.example.demo.repository;

import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.FeedReport;
import com.example.demo.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FeedReportRepository extends JpaRepository<FeedReport, Long> {

    long countByFeed(Feed feed);

    boolean existsByReporterAndFeed(User reporter, Feed feed);

    void deleteByReporterAndFeed(User reporter, Feed feed);

    @Modifying
    @Query("DELETE FROM FeedReport fr WHERE fr.feed = :feed")
    void deleteByFeed(@Param("feed") Feed feed);

    // [이벤트 기반 삭제] feedId로 삭제 (Soft Delete 후에도 사용 가능)
    @Modifying
    @Query("DELETE FROM FeedReport fr WHERE fr.feed.id = :feedId")
    void deleteByFeedId(@Param("feedId") Long feedId);

    // [추가] 객체 대신 ID로 카운트 조회 (Proxy 문제 해결용)
    long countByFeedId(Long feedId);

    @Query("SELECT COUNT(fr) > 0 FROM FeedReport fr WHERE fr.reporter.id = :reporterId AND fr.feed.id = :feedId")
    boolean existsByReporterIdAndFeedId(@Param("reporterId") Long reporterId, @Param("feedId") Long feedId);

    // [성능 최적화] 여러 피드에 대해 내가 신고한 피드 ID 목록 조회 (Batch Fetch)
    @Query("SELECT fr.feed.id FROM FeedReport fr WHERE fr.reporter.id = :reporterId AND fr.feed.id IN :feedIds")
    java.util.List<Long> findReportedFeedIdsByReporterId(@Param("reporterId") Long reporterId, @Param("feedIds") java.util.List<Long> feedIds);

    // [추가] 프록시 객체 비교 문제를 해결하기 위한 ID 기반 삭제 메서드
    @Modifying
    @Query("DELETE FROM FeedReport fr WHERE fr.reporter.id = :reporterId AND fr.feed.id = :feedId")
    void deleteByReporterIdAndFeedId(@Param("reporterId") Long reporterId, @Param("feedId") Long feedId);
}