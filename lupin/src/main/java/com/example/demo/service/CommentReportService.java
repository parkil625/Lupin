package com.example.demo.service;

import com.example.demo.domain.entity.Comment;
import com.example.demo.domain.entity.CommentReport;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.PenaltyType;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.CommentLikeRepository;
import com.example.demo.repository.CommentReportRepository;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentReportService {

    private final CommentReportRepository commentReportRepository;
    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;
    private final UserPenaltyService userPenaltyService;

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

            checkAndApplyPenalty(comment);
        }
    }

    private void checkAndApplyPenalty(Comment comment) {
        long likeCount = commentLikeRepository.countByComment(comment);
        long reportCount = commentReportRepository.countByComment(comment);

        if (userPenaltyService.shouldApplyPenalty(likeCount, reportCount)) {
            User writer = comment.getWriter();
            if (!userPenaltyService.hasActivePenalty(writer, PenaltyType.COMMENT)) {
                userPenaltyService.addPenalty(writer, PenaltyType.COMMENT);
                deleteCommentByReport(comment);
                notificationService.createCommentDeletedByReportNotification(writer);
            }
        }
    }

    private void deleteCommentByReport(Comment comment) {
        // COMMENT_LIKE 알림 삭제 (refId = CommentLike ID)
        List<String> commentLikeIds = commentLikeRepository.findByComment(comment).stream()
                .map(cl -> String.valueOf(cl.getId()))
                .toList();
        if (!commentLikeIds.isEmpty()) {
            notificationRepository.deleteByRefIdInAndType(commentLikeIds, "COMMENT_LIKE");
        }

        // REPLY 알림 삭제 (refId = Reply Comment ID)
        List<String> replyIds = commentRepository.findByParentOrderByIdAsc(comment).stream()
                .map(r -> String.valueOf(r.getId()))
                .toList();
        if (!replyIds.isEmpty()) {
            notificationRepository.deleteByRefIdInAndType(replyIds, "REPLY");
        }

        commentLikeRepository.deleteByComment(comment);
        commentReportRepository.deleteByComment(comment);
        commentRepository.delete(comment);
    }
}
