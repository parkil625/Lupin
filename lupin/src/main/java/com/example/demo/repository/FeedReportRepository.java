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

    @Modifying
    @Query("DELETE FROM FeedReport fr WHERE fr.feed.id = :feedId")
    void deleteByFeedId(@Param("feedId") Long feedId);

    // [수정] 반환 타입 Long으로 변경, 쿼리 단순화 (COUNT(*) > 0 제거)
    @Query(value = "SELECT COUNT(*) FROM feed_reports WHERE reporter_id = :reporterId AND feed_id = :feedId", nativeQuery = true)
    Long countByReporterIdAndFeedId(@Param("reporterId") Long reporterId, @Param("feedId") Long feedId);

    @Query(value = "SELECT feed_id FROM feed_reports WHERE reporter_id = :reporterId AND feed_id IN :feedIds", nativeQuery = true)
    java.util.List<Long> findReportedFeedIdsByReporterId(@Param("reporterId") Long reporterId, @Param("feedIds") java.util.List<Long> feedIds);

    @Modifying
    @Query(value = "DELETE FROM feed_reports WHERE reporter_id = :reporterId AND feed_id = :feedId", nativeQuery = true)
    void deleteByReporterIdAndFeedId(@Param("reporterId") Long reporterId, @Param("feedId") Long feedId);

    long countByFeedId(Long feedId);
}