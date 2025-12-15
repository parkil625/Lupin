package com.example.demo.repository;

import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.FeedImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeedImageRepository extends JpaRepository<FeedImage, Long> {

    @Modifying
    @Query("DELETE FROM FeedImage fi WHERE fi.feed = :feed")
    void deleteByFeed(@Param("feed") Feed feed);

    /**
     * 모든 S3 키 조회 (고아 이미지 정리용)
     */
    @Query("SELECT fi.s3Key FROM FeedImage fi")
    List<String> findAllS3Keys();
}
