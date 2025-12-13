package com.example.demo.repository;

import com.example.demo.domain.entity.Feed;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Feed 관리/배치용 Repository
 * - 비즈니스 로직과 분리된 관리자용 쿼리
 * - 카운트 동기화 등 배치 작업에 사용
 */
@Repository
public interface FeedAdminRepository extends JpaRepository<Feed, Long> {

    /**
     * 좋아요 카운트 동기화
     * 실제 좋아요 수와 반정규화 필드가 다른 피드를 업데이트
     */
    @Modifying(clearAutomatically = true)
    @Query(value = """
        UPDATE feeds f
        SET like_count = (
            SELECT COUNT(*) FROM feed_likes fl WHERE fl.feed_id = f.id
        )
        WHERE f.like_count <> (
            SELECT COUNT(*) FROM feed_likes fl WHERE fl.feed_id = f.id
        )
        """, nativeQuery = true)
    int syncLikeCounts();

    /**
     * 댓글 카운트 동기화
     * 실제 댓글 수와 반정규화 필드가 다른 피드를 업데이트
     */
    @Modifying(clearAutomatically = true)
    @Query(value = """
        UPDATE feeds f
        SET comment_count = (
            SELECT COUNT(*) FROM comments c WHERE c.feed_id = f.id
        )
        WHERE f.comment_count <> (
            SELECT COUNT(*) FROM comments c WHERE c.feed_id = f.id
        )
        """, nativeQuery = true)
    int syncCommentCounts();
}
