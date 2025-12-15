package com.example.demo.controller;

import com.example.demo.domain.entity.Comment;
import com.example.demo.domain.entity.User;
import com.example.demo.dto.request.CommentRequest;
import com.example.demo.dto.response.CommentResponse;
import com.example.demo.security.CurrentUser;
import com.example.demo.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * 댓글 컨트롤러 - CQRS 패턴 적용
 * Write: CommentService, CommentDeleteFacade
 * Read: CommentReadService
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;
    private final CommentReadService commentReadService;
    private final CommentLikeService commentLikeService;
    private final ReportService reportService;
    private final CommentDeleteFacade commentDeleteFacade;

    @PostMapping("/feeds/{feedId}/comments")
    public ResponseEntity<CommentResponse> createComment(
            @CurrentUser User user,
            @PathVariable Long feedId,
            @Valid @RequestBody CommentRequest request
    ) {
        Comment comment = commentService.create(user, feedId, Optional.ofNullable(request.getParentId()), request.getContent());
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
        commentDeleteFacade.deleteComment(user, commentId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/feeds/{feedId}/comments")
    public ResponseEntity<List<CommentResponse>> getComments(
            @CurrentUser User currentUser,
            @PathVariable Long feedId
    ) {
        List<CommentResponse> responses = commentReadService.getCommentResponsesByFeed(feedId, currentUser);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/comments/{commentId}")
    public ResponseEntity<CommentResponse> getComment(@PathVariable Long commentId) {
        Comment comment = commentReadService.getComment(commentId);
        return ResponseEntity.ok(CommentResponse.from(comment));
    }

    @GetMapping("/comments/{commentId}/replies")
    public ResponseEntity<List<CommentResponse>> getReplies(
            @CurrentUser User currentUser,
            @PathVariable Long commentId
    ) {
        List<CommentResponse> responses = commentReadService.getReplyResponses(commentId, currentUser);
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
        reportService.toggleCommentReport(user, commentId);
        return ResponseEntity.ok().build();
    }
}
