package com.example.demo.repository;

import com.example.demo.domain.entity.FeedLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FeedLikeRepository extends JpaRepository<FeedLike, Long> {

    // 특정 사용자가 특정 피드를 좋아요 했는지 확인
    boolean existsByUserIdAndFeedId(Long userId, Long feedId);

    // 좋아요 조회
    Optional<FeedLike> findByUserIdAndFeedId(Long userId, Long feedId);

    // 피드의 좋아요 개수
    Long countByFeedId(Long feedId);
}
