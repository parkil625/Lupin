package com.example.demo.event;

import com.example.demo.domain.enums.NotificationType;
import com.example.demo.repository.CommentLikeRepository;
import com.example.demo.repository.CommentReportRepository;
import com.example.demo.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CommentEventListener {

    private final NotificationRepository notificationRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final CommentReportRepository commentReportRepository;

    @Async
    @EventListener
    public void handleCommentDeletedEvent(CommentDeletedEvent event) {
        try {
            // 1. 관련 알림 삭제
            // - 본인 댓글에 대한 좋아요 알림
            notificationRepository.deleteByRefIdAndType(String.valueOf(event.commentId()), NotificationType.COMMENT_LIKE);
            // - 본인 댓글이 부모일 경우, 달린 답글 알림
            if (event.isParent()) {
                notificationRepository.deleteByRefIdAndType(String.valueOf(event.commentId()), NotificationType.REPLY);
            }

            // 2. 관련 좋아요 삭제
            commentLikeRepository.deleteByCommentId(event.commentId());

            // 3. 관련 신고 삭제
            commentReportRepository.deleteByCommentId(event.commentId());

            // 4. 대댓글이 있다면 대댓글의 좋아요, 신고도 삭제
            if (!event.replyIds().isEmpty()) {
                commentLikeRepository.deleteByCommentIds(event.replyIds());
                commentReportRepository.deleteByCommentIds(event.replyIds());
            }

            log.info("Related data cleaned up for deleted comment: {}", event.commentId());
        } catch (Exception e) {
            log.error("Failed to clean up data for deleted comment: {}", event, e);
        }
    }
}
