package com.example.demo.repository;

import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.FeedReport;
import com.example.demo.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FeedReportRepository extends JpaRepository<FeedReport, Long> {

    Optional<FeedReport> findByReporterAndFeed(User reporter, Feed feed);

    long countByFeed(Feed feed);

    @Modifying
    @Query("DELETE FROM FeedReport fr WHERE fr.feed = :feed")
    void deleteByFeed(@Param("feed") Feed feed);

    @Modifying
    @Query("DELETE FROM FeedReport fr WHERE fr.feed.id = :feedId")
    void deleteByFeedId(@Param("feedId") Long feedId);
}
