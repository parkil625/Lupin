package com.example.demo.repository;

import com.example.demo.domain.entity.Comment;
import com.example.demo.domain.entity.CommentReport;
import com.example.demo.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommentReportRepository extends JpaRepository<CommentReport, Long> {

    Optional<CommentReport> findByReporterAndComment(User reporter, Comment comment);

    long countByComment(Comment comment);

    void deleteByComment(Comment comment);

    @Modifying
    @Query("DELETE FROM CommentReport cr WHERE cr.comment.id = :commentId")
    void deleteByCommentId(@Param("commentId") Long commentId);

    @Modifying
    @Query("DELETE FROM CommentReport cr WHERE cr.comment.id IN :commentIds")
    void deleteByCommentIds(@Param("commentIds") List<Long> commentIds);

    @Modifying
    @Query("DELETE FROM CommentReport cr WHERE cr.comment.feed.id = :feedId")
    void deleteByFeedId(@Param("feedId") Long feedId);
}
