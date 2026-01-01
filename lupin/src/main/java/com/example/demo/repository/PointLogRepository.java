package com.example.demo.repository;

import com.example.demo.domain.entity.PointLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PointLogRepository extends JpaRepository<PointLog, Long> {

    // ... (기존 sumPointsByUserId 등은 유지) ...
    @Query("SELECT COALESCE(SUM(p.points), 0) FROM PointLog p WHERE p.user.id = :userId")
    Long sumPointsByUserId(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(p.points), 0) FROM PointLog p " +
            "WHERE p.user.id = :userId " +
            "AND p.type IN (com.example.demo.domain.enums.PointType.EARN, com.example.demo.domain.enums.PointType.DEDUCT) " +
            "AND p.createdAt BETWEEN :startDateTime AND :endDateTime")
    Long sumPointsByUserIdAndMonth(
            @Param("userId") Long userId,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime
    );

    // [수정 1] 랭킹 조회 (페이징 포함)
    // User.totalPoints 대신 PointLog에서 EARN, DEDUCT만 합산하여 정렬
    @Query("SELECT u, COALESCE(SUM(p.points), 0) as rankingScore " +
            "FROM User u " +
            "LEFT JOIN PointLog p ON u.id = p.user.id " +
            "AND p.type IN (com.example.demo.domain.enums.PointType.EARN, com.example.demo.domain.enums.PointType.DEDUCT) " +
            "GROUP BY u.id " +
            "ORDER BY rankingScore DESC, u.id ASC")
    List<Object[]> findUsersRankedByPoints(Pageable pageable);

    // [수정 2] 전체 랭킹 조회
    @Query("SELECT u, COALESCE(SUM(p.points), 0) as rankingScore " +
            "FROM User u " +
            "LEFT JOIN PointLog p ON u.id = p.user.id " +
            "AND p.type IN (com.example.demo.domain.enums.PointType.EARN, com.example.demo.domain.enums.PointType.DEDUCT) " +
            "GROUP BY u.id " +
            "ORDER BY rankingScore DESC, u.id ASC")
    List<Object[]> findAllUsersRankedByPoints();

    // [수정 3] 기간별 랭킹 조회 (이번 달 기준)
    @Query("SELECT u, COALESCE(SUM(p.points), 0) as rankingScore " +
            "FROM User u " +
            "LEFT JOIN PointLog p ON u.id = p.user.id " +
            "AND p.type IN (com.example.demo.domain.enums.PointType.EARN, com.example.demo.domain.enums.PointType.DEDUCT) " +
            "AND p.createdAt BETWEEN :start AND :end " + // [추가] 기간 필터링
            "GROUP BY u.id " +
            "ORDER BY rankingScore DESC, u.id ASC")
    List<Object[]> findAllUsersWithPointsRanked(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            Pageable pageable
    );

    // [수정 4] 중복된 메서드도 동일하게 수정
    @Query("SELECT u, COALESCE(SUM(p.points), 0) as rankingScore " +
            "FROM User u " +
            "LEFT JOIN PointLog p ON u.id = p.user.id " +
            "AND p.type IN (com.example.demo.domain.enums.PointType.EARN, com.example.demo.domain.enums.PointType.DEDUCT) " +
            "GROUP BY u.id " +
            "ORDER BY rankingScore DESC, u.id ASC")
    List<Object[]> findAllUsersWithPointsRankedAll();

    // 이번 달 활동한 유저 수
    @Query("SELECT COUNT(DISTINCT p.user) FROM PointLog p WHERE p.createdAt BETWEEN :startDateTime AND :endDateTime")
    Long countActiveUsersThisMonth(@Param("startDateTime") LocalDateTime startDateTime, @Param("endDateTime") LocalDateTime endDateTime);

    // [수정 5] 기간별 전체 유저 평균 포인트
    @Query(value = """
        SELECT COALESCE(AVG(sub.score), 0) 
        FROM (
            SELECT COALESCE(SUM(CASE WHEN pl.type IN ('EARN', 'DEDUCT') AND pl.created_at BETWEEN :start AND :end THEN pl.points ELSE 0 END), 0) as score
            FROM users u 
            LEFT JOIN point_logs pl ON u.id = pl.user_id 
            GROUP BY u.id
        ) sub
        """, nativeQuery = true)
    Double getAveragePointsPerUser(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    // [수정 6] 기간별 내 랭킹 및 앞뒤 유저 조회
    @Query(value = """
        WITH ranking_calc AS (
            SELECT
                u.id,
                COALESCE(SUM(CASE WHEN pl.type IN ('EARN', 'DEDUCT') AND pl.created_at BETWEEN :start AND :end THEN pl.points ELSE 0 END), 0) as score
            FROM users u
            LEFT JOIN point_logs pl ON u.id = pl.user_id
            GROUP BY u.id
        ),
        ranked_users AS (
            SELECT
                u.id,
                u.name,
                u.avatar,
                u.department,
                GREATEST(rc.score, 0) as total_points,
                ROW_NUMBER() OVER (ORDER BY rc.score DESC, u.id ASC) as user_rank
            FROM users u
            JOIN ranking_calc rc ON u.id = rc.id
        )
        SELECT r.id, r.name, r.avatar, r.department, r.total_points, r.user_rank
        FROM ranked_users r
        CROSS JOIN (
            SELECT COALESCE(MAX(user_rank), 1) as target_rank
            FROM ranked_users
            WHERE id = :userId
        ) t
        WHERE r.user_rank BETWEEN GREATEST(1, t.target_rank - 1) AND t.target_rank + 1
        ORDER BY r.user_rank
        """, nativeQuery = true)
    List<Object[]> findUserRankingContext(
            @Param("userId") Long userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    // [추가] 랭킹 복구를 위한 월간 포인트 집계 쿼리 (User 기준 LEFT JOIN으로 변경하여 0점 유저도 포함)
    @Query("SELECT u.id, COALESCE(SUM(pl.points), 0) " +
           "FROM User u " +
           "LEFT JOIN PointLog pl ON u.id = pl.user.id " +
           "AND pl.createdAt BETWEEN :start AND :end " +
           "AND pl.type IN (com.example.demo.domain.enums.PointType.EARN, com.example.demo.domain.enums.PointType.DEDUCT) " +
           "GROUP BY u.id")
    List<Object[]> sumPointsPerUser(@Param("start") LocalDateTime start, 
                                    @Param("end") LocalDateTime end);
}