package com.example.demo.repository;

import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FeedRepository extends JpaRepository<Feed, Long>, FeedRepositoryCustom {

    // [최적화] writer만 JOIN FETCH, images는 BatchSize(100)로 처리
    @Query("SELECT f FROM Feed f JOIN FETCH f.writer WHERE f.writer = :writer ORDER BY f.id DESC")
    Slice<Feed> findByWriterOrderByIdDesc(@Param("writer") User writer, Pageable pageable);

    // [최적화] 홈 피드 - 본인 제외
    @Query("SELECT f FROM Feed f JOIN FETCH f.writer WHERE f.writer <> :writer ORDER BY f.id DESC")
    Slice<Feed> findByWriterNotOrderByIdDesc(@Param("writer") User writer, Pageable pageable);

    // [최적화] 이름 검색
    @Query("SELECT f FROM Feed f JOIN FETCH f.writer WHERE f.writer.name LIKE %:name% ORDER BY f.id DESC")
    Slice<Feed> findByWriterNameContainingOrderByIdDesc(@Param("name") String name, Pageable pageable);

    // [최적화] 오늘 글 존재 여부 - exists 사용
    boolean existsByWriterAndCreatedAtBetween(User writer, LocalDateTime start, LocalDateTime end);

    // [최적화] 상세 조회 - writer만 페치
    @Query("SELECT f FROM Feed f JOIN FETCH f.writer WHERE f.id = :id")
    Optional<Feed> findByIdWithWriter(@Param("id") Long id);

    // [최적화] 상세 조회 - writer와 images 함께 페치 (EntityGraph 사용)
    @EntityGraph(attributePaths = {"writer", "images"})
    @Query("SELECT f FROM Feed f WHERE f.id = :id")
    Optional<Feed> findByIdWithWriterAndImages(@Param("id") Long id);

    // [삭제용] writer와 images 함께 페치 - cascade delete 지원
    @Query("SELECT f FROM Feed f JOIN FETCH f.writer LEFT JOIN FETCH f.images WHERE f.id = :id")
    Optional<Feed> findByIdForDelete(@Param("id") Long id);

    // 기본 findById는 JpaRepository에서 제공

    // [activeDays] 사용자의 이번 달 피드 작성 고유 날짜 수
    @Query("SELECT COUNT(DISTINCT CAST(f.createdAt AS LocalDate)) FROM Feed f WHERE f.writer = :writer AND f.createdAt BETWEEN :start AND :end")
    int countDistinctDaysByWriterAndCreatedAtBetween(@Param("writer") User writer, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // [동시성] 좋아요 카운트 원자적 증가
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Feed f SET f.likeCount = f.likeCount + 1 WHERE f.id = :feedId")
    void incrementLikeCount(@Param("feedId") Long feedId);

    // [동시성] 좋아요 카운트 원자적 감소
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Feed f SET f.likeCount = CASE WHEN f.likeCount > 0 THEN f.likeCount - 1 ELSE 0 END WHERE f.id = :feedId")
    void decrementLikeCount(@Param("feedId") Long feedId);

    // [동시성] 댓글 카운트 원자적 증가
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Feed f SET f.commentCount = f.commentCount + 1 WHERE f.id = :feedId")
    void incrementCommentCount(@Param("feedId") Long feedId);

    // [동시성] 댓글 카운트 원자적 감소
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Feed f SET f.commentCount = CASE WHEN f.commentCount > 0 THEN f.commentCount - 1 ELSE 0 END WHERE f.id = :feedId")
    void decrementCommentCount(@Param("feedId") Long feedId);

    // 사용자별 피드 수 조회
    long countByWriterId(Long writerId);
}
