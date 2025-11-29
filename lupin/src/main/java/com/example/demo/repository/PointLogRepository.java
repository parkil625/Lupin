package com.example.demo.repository;

import com.example.demo.domain.entity.PointLog;
import com.example.demo.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PointLogRepository extends JpaRepository<PointLog, Long> {

    @Query("SELECT COALESCE(SUM(p.points), 0) FROM PointLog p WHERE p.user = :user")
    Long sumPointsByUser(@Param("user") User user);
}
