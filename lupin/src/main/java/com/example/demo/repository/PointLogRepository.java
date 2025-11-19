package com.example.demo.repository;

import com.example.demo.domain.entity.PointLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PointLogRepository extends JpaRepository<PointLog, Long> {

    /**
     * 특정 사용자의 포인트 로그 조회 (페이징)
     */
    @Query("SELECT pl FROM PointLog pl WHERE pl.user.id = :userId ORDER BY pl.createdAt DESC")
    Page<PointLog> findByUserId(@Param("userId") Long userId, Pageable pageable);

    /**
     * 특정 사용자의 포인트 로그 조회 (전체)
     */
    @Query("SELECT pl FROM PointLog pl WHERE pl.user.id = :userId ORDER BY pl.createdAt DESC")
    List<PointLog> findByUserId(@Param("userId") Long userId);

    /**
     * 특정 사용자의 포인트 합계 조회
     */
    @Query("SELECT COALESCE(SUM(pl.amount), 0) FROM PointLog pl WHERE pl.user.id = :userId")
    Long sumPointsByUserId(@Param("userId") Long userId);

    /**
     * 특정 기간 내 포인트 로그 조회
     */
    @Query("SELECT pl FROM PointLog pl WHERE pl.user.id = :userId AND pl.createdAt BETWEEN :startDate AND :endDate ORDER BY pl.createdAt DESC")
    List<PointLog> findByUserIdAndDateRange(@Param("userId") Long userId,
                                             @Param("startDate") LocalDateTime startDate,
                                             @Param("endDate") LocalDateTime endDate);

    /**
     * 특정 사유의 포인트 로그 조회
     */
    @Query("SELECT pl FROM PointLog pl WHERE pl.user.id = :userId AND pl.reason LIKE %:reason% ORDER BY pl.createdAt DESC")
    Page<PointLog> findByUserIdAndReason(@Param("userId") Long userId, @Param("reason") String reason, Pageable pageable);

    /**
     * 특정 참조 ID의 포인트 로그 조회
     */
    @Query("SELECT pl FROM PointLog pl WHERE pl.refId = :refId ORDER BY pl.createdAt DESC")
    List<PointLog> findByRefId(@Param("refId") String refId);

    /**
     * 특정 기간 내 포인트 합계 조회
     */
    @Query("SELECT COALESCE(SUM(pl.amount), 0) FROM PointLog pl WHERE pl.user.id = :userId AND pl.createdAt BETWEEN :startDate AND :endDate")
    Long sumPointsByUserIdAndDateRange(@Param("userId") Long userId,
                                        @Param("startDate") LocalDateTime startDate,
                                        @Param("endDate") LocalDateTime endDate);

    /**
     * 최근 포인트 로그 조회
     */
    @Query("SELECT pl FROM PointLog pl WHERE pl.user.id = :userId ORDER BY pl.createdAt DESC")
    List<PointLog> findRecentPointLogsByUserId(@Param("userId") Long userId, Pageable pageable);

    /**
     * 특정 사용자의 포인트 로그 수 조회
     */
    @Query("SELECT COUNT(pl) FROM PointLog pl WHERE pl.user.id = :userId")
    Long countByUserId(@Param("userId") Long userId);
}
