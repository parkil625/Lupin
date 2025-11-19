package com.example.demo.repository;

import com.example.demo.domain.entity.ChallengeEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChallengeEntryRepository extends JpaRepository<ChallengeEntry, Long> {

    // 사용자의 챌린지 참가 여부 확인
    boolean existsByChallengeIdAndUserId(Long challengeId, Long userId);

    // 사용자의 챌린지 참가 조회
    Optional<ChallengeEntry> findByChallengeIdAndUserId(Long challengeId, Long userId);

    // 챌린지의 참가자 목록
    List<ChallengeEntry> findByChallengeIdOrderByJoinedAtAsc(Long challengeId);

    // 챌린지 참가자 수
    Long countByChallengeId(Long challengeId);
}
