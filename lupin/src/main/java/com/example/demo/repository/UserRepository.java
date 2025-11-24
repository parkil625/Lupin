package com.example.demo.repository;

import com.example.demo.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUserId(String userId);

    boolean existsByEmail(String email);

    boolean existsByUserId(String userId);

    // 포인트 업데이트 (Redis 동기화용)
    @Modifying
    @Query("UPDATE User u SET u.currentPoints = :current, u.monthlyPoints = :monthly WHERE u.id = :id")
    void updatePoints(@Param("id") Long id, @Param("current") Long current, @Param("monthly") Long monthly);

    // 랭킹 조회
    @Query("SELECT u FROM User u ORDER BY u.monthlyPoints DESC")
    List<User> findTopByMonthlyPoints();

    @Query("SELECT u FROM User u ORDER BY u.monthlyLikes DESC")
    List<User> findTopByMonthlyLikes();
}
