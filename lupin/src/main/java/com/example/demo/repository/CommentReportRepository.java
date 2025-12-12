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

    void deleteByComment(Comment comment);

    // [피드 삭제] 피드의 모든 댓글 신고 일괄 삭제
    @Modifying
    @Query("DELETE FROM CommentReport cr WHERE cr.comment.feed = :feed")
    void deleteByFeed(@Param("feed") Feed feed);
}
