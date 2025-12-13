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

    // userId만 사용하여 detached entity 문제 방지
    @Query("SELECT COALESCE(SUM(p.points), 0) FROM PointLog p WHERE p.user.id = :userId")
    Long sumPointsByUserId(@Param("userId") Long userId);

    // userId만 사용하여 detached entity 문제 방지
    @Query("SELECT COALESCE(SUM(p.points), 0) FROM PointLog p WHERE p.user.id = :userId AND p.createdAt BETWEEN :startDateTime AND :endDateTime")
    Long sumPointsByUserIdAndMonth(@Param("userId") Long userId, @Param("startDateTime") LocalDateTime startDateTime, @Param("endDateTime") LocalDateTime endDateTime);

    @Query("SELECT u, u.totalPoints as totalPoints FROM User u ORDER BY u.totalPoints DESC, u.id ASC")
    List<Object[]> findUsersRankedByPoints(Pageable pageable);

    @Query("SELECT u, u.totalPoints as totalPoints FROM User u ORDER BY u.totalPoints DESC, u.id ASC")
    List<Object[]> findAllUsersRankedByPoints();

    @Query("SELECT u, u.totalPoints as totalPoints FROM User u ORDER BY u.totalPoints DESC, u.id ASC")
    List<Object[]> findAllUsersWithPointsRanked(Pageable pageable);

    @Query("SELECT u, u.totalPoints as totalPoints FROM User u ORDER BY u.totalPoints DESC, u.id ASC")
    List<Object[]> findAllUsersWithPointsRankedAll();

    // 이번 달 활동한 유저 수 (PointLog 기록이 있는 유저)
    @Query("SELECT COUNT(DISTINCT p.user) FROM PointLog p WHERE p.createdAt BETWEEN :startDateTime AND :endDateTime")
    Long countActiveUsersThisMonth(@Param("startDateTime") LocalDateTime startDateTime, @Param("endDateTime") LocalDateTime endDateTime);

    // 전체 유저 평균 포인트 (User.totalPoints 사용)
    @Query(value = "SELECT COALESCE(AVG(u.total_points), 0) FROM users u", nativeQuery = true)
    Double getAveragePointsPerUser();

    /**
     * 특정 사용자의 랭킹과 앞뒤 사용자를 조회 (Window Function 사용)
     * User.totalPoints 반정규화 필드를 사용하여 JOIN 없이 조회
     * COALESCE로 NULL 값 방어 처리
     */
    @Query(value = """
        WITH ranked_users AS (
            SELECT
                u.id,
                u.name,
                u.avatar,
                u.department,
                COALESCE(u.total_points, 0) as total_points,
                ROW_NUMBER() OVER (ORDER BY COALESCE(u.total_points, 0) DESC, u.id ASC) as user_rank
            FROM users u
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
