package com.example.demo.controller;

import com.example.demo.domain.entity.Comment;
import com.example.demo.domain.entity.User;
import com.example.demo.dto.request.CommentRequest;
import com.example.demo.dto.response.CommentResponse;
import com.example.demo.service.CommentLikeService;
import com.example.demo.service.CommentReportService;
import com.example.demo.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CommentController extends BaseController {

    private final CommentService commentService;
    private final CommentLikeService commentLikeService;
    private final CommentReportService commentReportService;

    @PostMapping("/feeds/{feedId}/comments")
    public ResponseEntity<CommentResponse> createComment(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long feedId,
            @Valid @RequestBody CommentRequest request
    ) {
        User user = getCurrentUser(userDetails);
        Comment comment = commentService.createComment(user, feedId, request.getContent());
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
    public ResponseEntity<List<CommentResponse>> getComments(@PathVariable Long feedId) {
        List<Comment> comments = commentService.getCommentsByFeed(feedId);
        List<CommentResponse> responses = comments.stream()
                .map(CommentResponse::from)
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
    public ResponseEntity<List<CommentResponse>> getReplies(@PathVariable Long commentId) {
        List<Comment> replies = commentService.getReplies(commentId);
        List<CommentResponse> responses = replies.stream()
                .map(CommentResponse::from)
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

}
