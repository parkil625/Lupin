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

    // type이 USE가 아닌 것들(EARN, DEDUCT)만 합산하여 랭킹 점수 산출
    @Query("SELECT COALESCE(SUM(p.points), 0) FROM PointLog p " +
           "WHERE p.user.id = :userId " +
           "AND p.type IN (com.example.demo.domain.enums.PointType.EARN, com.example.demo.domain.enums.PointType.DEDUCT) " +
           "AND p.createdAt BETWEEN :startDateTime AND :endDateTime")
    Long sumPointsByUserIdAndMonth(
            @Param("userId") Long userId, 
            @Param("startDateTime") LocalDateTime startDateTime, 
            @Param("endDateTime") LocalDateTime endDateTime
    );

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

    // 전체 유저 평균 포인트 (User.currentPoints 사용)
    @Query(value = "SELECT COALESCE(AVG(u.current_points), 0) FROM users u", nativeQuery = true)
    Double getAveragePointsPerUser();

    /**
     * 특정 사용자의 랭킹과 앞뒤 사용자를 조회 (Window Function 사용)
     * User.currentPoints 반정규화 필드를 사용하여 JOIN 없이 조회
     * CROSS JOIN + 집계 서브쿼리로 CTE 다중 참조 문제 해결
     */
    @Query(value = """
        WITH ranked_users AS (
            SELECT
                u.id,
                u.name,
                u.avatar,
                u.department,
                COALESCE(u.current_points, 0) as total_points,
                ROW_NUMBER() OVER (ORDER BY COALESCE(u.current_points, 0) DESC, u.id ASC) as user_rank
            FROM users u
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
    List<Object[]> findUserRankingContext(@Param("userId") Long userId);
}
