package com.example.demo.repository;

import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.FeedLike;
import com.example.demo.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeedLikeRepository extends JpaRepository<FeedLike, Long> {

    // 특정 사용자가 특정 피드를 좋아요 했는지 확인
    boolean existsByUserIdAndFeedId(Long userId, Long feedId);

    // Feed와 User 객체로 존재 여부 확인
    boolean existsByFeedAndUser(Feed feed, User user);

    // 좋아요 조회
    Optional<FeedLike> findByUserIdAndFeedId(Long userId, Long feedId);

    // 피드의 좋아요 개수
    Long countByFeedId(Long feedId);

    // 특정 피드의 모든 좋아요 조회
    List<FeedLike> findAllByFeed(Feed feed);
}
