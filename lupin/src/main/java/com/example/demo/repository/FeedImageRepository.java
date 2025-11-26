package com.example.demo.repository;

import com.example.demo.domain.entity.FeedImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FeedImageRepository extends JpaRepository<FeedImage, Long> {

}
