package com.example.demo.repository;

import com.example.demo.domain.entity.Comment;
import com.example.demo.domain.entity.CommentLike;
import com.example.demo.domain.entity.Feed;
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

    boolean existsByUserAndComment(User user, Comment comment);

    Optional<CommentLike> findByUserAndComment(User user, Comment comment);

    void deleteByUserAndComment(User user, Comment comment);

    long countByComment(Comment comment);

    void deleteByComment(Comment comment);

    @Modifying
    @Query("DELETE FROM CommentLike cl WHERE cl.comment.feed = :feed")
    void deleteByFeed(@Param("feed") Feed feed);

    List<CommentLike> findByComment(Comment comment);

    @Query("SELECT cl FROM CommentLike cl JOIN FETCH cl.comment WHERE cl.id = :id")
    Optional<CommentLike> findByIdWithComment(@Param("id") Long id);
}
