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
import com.example.demo.domain.enums.NotificationType;
import com.example.demo.event.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentReportService {

    private final CommentReportRepository commentReportRepository;
    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final NotificationRepository notificationRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final UserPenaltyService userPenaltyService;

    @Transactional
    public void toggleReport(User reporter, Long commentId) {
        if (!commentRepository.existsById(commentId)) {
            throw new BusinessException(ErrorCode.COMMENT_NOT_FOUND);
        }

        // [수정] count > 0 체크
        if (commentReportRepository.countByReporterIdAndCommentId(reporter.getId(), commentId) > 0) {
            log.info(">>> [CommentReportService] Removing report for commentId: {} by reporterId: {}", commentId, reporter.getId());
            // [Fix] Repository method name match: deleteByReporter_IdAndComment_Id
            commentReportRepository.deleteByReporter_IdAndComment_Id(reporter.getId(), commentId);
        } else {
            log.info(">>> [CommentReportService] Creating report for commentId: {} by reporterId: {}", commentId, reporter.getId());
            Comment comment = commentRepository.getReferenceById(commentId);

            CommentReport commentReport = CommentReport.builder()
                    .reporter(reporter)
                    .comment(comment)
                    .build();
            commentReportRepository.save(commentReport);

            checkAndApplyPenalty(comment);
        }
    }

    // ... 나머지 메서드(checkAndApplyPenalty, deleteCommentByReport) 유지 ...
    private void checkAndApplyPenalty(Comment comment) {
        long likeCount = commentLikeRepository.countByComment(comment);
        long reportCount = commentReportRepository.countByComment(comment);

        if (userPenaltyService.shouldApplyPenalty(likeCount, reportCount)) {
            User writer = comment.getWriter();
            if (!userPenaltyService.hasActivePenalty(writer, PenaltyType.COMMENT)) {
                userPenaltyService.addPenalty(writer, PenaltyType.COMMENT);
                deleteCommentByReport(comment);
                eventPublisher.publishEvent(NotificationEvent.commentDeleted(writer.getId()));
            }
        }
    }

    private void deleteCommentByReport(Comment comment) {
        List<String> commentLikeIds = commentLikeRepository.findByComment(comment).stream()
                .map(cl -> String.valueOf(cl.getId()))
                .toList();
        if (!commentLikeIds.isEmpty()) {
            notificationRepository.deleteByRefIdInAndType(commentLikeIds, NotificationType.COMMENT_LIKE);
        }

        List<String> replyIds = commentRepository.findByParentOrderByIdAsc(comment).stream()
                .map(r -> String.valueOf(r.getId()))
                .toList();
        if (!replyIds.isEmpty()) {
            notificationRepository.deleteByRefIdInAndType(replyIds, NotificationType.REPLY);
        }

        commentLikeRepository.deleteByComment(comment);
        commentReportRepository.deleteByComment(comment);
        commentRepository.delete(comment);
    }
}