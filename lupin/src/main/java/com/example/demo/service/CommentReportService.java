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
import org.springframework.context.ApplicationEventPublisher;
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
    private final ApplicationEventPublisher eventPublisher;
    private final UserPenaltyService userPenaltyService;

    @Transactional
    public void toggleReport(User reporter, Long commentId) {
        // 엔티티 조회 전 ID로 존재 여부 확인 (성능 최적화 및 안전성)
        if (!commentRepository.existsById(commentId)) {
            throw new BusinessException(ErrorCode.COMMENT_NOT_FOUND);
        }

        // [수정] ID 기반 메서드로 토글 로직 수행
        if (commentReportRepository.existsByReporterIdAndCommentId(reporter.getId(), commentId)) {
            commentReportRepository.deleteByReporterIdAndCommentId(reporter.getId(), commentId);
        } else {
            // 생성 시에만 엔티티 조회
            Comment comment = commentRepository.getReferenceById(commentId);

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
                eventPublisher.publishEvent(NotificationEvent.commentDeleted(writer.getId()));
            }
        }
    }

    private void deleteCommentByReport(Comment comment) {
        // COMMENT_LIKE 알림 삭제 (refId = CommentLike ID)
        List<String> commentLikeIds = commentLikeRepository.findByComment(comment).stream()
                .map(cl -> String.valueOf(cl.getId()))
                .toList();
        if (!commentLikeIds.isEmpty()) {
            notificationRepository.deleteByRefIdInAndType(commentLikeIds, NotificationType.COMMENT_LIKE);
        }

        // REPLY 알림 삭제 (refId = Reply Comment ID)
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
