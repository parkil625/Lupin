package com.example.demo.repository;

import com.example.demo.domain.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    /**
     * 특정 피드의 최상위 댓글 조회 (페이징)
     */
    @Query("SELECT c FROM Comment c WHERE c.feed.id = :feedId AND c.parent IS NULL ORDER BY c.createdAt DESC")
    Page<Comment> findTopLevelCommentsByFeedId(@Param("feedId") Long feedId, Pageable pageable);

    /**
     * 특정 피드의 최상위 댓글 조회 (전체)
     */
    @Query("SELECT c FROM Comment c WHERE c.feed.id = :feedId AND c.parent IS NULL ORDER BY c.createdAt DESC")
    List<Comment> findTopLevelCommentsByFeedId(@Param("feedId") Long feedId);

    /**
     * 특정 댓글의 답글 조회
     */
    @Query("SELECT c FROM Comment c WHERE c.parent.id = :parentId ORDER BY c.createdAt ASC")
    List<Comment> findRepliesByParentId(@Param("parentId") Long parentId);

    /**
     * 특정 피드의 댓글 수 조회
     */
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.feed.id = :feedId")
    Long countByFeedId(@Param("feedId") Long feedId);

    /**
     * 특정 사용자의 댓글 조회
     */
    @Query("SELECT c FROM Comment c WHERE c.writer.id = :userId ORDER BY c.createdAt DESC")
    Page<Comment> findByWriterId(@Param("userId") Long userId, Pageable pageable);
}
