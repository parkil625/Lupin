package com.example.demo.repository;

import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.FeedImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeedImageRepository extends JpaRepository<FeedImage, Long> {

    List<FeedImage> findByFeedOrderBySortOrderAsc(Feed feed);

    void deleteByFeed(Feed feed);
}
