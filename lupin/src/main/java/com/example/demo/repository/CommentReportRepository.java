package com.example.demo.repository;

import com.example.demo.domain.entity.Comment;
import com.example.demo.domain.entity.CommentReport;
import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentReportRepository extends JpaRepository<CommentReport, Long> {

    long countByComment(Comment comment);

    boolean existsByReporterAndComment(User reporter, Comment comment);

    void deleteByReporterAndComment(User reporter, Comment comment);

    @Query(value = "SELECT COUNT(*) > 0 FROM comment_reports WHERE reporter_id = :reporterId AND comment_id = :commentId", nativeQuery = true)
    boolean existsByReporterIdAndCommentId(@Param("reporterId") Long reporterId, @Param("commentId") Long commentId);

    @Modifying
    @Query(value = "DELETE FROM comment_reports WHERE reporter_id = :reporterId AND comment_id = :commentId", nativeQuery = true)
    void deleteByReporterIdAndCommentId(@Param("reporterId") Long reporterId, @Param("commentId") Long commentId);

    void deleteByComment(Comment comment);

    // [피드 삭제] 피드의 모든 댓글 신고 일괄 삭제
    @Modifying
    @Query("DELETE FROM CommentReport cr WHERE cr.comment.feed = :feed")
    void deleteByFeed(@Param("feed") Feed feed);

    // [이벤트 기반 삭제] feedId로 삭제 (Soft Delete 후에도 사용 가능)
    @Modifying
    @Query("DELETE FROM CommentReport cr WHERE cr.comment.feed.id = :feedId")
    void deleteByFeedId(@Param("feedId") Long feedId);

    @Query(value = "SELECT comment_id FROM comment_reports WHERE reporter_id = :reporterId AND comment_id IN :commentIds", nativeQuery = true)
    java.util.List<Long> findReportedCommentIdsByReporterId(@Param("reporterId") Long reporterId, @Param("commentIds") java.util.List<Long> commentIds);
}
