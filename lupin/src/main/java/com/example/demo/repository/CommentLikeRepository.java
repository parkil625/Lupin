package com.example.demo.repository;

import com.example.demo.domain.entity.Comment;
import com.example.demo.domain.entity.CommentLike;
import com.example.demo.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {

    // 특정 사용자가 특정 댓글을 좋아요 했는지 확인
    boolean existsByUserIdAndCommentId(Long userId, Long commentId);

    // Comment와 User 객체로 존재 여부 확인
    boolean existsByCommentAndUser(Comment comment, User user);

    // 좋아요 조회
    Optional<CommentLike> findByUserIdAndCommentId(Long userId, Long commentId);

    // 댓글의 좋아요 개수
    Long countByCommentId(Long commentId);

    // 특정 댓글의 모든 좋아요 조회
    List<CommentLike> findAllByComment(Comment comment);
}
