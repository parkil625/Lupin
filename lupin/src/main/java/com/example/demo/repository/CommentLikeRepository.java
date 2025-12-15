package com.example.demo.repository;

import com.example.demo.domain.entity.Comment;
import com.example.demo.domain.entity.CommentLike;
import com.example.demo.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {

    @Query("SELECT COUNT(cl) > 0 FROM CommentLike cl WHERE cl.user.id = :userId AND cl.comment.id = :commentId")
    boolean existsByUserIdAndCommentId(@Param("userId") Long userId, @Param("commentId") Long commentId);

    @Query("SELECT cl FROM CommentLike cl WHERE cl.user.id = :userId AND cl.comment.id = :commentId")
    Optional<CommentLike> findByUserIdAndCommentId(@Param("userId") Long userId, @Param("commentId") Long commentId);

    long countByComment(Comment comment);

    void deleteByComment(Comment comment);

    @Modifying
    @Query("DELETE FROM CommentLike cl WHERE cl.comment.id = :commentId")
    void deleteByCommentId(@Param("commentId") Long commentId);

    @Modifying
    @Query("DELETE FROM CommentLike cl WHERE cl.comment.id IN :commentIds")
    void deleteByCommentIds(@Param("commentIds") List<Long> commentIds);

    @Modifying
    @Query("DELETE FROM CommentLike cl WHERE cl.comment.feed.id = :feedId")
    void deleteByFeedId(@Param("feedId") Long feedId);

    List<CommentLike> findByComment(Comment comment);

    @Query("SELECT cl FROM CommentLike cl JOIN FETCH cl.comment WHERE cl.id = :id")
    Optional<CommentLike> findByIdWithComment(@Param("id") Long id);

    @Query("SELECT cl.comment.id, COUNT(cl) FROM CommentLike cl WHERE cl.comment.id IN :commentIds GROUP BY cl.comment.id")
    List<Object[]> countByCommentIds(@Param("commentIds") List<Long> commentIds);

    @Query("SELECT cl.comment.id FROM CommentLike cl WHERE cl.user.id = :userId AND cl.comment.id IN :commentIds")
    List<Long> findLikedCommentIdsByUserId(@Param("userId") Long userId, @Param("commentIds") List<Long> commentIds);

    @Modifying
    @Query("DELETE FROM CommentLike cl WHERE cl.comment IN :comments")
    void deleteByCommentIn(@Param("comments") List<Comment> comments);
}
