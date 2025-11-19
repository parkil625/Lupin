package com.example.demo.repository;

import com.example.demo.domain.entity.Feed;
import com.example.demo.repository.custom.FeedRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeedRepository extends JpaRepository<Feed, Long>, FeedRepositoryCustom {

    // 사용자의 피드 목록 조회
    @Query("SELECT f FROM Feed f " +
           "LEFT JOIN FETCH f.writer " +
           "WHERE f.writer.id = :userId " +
           "ORDER BY f.createdAt DESC")
    List<Feed> findByUserId(@Param("userId") Long userId);

    // 특정 활동 타입의 피드 조회
    List<Feed> findByActivityTypeOrderByCreatedAtDesc(String activityType);
}
