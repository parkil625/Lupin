package com.example.demo.controller;

import com.example.demo.domain.entity.Comment;
import com.example.demo.domain.entity.User;
import com.example.demo.dto.request.CommentRequest;
import com.example.demo.dto.response.CommentResponse;
import com.example.demo.security.CurrentUser;
import com.example.demo.service.CommentLikeService;
import com.example.demo.service.CommentReportService;
import com.example.demo.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;
    private final CommentLikeService commentLikeService;
    private final CommentReportService commentReportService;

    @PostMapping("/feeds/{feedId}/comments")
    public ResponseEntity<CommentResponse> createComment(
            @CurrentUser User user,
            @PathVariable Long feedId,
            @Valid @RequestBody CommentRequest request
    ) {
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
            @CurrentUser User user,
            @PathVariable Long commentId,
            @Valid @RequestBody CommentRequest request
    ) {
        Comment comment = commentService.updateComment(user, commentId, request.getContent());
        return ResponseEntity.ok(CommentResponse.from(comment));
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @CurrentUser User user,
            @PathVariable Long commentId
    ) {
        commentService.deleteComment(user, commentId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/feeds/{feedId}/comments")
    public ResponseEntity<List<CommentResponse>> getComments(
            @CurrentUser User currentUser,
            @PathVariable Long feedId
    ) {
        List<CommentResponse> responses = commentService.getCommentResponsesByFeed(feedId, currentUser);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/comments/{commentId}")
    public ResponseEntity<CommentResponse> getComment(@PathVariable Long commentId) {
        Comment comment = commentService.getComment(commentId);
        return ResponseEntity.ok(CommentResponse.from(comment));
    }

    @PostMapping("/comments/{commentId}/replies")
    public ResponseEntity<CommentResponse> createReply(
            @CurrentUser User user,
            @PathVariable Long commentId,
            @RequestParam Long feedId,
            @Valid @RequestBody CommentRequest request
    ) {
        Comment reply = commentService.createReply(user, feedId, commentId, request.getContent());
        return ResponseEntity.ok(CommentResponse.from(reply));
    }

    @GetMapping("/comments/{commentId}/replies")
    public ResponseEntity<List<CommentResponse>> getReplies(
            @CurrentUser User currentUser,
            @PathVariable Long commentId
    ) {
        List<CommentResponse> responses = commentService.getReplyResponses(commentId, currentUser);
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/comments/{commentId}/like")
    public ResponseEntity<Void> likeComment(
            @CurrentUser User user,
            @PathVariable Long commentId
    ) {
        commentLikeService.likeComment(user, commentId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/comments/{commentId}/like")
    public ResponseEntity<Void> unlikeComment(
            @CurrentUser User user,
            @PathVariable Long commentId
    ) {
        commentLikeService.unlikeComment(user, commentId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/comments/{commentId}/report")
    public ResponseEntity<Void> reportComment(
            @CurrentUser User user,
            @PathVariable Long commentId
    ) {
        commentReportService.toggleReport(user, commentId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/comment-likes/{commentLikeId}")
    public ResponseEntity<Map<String, Long>> getCommentLike(@PathVariable Long commentLikeId) {
        return commentLikeService.getCommentIdByLikeId(commentLikeId)
                .map(commentId -> ResponseEntity.ok(Map.of("commentId", commentId)))
                .orElse(ResponseEntity.notFound().build());
    }
}
