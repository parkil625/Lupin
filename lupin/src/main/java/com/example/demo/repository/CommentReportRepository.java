package com.example.demo.repository;

import com.example.demo.domain.entity.Comment;
import com.example.demo.domain.entity.CommentReport;
import com.example.demo.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentReportRepository extends JpaRepository<CommentReport, Long> {

    long countByComment(Comment comment);

    boolean existsByReporterAndComment(User reporter, Comment comment);

    void deleteByReporterAndComment(User reporter, Comment comment);

    void deleteByComment(Comment comment);
}
