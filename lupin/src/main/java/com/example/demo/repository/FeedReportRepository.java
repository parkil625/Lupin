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

    // [수정] JPQL로 변경하여 Type Safe하게 조회 (새로고침 시 상태 유지 버그 해결)
    @Query("SELECT COUNT(fr) FROM FeedReport fr WHERE fr.reporter.id = :reporterId AND fr.feed.id = :feedId")
    long countByReporterIdAndFeedId(@Param("reporterId") Long reporterId, @Param("feedId") Long feedId);

    @Query("SELECT fr.feed.id FROM FeedReport fr WHERE fr.reporter.id = :reporterId AND fr.feed.id IN :feedIds")
    java.util.List<Long> findReportedFeedIdsByReporterId(@Param("reporterId") Long reporterId, @Param("feedIds") java.util.List<Long> feedIds);

    @Modifying
    @Query("DELETE FROM FeedReport fr WHERE fr.reporter.id = :reporterId AND fr.feed.id = :feedId")
    void deleteByReporterIdAndFeedId(@Param("reporterId") Long reporterId, @Param("feedId") Long feedId);

    long countByFeedId(Long feedId);
}