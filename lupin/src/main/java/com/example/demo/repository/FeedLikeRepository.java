package com.example.demo.repository;

import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.FeedLike;
import com.example.demo.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FeedLikeRepository extends JpaRepository<FeedLike, Long> {

    boolean existsByUserAndFeed(User user, Feed feed);

    Optional<FeedLike> findByUserAndFeed(User user, Feed feed);

    void deleteByUserAndFeed(User user, Feed feed);

    void deleteByFeed(Feed feed);

    long countByFeed(Feed feed);
}
