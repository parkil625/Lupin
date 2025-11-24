package com.example.demo.repository;

import com.example.demo.domain.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    // 좋아요 수 업데이트 (Redis 동기화용)
    @Modifying
    @Query("UPDATE Comment c SET c.likesCount = :likes WHERE c.id = :id")
    void updateLikesCount(@Param("id") Long id, @Param("likes") int likes);

    /**
     * 특정 피드의 최상위 댓글 조회 (페이징)
     */
    @Query("SELECT c FROM Comment c WHERE c.feedId = :feedId AND c.parentId IS NULL ORDER BY c.createdAt DESC")
    Page<Comment> findTopLevelCommentsByFeedId(@Param("feedId") Long feedId, Pageable pageable);

    /**
     * 특정 피드의 최상위 댓글 조회 (전체)
     */
    @Query("SELECT c FROM Comment c WHERE c.feedId = :feedId AND c.parentId IS NULL ORDER BY c.createdAt DESC")
    List<Comment> findTopLevelCommentsByFeedId(@Param("feedId") Long feedId);

    /**
     * 특정 댓글의 답글 조회
     */
    @Query("SELECT c FROM Comment c WHERE c.parentId = :parentId ORDER BY c.createdAt ASC")
    List<Comment> findRepliesByParentId(@Param("parentId") Long parentId);

    /**
     * 특정 피드의 댓글 수 조회
     */
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.feedId = :feedId")
    Long countByFeedId(@Param("feedId") Long feedId);

    /**
     * 특정 사용자의 댓글 조회
     */
    @Query("SELECT c FROM Comment c WHERE c.writerId = :userId ORDER BY c.createdAt DESC")
    Page<Comment> findByWriterId(@Param("userId") Long userId, Pageable pageable);

    List<Comment> findByFeedId(Long feedId);
}
