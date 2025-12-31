package com.example.demo.service;

import com.example.demo.domain.entity.Comment;
import com.example.demo.domain.entity.CommentLike;
import com.example.demo.domain.entity.User;
import com.example.demo.event.NotificationEvent;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.CommentLikeRepository;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.NotificationRepository;
import com.example.demo.domain.enums.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentLikeService {

    private final CommentLikeRepository commentLikeRepository;
    private final CommentRepository commentRepository;
    private final NotificationRepository notificationRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final NotificationSseService notificationSseService;

    @Transactional
    public CommentLike likeComment(User user, Long commentId) {
        // [최적화] ID 기반 존재 확인
        if (commentLikeRepository.existsByUserIdAndCommentId(user.getId(), commentId)) {
            throw new BusinessException(ErrorCode.ALREADY_LIKED);
        }

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));

        CommentLike commentLike = CommentLike.builder()
                .user(user)
                .comment(comment)
                .build();

        CommentLike savedCommentLike = commentLikeRepository.save(commentLike);

        // [최적화] 이벤트 발행 - 트랜잭션 커밋 후 비동기 알림 처리
        eventPublisher.publishEvent(NotificationEvent.commentLike(
                comment.getWriter().getId(),
                user.getId(),
                user.getName(),
                user.getAvatar(),
                commentId,
                comment.getContent()
        ));

        return savedCommentLike;
    }

    @Transactional
    public void unlikeComment(User user, Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));

        CommentLike commentLike = commentLikeRepository.findByUserIdAndCommentId(user.getId(), commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LIKE_NOT_FOUND));

        // 1. 좋아요 데이터 삭제
        commentLikeRepository.delete(commentLike);
        log.info(">>> [CommentLike Service] Deleted like for commentId: {}, userId: {}", commentId, user.getId());

        // 2. [핵심] 알림 뭉치기 대응 로직
        // 삭제 후 남은 좋아요 개수 확인
        long remainingLikes = commentLikeRepository.countByCommentId(commentId);

        if (remainingLikes == 0) {
            // 남은 좋아요가 없으면 알림 조회 -> SSE 전송 -> 삭제
            String refId = String.valueOf(commentId);

            // 1. 삭제할 알림 조회
            java.util.List<com.example.demo.domain.entity.Notification> targets = notificationRepository.findByRefIdAndType(refId, NotificationType.COMMENT_LIKE);

            if (!targets.isEmpty()) {
                // 2. 알림 수신자에게 삭제 이벤트 전송
                Long receiverId = targets.get(0).getUser().getId();
                java.util.List<Long> ids = targets.stream().map(com.example.demo.domain.entity.Notification::getId).toList();

                // SSE로 "이 알림 지워라" 명령 전송
                notificationSseService.sendNotificationDelete(receiverId, ids);

                // 3. DB 삭제
                notificationRepository.deleteAll(targets);
            }
            log.info(">>> [CommentLike Service] No likes remaining. Deleted notification for commentId: {}", commentId);
        } else {
            // 남은 좋아요가 있다면, 가장 최근에 좋아요 누른 사람을 찾아 알림 갱신 이벤트 발행
            // -> Listener가 이 이벤트를 받아서 기존 알림을 찾아 "A님 외 N명이..." 형태로 내용을 업데이트함
            log.info(">>> [CommentLike Service] Likes remaining: {}. Updating notification...", remainingLikes);
            commentLikeRepository.findTopByCommentIdOrderByCreatedAtDesc(commentId)
                    .ifPresent(latestLike -> {
                        User latestLiker = latestLike.getUser();

                        eventPublisher.publishEvent(NotificationEvent.commentLike(
                                comment.getWriter().getId(),
                                latestLiker.getId(), // 이제 이 사람이 대표 알림 유발자
                                latestLiker.getName(),
                                latestLiker.getAvatar(),
                                commentId,
                                comment.getContent()
                        ));
                    });
        }
    }

    /**
     * 댓글 좋아요 ID로 댓글 ID 조회
     */
    public Optional<Long> getCommentIdByLikeId(Long commentLikeId) {
        return commentLikeRepository.findByIdWithComment(commentLikeId)
                .map(commentLike -> commentLike.getComment().getId());
    }
}
