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
}
