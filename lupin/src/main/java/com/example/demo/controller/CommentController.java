package com.example.demo.controller;

import com.example.demo.domain.entity.Comment;
import com.example.demo.domain.entity.User;
import com.example.demo.dto.request.CommentRequest;
import com.example.demo.dto.response.CommentResponse;
import com.example.demo.repository.CommentLikeRepository;
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
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CommentController extends BaseController {

    private final CommentService commentService;
    private final CommentLikeService commentLikeService;
    private final CommentReportService commentReportService;
    private final CommentLikeRepository commentLikeRepository;

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
        List<CommentResponse> responses = comments.stream()
                .map(comment -> {
                    long likeCount = commentLikeRepository.countByComment(comment);
                    boolean isLiked = currentUser != null && commentLikeRepository.existsByUserAndComment(currentUser, comment);
                    return CommentResponse.from(comment, likeCount, isLiked);
                })
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
        List<CommentResponse> responses = replies.stream()
                .map(reply -> {
                    long likeCount = commentLikeRepository.countByComment(reply);
                    boolean isLiked = currentUser != null && commentLikeRepository.existsByUserAndComment(currentUser, reply);
                    return CommentResponse.from(reply, likeCount, isLiked);
                })
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
        return commentLikeRepository.findById(commentLikeId)
                .map(commentLike -> ResponseEntity.ok(Map.of("commentId", commentLike.getComment().getId())))
                .orElse(ResponseEntity.notFound().build());
    }
}
