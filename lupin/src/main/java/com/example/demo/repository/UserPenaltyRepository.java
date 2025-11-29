package com.example.demo.repository;

import com.example.demo.domain.entity.User;
import com.example.demo.domain.entity.UserPenalty;
import com.example.demo.domain.enums.PenaltyType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface UserPenaltyRepository extends JpaRepository<UserPenalty, Long> {

    boolean existsByUserAndPenaltyTypeAndCreatedAtAfter(User user, PenaltyType penaltyType, LocalDateTime since);
}
