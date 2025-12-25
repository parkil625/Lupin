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

    // [추가] ID 기반 존재 확인
    @Query("SELECT COUNT(cr) > 0 FROM CommentReport cr WHERE cr.reporter.id = :reporterId AND cr.comment.id = :commentId")
    boolean existsByReporterIdAndCommentId(@Param("reporterId") Long reporterId, @Param("commentId") Long commentId);

    // [추가] ID 기반 삭제
    @Modifying
    @Query("DELETE FROM CommentReport cr WHERE cr.reporter.id = :reporterId AND cr.comment.id = :commentId")
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

    @Query("SELECT cr.comment.id FROM CommentReport cr WHERE cr.reporter.id = :reporterId AND cr.comment.id IN :commentIds")
    java.util.List<Long> findReportedCommentIdsByReporterId(@Param("reporterId") Long reporterId, @Param("commentIds") java.util.List<Long> commentIds);
}
