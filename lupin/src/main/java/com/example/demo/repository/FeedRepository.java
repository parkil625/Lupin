package com.example.demo.repository;

import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FeedRepository extends JpaRepository<Feed, Long> {

    @EntityGraph(attributePaths = {"writer", "images"})
    Slice<Feed> findByWriterOrderByIdDesc(User writer, Pageable pageable);

    @EntityGraph(attributePaths = {"writer", "images"})
    Slice<Feed> findByWriterNotOrderByIdDesc(User writer, Pageable pageable);

    @EntityGraph(attributePaths = {"writer", "images"})
    Slice<Feed> findByWriterNameContainingOrderByIdDesc(String name, Pageable pageable);

    boolean existsByWriterAndCreatedAtBetween(User writer, java.time.LocalDateTime start, java.time.LocalDateTime end);

    @EntityGraph(attributePaths = {"writer", "images"})
    Optional<Feed> findById(Long id);
}
