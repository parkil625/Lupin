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
}
