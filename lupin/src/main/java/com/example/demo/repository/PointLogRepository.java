package com.example.demo.repository;

import com.example.demo.domain.entity.PointLog;
import com.example.demo.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;

@Repository
public interface PointLogRepository extends JpaRepository<PointLog, Long> {

    @Query("SELECT COALESCE(SUM(p.points), 0) FROM PointLog p WHERE p.user = :user")
    Long sumPointsByUser(@Param("user") User user);

    @Query("SELECT COALESCE(SUM(p.points), 0) FROM PointLog p WHERE p.user = :user AND p.createdAt BETWEEN :startDateTime AND :endDateTime")
    Long sumPointsByUserAndMonth(@Param("user") User user, @Param("startDateTime") LocalDateTime startDateTime, @Param("endDateTime") LocalDateTime endDateTime);

    @Query("SELECT p.user, COALESCE(SUM(p.points), 0) as totalPoints FROM PointLog p GROUP BY p.user ORDER BY totalPoints DESC")
    List<Object[]> findUsersRankedByPoints(Pageable pageable);

    @Query("SELECT p.user, COALESCE(SUM(p.points), 0) as totalPoints FROM PointLog p GROUP BY p.user ORDER BY totalPoints DESC")
    List<Object[]> findAllUsersRankedByPoints();

    @Query("SELECT u, COALESCE(SUM(p.points), 0) as totalPoints FROM User u LEFT JOIN PointLog p ON p.user = u GROUP BY u ORDER BY totalPoints DESC, u.id ASC")
    List<Object[]> findAllUsersWithPointsRanked(Pageable pageable);

    @Query("SELECT u, COALESCE(SUM(p.points), 0) as totalPoints FROM User u LEFT JOIN PointLog p ON p.user = u GROUP BY u ORDER BY totalPoints DESC, u.id ASC")
    List<Object[]> findAllUsersWithPointsRankedAll();

    // 이번 달 활동한 유저 수 (PointLog 기록이 있는 유저)
    @Query("SELECT COUNT(DISTINCT p.user) FROM PointLog p WHERE p.createdAt BETWEEN :startDateTime AND :endDateTime")
    Long countActiveUsersThisMonth(@Param("startDateTime") LocalDateTime startDateTime, @Param("endDateTime") LocalDateTime endDateTime);

    // 전체 유저 평균 포인트 (Native Query)
    @Query(value = "SELECT COALESCE(AVG(total_points), 0) FROM (SELECT COALESCE(SUM(p.points), 0) as total_points FROM users u LEFT JOIN point_logs p ON p.user_id = u.id GROUP BY u.id) as user_totals", nativeQuery = true)
    Double getAveragePointsPerUser();

    /**
     * 특정 사용자의 랭킹과 앞뒤 사용자를 조회 (Window Function 사용)
     * 전체 사용자를 메모리에 로드하지 않고 DB에서 필요한 데이터만 조회
     */
    @Query(value = """
        WITH ranked_users AS (
            SELECT
                u.id,
                u.name,
                u.avatar,
                u.department,
                COALESCE(SUM(p.points), 0) as total_points,
                ROW_NUMBER() OVER (ORDER BY COALESCE(SUM(p.points), 0) DESC, u.id ASC) as user_rank
            FROM users u
            LEFT JOIN point_logs p ON p.user_id = u.id
            GROUP BY u.id, u.name, u.avatar, u.department
        ),
        target_rank AS (
            SELECT user_rank FROM ranked_users WHERE id = :userId
        )
        SELECT r.id, r.name, r.avatar, r.department, r.total_points, r.user_rank
        FROM ranked_users r, target_rank t
        WHERE r.user_rank BETWEEN GREATEST(1, t.user_rank - 1) AND t.user_rank + 1
        ORDER BY r.user_rank
        """, nativeQuery = true)
    List<Object[]> findUserRankingContext(@Param("userId") Long userId);
}
