package com.example.demo.service;

import com.example.demo.domain.entity.Comment;
import com.example.demo.domain.entity.CommentReport;
import com.example.demo.domain.entity.User;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.CommentReportRepository;
import com.example.demo.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentReportService {

    private final CommentReportRepository commentReportRepository;
    private final CommentRepository commentRepository;

    @Transactional
    public void toggleReport(User reporter, Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));

        if (commentReportRepository.existsByReporterAndComment(reporter, comment)) {
            commentReportRepository.deleteByReporterAndComment(reporter, comment);
        } else {
            CommentReport commentReport = CommentReport.builder()
                    .reporter(reporter)
                    .comment(comment)
                    .build();
            commentReportRepository.save(commentReport);
        }
    }
}
