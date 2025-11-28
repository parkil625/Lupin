package com.example.demo.repository;

import com.example.demo.domain.entity.Comment;
import com.example.demo.domain.entity.Feed;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByFeedOrderByIdDesc(Feed feed);

    void deleteByFeed(Feed feed);

    List<Comment> findByParentOrderByIdAsc(Comment parent);

    long countByParent(Comment parent);

    @Query("SELECT c FROM Comment c LEFT JOIN CommentLike cl ON cl.comment = c " +
           "WHERE c.feed = :feed AND c.parent IS NULL " +
           "GROUP BY c ORDER BY COUNT(cl) DESC")
    List<Comment> findByFeedOrderByLikeCountDesc(@Param("feed") Feed feed);
}
