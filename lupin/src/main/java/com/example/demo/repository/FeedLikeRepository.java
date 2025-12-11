package com.example.demo.repository;

import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.FeedLike;
import com.example.demo.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeedLikeRepository extends JpaRepository<FeedLike, Long> {

    // [최적화] 존재 확인 - LIMIT 1로 바로 종료
    boolean existsByUserAndFeed(User user, Feed feed);

    // ID로 존재 확인 (더 빠름)
    @Query("SELECT COUNT(fl) > 0 FROM FeedLike fl WHERE fl.user.id = :userId AND fl.feed.id = :feedId")
    boolean existsByUserIdAndFeedId(@Param("userId") Long userId, @Param("feedId") Long feedId);

    Optional<FeedLike> findByUserAndFeed(User user, Feed feed);

    // [최적화] 벌크 삭제 - 조회 없이 바로 삭제
    @Modifying
    @Query("DELETE FROM FeedLike fl WHERE fl.user = :user AND fl.feed = :feed")
    void deleteByUserAndFeed(@Param("user") User user, @Param("feed") Feed feed);

    // [최적화] 피드 삭제 시 벌크 삭제
    @Modifying
    @Query("DELETE FROM FeedLike fl WHERE fl.feed = :feed")
    void deleteByFeed(@Param("feed") Feed feed);

    long countByFeed(Feed feed);

    List<FeedLike> findByFeed(Feed feed);
}
