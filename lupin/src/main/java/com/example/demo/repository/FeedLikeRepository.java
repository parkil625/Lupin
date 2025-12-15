package com.example.demo.repository;

import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.FeedLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeedLikeRepository extends JpaRepository<FeedLike, Long> {

    // ID로 존재 확인 (userId만 사용하여 detached entity 문제 방지)
    @Query("SELECT COUNT(fl) > 0 FROM FeedLike fl WHERE fl.user.id = :userId AND fl.feed.id = :feedId")
    boolean existsByUserIdAndFeedId(@Param("userId") Long userId, @Param("feedId") Long feedId);

    // ID로 조회 (userId만 사용하여 detached entity 문제 방지)
    @Query("SELECT fl FROM FeedLike fl WHERE fl.user.id = :userId AND fl.feed.id = :feedId")
    Optional<FeedLike> findByUserIdAndFeedId(@Param("userId") Long userId, @Param("feedId") Long feedId);

    // [최적화] 피드 삭제 시 벌크 삭제
    @Modifying
    @Query("DELETE FROM FeedLike fl WHERE fl.feed = :feed")
    void deleteByFeed(@Param("feed") Feed feed);

    // [이벤트 기반 삭제] feedId로 삭제 (Soft Delete 후에도 사용 가능)
    @Modifying
    @Query("DELETE FROM FeedLike fl WHERE fl.feed.id = :feedId")
    void deleteByFeedId(@Param("feedId") Long feedId);

    long countByFeed(Feed feed);
}
