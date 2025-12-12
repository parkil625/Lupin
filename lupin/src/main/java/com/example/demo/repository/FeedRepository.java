package com.example.demo.repository;

import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface FeedRepository extends JpaRepository<Feed, Long> {

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

    // [삭제용] writer와 images 함께 페치 - cascade delete 지원
    @Query("SELECT f FROM Feed f JOIN FETCH f.writer LEFT JOIN FETCH f.images WHERE f.id = :id")
    Optional<Feed> findByIdForDelete(@Param("id") Long id);

    // 기본 findById는 JpaRepository에서 제공
}
