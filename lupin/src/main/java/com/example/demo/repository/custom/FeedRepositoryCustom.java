package com.example.demo.repository.custom;

import com.example.demo.domain.entity.Feed;
import com.example.demo.dto.response.FeedListResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Feed 엔티티에 대한 QueryDSL 커스텀 Repository
 */
public interface FeedRepositoryCustom {

    /**
     * 피드 목록 조회 (페이징, 검색, 정렬 포함)
     */
    Page<FeedListResponse> searchFeeds(String keyword, String activityType, Pageable pageable);

    /**
     * 특정 기간 동안의 피드 조회
     */
    List<Feed> findFeedsBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * 인기 피드 조회 (좋아요 수 기준)
     */
    List<Feed> findPopularFeeds(int limit);

    /**
     * 사용자의 피드 통계 조회
     */
    Long countUserFeeds(Long userId);

    /**
     * 사용자의 총 활동 시간 조회 (분 단위)
     */
    Long sumUserActivityDuration(Long userId);
}
