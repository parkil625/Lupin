package com.example.demo.repository;

import com.example.demo.domain.entity.Draw;
import com.example.demo.domain.enums.DrawResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DrawRepository extends JpaRepository<Draw, Long> {

    List<Draw> findByUserId(Long userId);

    List<Draw> findByChallengeId(Long challengeId);

    List<Draw> findByUserIdAndResult(Long userId, DrawResult result);

    @Query("SELECT d FROM Draw d WHERE d.userId = :userId AND d.result = 'UNUSED'")
    List<Draw> findUnusedByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(d) FROM Draw d WHERE d.userId = :userId AND d.result = 'UNUSED'")
    Long countUnusedByUserId(@Param("userId") Long userId);

    Optional<Draw> findByUserIdAndChallengeId(Long userId, Long challengeId);
}
