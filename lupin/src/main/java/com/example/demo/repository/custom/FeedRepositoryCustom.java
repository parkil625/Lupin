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
    Page<FeedListResponse> searchFeeds(String keyword, String activityType, Long excludeUserId, Pageable pageable);

    /**
     * 특정 사용자의 피드 조회
     */
    Page<FeedListResponse> findByWriterId(Long userId, Pageable pageable);

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
     * 사용자의 이번 달 활동 일수 조회 (피드 작성한 날짜 수)
     */
    Integer countUserActiveDaysInCurrentMonth(Long userId);

    /**
     * 사용자가 오늘 피드를 작성했는지 확인
     */
    boolean hasUserPostedToday(Long userId);
}
