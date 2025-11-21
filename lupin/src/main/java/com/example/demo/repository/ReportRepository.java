package com.example.demo.repository;

import com.example.demo.domain.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReportRepository extends JpaRepository<Report, Long> {

    // 특정 대상에 대한 신고 수 조회
    Long countByTargetTypeAndTargetId(String targetType, Long targetId);

    // 같은 신고자가 같은 대상에 대해 신고한 기록 조회
    Optional<Report> findByReporterIdAndTargetTypeAndTargetId(Long reporterId, String targetType, Long targetId);

    // 특정 사용자가 3일 내 신고당한 기록이 있는지 확인
    @Query("SELECT COUNT(r) > 0 FROM Report r WHERE r.reportedUser.id = :userId AND r.createdAt > :since")
    boolean hasRecentReport(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    // 특정 대상에 대한 모든 신고 조회
    List<Report> findByTargetTypeAndTargetId(String targetType, Long targetId);

    // 특정 대상에 대한 모든 신고 삭제
    void deleteByTargetTypeAndTargetId(String targetType, Long targetId);
}
