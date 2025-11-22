package com.example.demo.controller;

import com.example.demo.dto.request.CommentCreateRequest;
import com.example.demo.dto.response.CommentResponse;
import com.example.demo.service.CommentCommandService;
import com.example.demo.service.CommentQueryService;
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
import java.util.Map;

/**
 * 댓글 관련 API
 * CQRS 패턴 적용 - Command/Query 서비스 분리
 */
@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentCommandService commentCommandService;
    private final CommentQueryService commentQueryService;

    // ========== Command API ==========

    /**
     * 댓글 생성
     */
    @PostMapping("/feeds/{feedId}")
    public ResponseEntity<Map<String, Long>> createComment(
            @PathVariable Long feedId,
            @RequestParam Long userId,
            @Valid @RequestBody CommentCreateRequest request) {
        Long commentId = commentCommandService.createComment(feedId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("commentId", commentId));
    }

    /**
     * 댓글 수정
     */
    @PutMapping("/{commentId}")
    public ResponseEntity<Void> updateComment(
            @PathVariable Long commentId,
            @RequestParam Long userId,
            @RequestParam String content) {
        commentCommandService.updateComment(commentId, userId, content);
        return ResponseEntity.ok().build();
    }

    /**
     * 댓글 삭제
     */
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long commentId,
            @RequestParam Long userId) {
        commentCommandService.deleteComment(commentId, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 댓글 좋아요
     */
    @PostMapping("/{commentId}/like")
    public ResponseEntity<Void> likeComment(
            @PathVariable Long commentId,
            @RequestParam Long userId) {
        commentCommandService.likeComment(commentId, userId);
        return ResponseEntity.ok().build();
    }

    /**
     * 댓글 좋아요 취소
     */
    @DeleteMapping("/{commentId}/like")
    public ResponseEntity<Void> unlikeComment(
            @PathVariable Long commentId,
            @RequestParam Long userId) {
        commentCommandService.unlikeComment(commentId, userId);
        return ResponseEntity.noContent().build();
    }

    // ========== Query API ==========

    /**
     * 특정 피드의 댓글 목록 조회 (페이징)
     */
    @GetMapping("/feeds/{feedId}")
    public ResponseEntity<Page<CommentResponse>> getCommentsByFeedId(
            @PathVariable Long feedId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<CommentResponse> comments = commentQueryService.getCommentsByFeedId(feedId, pageable);
        return ResponseEntity.ok(comments);
    }

    /**
     * 특정 피드의 댓글 목록 조회 (전체)
     */
    @GetMapping("/feeds/{feedId}/all")
    public ResponseEntity<List<CommentResponse>> getAllCommentsByFeedId(@PathVariable Long feedId) {
        List<CommentResponse> comments = commentQueryService.getAllCommentsByFeedId(feedId);
        return ResponseEntity.ok(comments);
    }

    /**
     * 특정 댓글의 답글 조회
     */
    @GetMapping("/{commentId}/replies")
    public ResponseEntity<List<CommentResponse>> getRepliesByCommentId(@PathVariable Long commentId) {
        List<CommentResponse> replies = commentQueryService.getRepliesByCommentId(commentId);
        return ResponseEntity.ok(replies);
    }

    /**
     * 댓글 상세 조회
     */
    @GetMapping("/{commentId}")
    public ResponseEntity<CommentResponse> getCommentDetail(@PathVariable Long commentId) {
        CommentResponse comment = commentQueryService.getCommentDetail(commentId);
        return ResponseEntity.ok(comment);
    }

    /**
     * 특정 피드의 댓글 수 조회
     */
    @GetMapping("/feeds/{feedId}/count")
    public ResponseEntity<Long> getCommentCountByFeedId(@PathVariable Long feedId) {
        Long count = commentQueryService.getCommentCountByFeedId(feedId);
        return ResponseEntity.ok(count);
    }

    /**
     * 특정 사용자의 댓글 조회
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<Page<CommentResponse>> getCommentsByUserId(
            @PathVariable Long userId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<CommentResponse> comments = commentQueryService.getCommentsByUserId(userId, pageable);
        return ResponseEntity.ok(comments);
    }
}
