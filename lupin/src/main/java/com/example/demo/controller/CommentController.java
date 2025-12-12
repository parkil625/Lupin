package com.example.demo.controller;

import com.example.demo.domain.entity.Comment;
import com.example.demo.domain.entity.User;
import com.example.demo.dto.request.CommentRequest;
import com.example.demo.dto.response.CommentResponse;
import com.example.demo.repository.CommentLikeRepository;
import com.example.demo.repository.FeedRepository;
import com.example.demo.service.CommentLikeService;
import com.example.demo.service.CommentReportService;
import com.example.demo.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CommentController extends BaseController {

    private final CommentService commentService;
    private final CommentLikeService commentLikeService;
    private final CommentReportService commentReportService;
    private final CommentLikeRepository commentLikeRepository; // [최적화] 배치 조회용
    private final FeedRepository feedRepository; // [activeDays] 계산용

    @PostMapping("/feeds/{feedId}/comments")
    public ResponseEntity<CommentResponse> createComment(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long feedId,
            @Valid @RequestBody CommentRequest request
    ) {
        User user = getCurrentUser(userDetails);
        Comment comment;
        if (request.getParentId() != null) {
            // 답글인 경우 createReply 호출 (답글 알림만 발생)
            comment = commentService.createReply(user, feedId, request.getParentId(), request.getContent());
        } else {
            // 일반 댓글인 경우
            comment = commentService.createComment(user, feedId, request.getContent());
        }
        return ResponseEntity.ok(CommentResponse.from(comment));
    }

    @PutMapping("/comments/{commentId}")
    public ResponseEntity<CommentResponse> updateComment(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long commentId,
            @Valid @RequestBody CommentRequest request
    ) {
        User user = getCurrentUser(userDetails);
        Comment comment = commentService.updateComment(user, commentId, request.getContent());
        return ResponseEntity.ok(CommentResponse.from(comment));
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long commentId
    ) {
        User user = getCurrentUser(userDetails);
        commentService.deleteComment(user, commentId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/feeds/{feedId}/comments")
    public ResponseEntity<List<CommentResponse>> getComments(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long feedId
    ) {
        User currentUser = userDetails != null ? getCurrentUser(userDetails) : null;
        List<Comment> comments = commentService.getCommentsByFeed(feedId);

        // [최적화] 배치 조회로 N+1 문제 해결
        List<Long> commentIds = comments.stream().map(Comment::getId).toList();
        Map<Long, Long> likeCounts = getLikeCountMap(commentIds);
        Set<Long> likedIds = currentUser != null ? getLikedCommentIds(currentUser.getId(), commentIds) : Collections.emptySet();

        // [activeDays] 작성자별 활동일 배치 조회
        Map<Long, Integer> activeDaysMap = getActiveDaysMap(comments);

        List<CommentResponse> responses = comments.stream()
                .map(comment -> CommentResponse.from(
                        comment,
                        likeCounts.getOrDefault(comment.getId(), 0L),
                        likedIds.contains(comment.getId()),
                        activeDaysMap.getOrDefault(comment.getWriter().getId(), 0)
                ))
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/comments/{commentId}")
    public ResponseEntity<CommentResponse> getComment(@PathVariable Long commentId) {
        Comment comment = commentService.getComment(commentId);
        return ResponseEntity.ok(CommentResponse.from(comment));
    }

    @PostMapping("/comments/{commentId}/replies")
    public ResponseEntity<CommentResponse> createReply(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long commentId,
            @RequestParam Long feedId,
            @Valid @RequestBody CommentRequest request
    ) {
        User user = getCurrentUser(userDetails);
        Comment reply = commentService.createReply(user, feedId, commentId, request.getContent());
        return ResponseEntity.ok(CommentResponse.from(reply));
    }

    @GetMapping("/comments/{commentId}/replies")
    public ResponseEntity<List<CommentResponse>> getReplies(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long commentId
    ) {
        User currentUser = userDetails != null ? getCurrentUser(userDetails) : null;
        List<Comment> replies = commentService.getReplies(commentId);

        // [최적화] 배치 조회로 N+1 문제 해결
        List<Long> replyIds = replies.stream().map(Comment::getId).toList();
        Map<Long, Long> likeCounts = getLikeCountMap(replyIds);
        Set<Long> likedIds = currentUser != null ? getLikedCommentIds(currentUser.getId(), replyIds) : Collections.emptySet();

        // [activeDays] 작성자별 활동일 배치 조회
        Map<Long, Integer> activeDaysMap = getActiveDaysMap(replies);

        List<CommentResponse> responses = replies.stream()
                .map(reply -> CommentResponse.from(
                        reply,
                        likeCounts.getOrDefault(reply.getId(), 0L),
                        likedIds.contains(reply.getId()),
                        activeDaysMap.getOrDefault(reply.getWriter().getId(), 0)
                ))
                .toList();
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/comments/{commentId}/like")
    public ResponseEntity<Void> likeComment(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long commentId
    ) {
        User user = getCurrentUser(userDetails);
        commentLikeService.likeComment(user, commentId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/comments/{commentId}/like")
    public ResponseEntity<Void> unlikeComment(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long commentId
    ) {
        User user = getCurrentUser(userDetails);
        commentLikeService.unlikeComment(user, commentId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/comments/{commentId}/report")
    public ResponseEntity<Void> reportComment(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long commentId
    ) {
        User user = getCurrentUser(userDetails);
        commentReportService.toggleReport(user, commentId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/comment-likes/{commentLikeId}")
    public ResponseEntity<Map<String, Long>> getCommentLike(@PathVariable Long commentLikeId) {
        return commentLikeRepository.findByIdWithComment(commentLikeId)
                .map(commentLike -> ResponseEntity.ok(Map.of("commentId", commentLike.getComment().getId())))
                .orElse(ResponseEntity.notFound().build());
    }

    // [최적화] 배치 조회 헬퍼 메서드 - N개의 댓글 좋아요 수를 2개 쿼리로 조회
    private Map<Long, Long> getLikeCountMap(List<Long> commentIds) {
        if (commentIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return commentLikeRepository.countByCommentIds(commentIds).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));
    }

    private Set<Long> getLikedCommentIds(Long userId, List<Long> commentIds) {
        if (commentIds.isEmpty()) {
            return Collections.emptySet();
        }
        return Set.copyOf(commentLikeRepository.findLikedCommentIdsByUserId(userId, commentIds));
    }

    // [activeDays] 댓글 작성자별 이번 달 활동일 배치 조회
    private Map<Long, Integer> getActiveDaysMap(List<Comment> comments) {
        if (comments.isEmpty()) {
            return Map.of();
        }

        List<Long> writerIds = comments.stream()
                .map(comment -> comment.getWriter().getId())
                .distinct()
                .toList();

        YearMonth currentMonth = YearMonth.now();
        LocalDateTime startOfMonth = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = currentMonth.atEndOfMonth().atTime(23, 59, 59);

        List<Object[]> results = feedRepository.countActiveDaysByWriterIds(writerIds, startOfMonth, endOfMonth);

        Map<Long, Integer> activeDaysMap = new HashMap<>();
        for (Object[] row : results) {
            Long writerId = (Long) row[0];
            Long activeDays = (Long) row[1];
            activeDaysMap.put(writerId, activeDays.intValue());
        }
        return activeDaysMap;
    }
}
