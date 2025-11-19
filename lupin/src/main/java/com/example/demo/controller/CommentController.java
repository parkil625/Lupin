package com.example.demo.controller;

import com.example.demo.dto.request.CommentCreateRequest;
import com.example.demo.dto.response.CommentResponse;
import com.example.demo.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 댓글 관련 API
 */
@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    /**
     * 댓글 생성
     */
    @PostMapping("/feeds/{feedId}")
    public ResponseEntity<CommentResponse> createComment(
            @PathVariable Long feedId,
            @RequestParam Long userId,
            @Valid @RequestBody CommentCreateRequest request) {
        CommentResponse response = commentService.createComment(feedId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 특정 피드의 댓글 목록 조회 (페이징)
     */
    @GetMapping("/feeds/{feedId}")
    public ResponseEntity<Page<CommentResponse>> getCommentsByFeedId(
            @PathVariable Long feedId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<CommentResponse> comments = commentService.getCommentsByFeedId(feedId, pageable);
        return ResponseEntity.ok(comments);
    }

    /**
     * 특정 피드의 댓글 목록 조회 (전체)
     */
    @GetMapping("/feeds/{feedId}/all")
    public ResponseEntity<List<CommentResponse>> getAllCommentsByFeedId(@PathVariable Long feedId) {
        List<CommentResponse> comments = commentService.getAllCommentsByFeedId(feedId);
        return ResponseEntity.ok(comments);
    }

    /**
     * 특정 댓글의 답글 조회
     */
    @GetMapping("/{commentId}/replies")
    public ResponseEntity<List<CommentResponse>> getRepliesByCommentId(@PathVariable Long commentId) {
        List<CommentResponse> replies = commentService.getRepliesByCommentId(commentId);
        return ResponseEntity.ok(replies);
    }

    /**
     * 댓글 상세 조회
     */
    @GetMapping("/{commentId}")
    public ResponseEntity<CommentResponse> getCommentDetail(@PathVariable Long commentId) {
        CommentResponse comment = commentService.getCommentDetail(commentId);
        return ResponseEntity.ok(comment);
    }

    /**
     * 댓글 수정
     */
    @PutMapping("/{commentId}")
    public ResponseEntity<CommentResponse> updateComment(
            @PathVariable Long commentId,
            @RequestParam Long userId,
            @RequestParam String content) {
        CommentResponse response = commentService.updateComment(commentId, userId, content);
        return ResponseEntity.ok(response);
    }

    /**
     * 댓글 삭제
     */
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long commentId,
            @RequestParam Long userId) {
        commentService.deleteComment(commentId, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 특정 피드의 댓글 수 조회
     */
    @GetMapping("/feeds/{feedId}/count")
    public ResponseEntity<Long> getCommentCountByFeedId(@PathVariable Long feedId) {
        Long count = commentService.getCommentCountByFeedId(feedId);
        return ResponseEntity.ok(count);
    }

    /**
     * 특정 사용자의 댓글 조회
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<Page<CommentResponse>> getCommentsByUserId(
            @PathVariable Long userId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<CommentResponse> comments = commentService.getCommentsByUserId(userId, pageable);
        return ResponseEntity.ok(comments);
    }
}
