package com.example.demo.repository;

import com.example.demo.domain.entity.Challenge;
import com.example.demo.domain.enums.ChallengeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChallengeRepository extends JpaRepository<Challenge, Long> {

    // 활성화된 챌린지 목록
    List<Challenge> findByStatusOrderByOpensAtDesc(ChallengeStatus status);

    // 현재 진행 중인 챌린지 조회
    @Query("SELECT c FROM Challenge c " +
           "WHERE c.status = 'ACTIVE' " +
           "AND c.opensAt <= :now " +
           "AND c.closesAt > :now " +
           "ORDER BY c.closesAt ASC")
    List<Challenge> findActiveChallenges(LocalDateTime now);

    // 특정 기간에 시작하는 챌린지
    List<Challenge> findByOpensAtBetweenOrderByOpensAtAsc(LocalDateTime start, LocalDateTime end);

    // 활성화 시간이 된 이벤트 조회 메소드
    @Query("SELECT c FROM Challenge c " +
           "WHERE c.status = 'SCHEDULED' " +
           "AND c.opensAt <= :now "
    )
    List<Challenge> findScheduledChallengesToOpen(LocalDateTime now);

    @Query("SELECT c FROM Challenge c " +
           "WHERE c.status = 'ACTIVE' "+
           "AND c.closesAt <= :now")

    List<Challenge> findActiveChallengesToClose(LocalDateTime now);





}
