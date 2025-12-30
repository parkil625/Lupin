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

    // [벤치마킹] JPQL로 변경하여 Type Safe하게 조회
    @Query("SELECT COUNT(cr) FROM CommentReport cr WHERE cr.reporter.id = :reporterId AND cr.comment.id = :commentId")
    long countByReporterIdAndCommentId(@Param("reporterId") Long reporterId, @Param("commentId") Long commentId);

    // [벤치마킹] 좋아요 기능 벤치마킹: ID로 존재 여부 확인 (Derived Query)
    boolean existsByReporter_IdAndComment_Id(Long reporterId, Long commentId);

    // [벤치마킹] 삭제도 JPA 방식으로 안전하게 (Derived Query)
    void deleteByReporter_IdAndComment_Id(Long reporterId, Long commentId);

    // [벤치마킹] N+1 방지용 Bulk 조회: 내가 신고한 댓글 ID 목록 조회 (JPQL)
    // Feed: findFeedIdsByReporterIdAndFeedIdIn -> Comment: findCommentIdsByReporterIdAndCommentIdIn
    @Query("SELECT cr.comment.id FROM CommentReport cr WHERE cr.reporter.id = :reporterId AND cr.comment.id IN :commentIds")
    java.util.List<Long> findCommentIdsByReporterIdAndCommentIdIn(@Param("reporterId") Long reporterId, @Param("commentIds") java.util.List<Long> commentIds);

    void deleteByComment(Comment comment);

    @Modifying
    @Query("DELETE FROM CommentReport cr WHERE cr.comment.feed = :feed")
    void deleteByFeed(@Param("feed") Feed feed);

    @Modifying
    @Query("DELETE FROM CommentReport cr WHERE cr.comment.feed.id = :feedId")
    void deleteByFeedId(@Param("feedId") Long feedId);
}