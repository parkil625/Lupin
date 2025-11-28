package com.example.demo.repository;

import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FeedRepository extends JpaRepository<Feed, Long> {

    Slice<Feed> findByWriterOrderByIdDesc(User writer, Pageable pageable);

    Slice<Feed> findByWriterNotOrderByIdDesc(User writer, Pageable pageable);

    Slice<Feed> findByWriterNameContainingOrderByIdDesc(String name, Pageable pageable);
}
