package com.example.demo.repository;

import com.example.demo.domain.entity.Comment;
import com.example.demo.domain.entity.Feed;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Query("SELECT c FROM Comment c LEFT JOIN FETCH c.writer LEFT JOIN FETCH c.feed LEFT JOIN FETCH c.parent WHERE c.id = :id")
    Optional<Comment> findByIdWithDetails(@Param("id") Long id);

    @EntityGraph(attributePaths = {"writer", "feed"})
    List<Comment> findByFeedAndParentIsNullOrderByIdDesc(Feed feed);

    List<Comment> findByFeed(Feed feed);

    @Modifying
    @Query("DELETE FROM Comment c WHERE c.feed = :feed AND c.parent IS NOT NULL")
    void deleteRepliesByFeed(@Param("feed") Feed feed);

    @Modifying
    @Query("DELETE FROM Comment c WHERE c.feed = :feed AND c.parent IS NULL")
    void deleteParentCommentsByFeed(@Param("feed") Feed feed);

    // [이벤트 기반 삭제] feedId로 삭제 (Soft Delete 후에도 사용 가능)
    @Modifying
    @Query("DELETE FROM Comment c WHERE c.feed.id = :feedId AND c.parent IS NOT NULL")
    void deleteRepliesByFeedId(@Param("feedId") Long feedId);

    @Modifying
    @Query("DELETE FROM Comment c WHERE c.feed.id = :feedId AND c.parent IS NULL")
    void deleteParentCommentsByFeedId(@Param("feedId") Long feedId);

    @EntityGraph(attributePaths = {"writer", "feed"})
    List<Comment> findByParentOrderByIdAsc(Comment parent);

    long countByParent(Comment parent);

    long countByFeed(Feed feed);

    // [피드 삭제] 부모 댓글 ID만 조회 (엔티티 로드 없이)
    @Query("SELECT c.id FROM Comment c WHERE c.feed = :feed AND c.parent IS NULL")
    List<Long> findParentCommentIdsByFeed(@Param("feed") Feed feed);

    // [피드 삭제] 모든 댓글 ID만 조회 (엔티티 로드 없이)
    @Query("SELECT c.id FROM Comment c WHERE c.feed = :feed")
    List<Long> findCommentIdsByFeed(@Param("feed") Feed feed);

    @Query("SELECT c FROM Comment c LEFT JOIN FETCH c.writer LEFT JOIN CommentLike cl ON cl.comment = c " +
           "WHERE c.feed = :feed AND c.parent IS NULL " +
           "GROUP BY c ORDER BY COUNT(cl) DESC")
    List<Comment> findByFeedOrderByLikeCountDesc(@Param("feed") Feed feed);

    // 사용자별 댓글 수 조회
    long countByWriterId(Long writerId);
}
