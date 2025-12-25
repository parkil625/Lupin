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

    // [수정] Native Query로 변경하여 객체 매핑 문제 원천 차단 (테이블명 feed_reports 확인)
    @Query(value = "SELECT COUNT(*) > 0 FROM feed_reports WHERE reporter_id = :reporterId AND feed_id = :feedId", nativeQuery = true)
    boolean existsByReporterIdAndFeedId(@Param("reporterId") Long reporterId, @Param("feedId") Long feedId);

    // [수정] 목록 조회용 Native Query
    @Query(value = "SELECT feed_id FROM feed_reports WHERE reporter_id = :reporterId AND feed_id IN :feedIds", nativeQuery = true)
    java.util.List<Long> findReportedFeedIdsByReporterId(@Param("reporterId") Long reporterId, @Param("feedIds") java.util.List<Long> feedIds);

    // [수정] 삭제도 Native Query로 확실하게 처리
    @Modifying
    @Query(value = "DELETE FROM feed_reports WHERE reporter_id = :reporterId AND feed_id = :feedId", nativeQuery = true)
    void deleteByReporterIdAndFeedId(@Param("reporterId") Long reporterId, @Param("feedId") Long feedId);

    long countByFeedId(Long feedId);
}