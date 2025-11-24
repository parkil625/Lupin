package com.example.demo.repository;

import com.example.demo.domain.entity.Report;
import com.example.demo.domain.enums.ReportTargetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReportRepository extends JpaRepository<Report, Long> {

    // 특정 대상에 대한 신고 수 조회
    Long countByTargetTypeAndTargetId(ReportTargetType targetType, Long targetId);

    // 같은 신고자가 같은 대상에 대해 신고한 기록 조회
    Optional<Report> findByReporterIdAndTargetTypeAndTargetId(Long reporterId, ReportTargetType targetType, Long targetId);

    // 특정 대상에 대한 모든 신고 조회
    List<Report> findByTargetTypeAndTargetId(ReportTargetType targetType, Long targetId);

    // 특정 대상에 대한 모든 신고 삭제
    void deleteByTargetTypeAndTargetId(ReportTargetType targetType, Long targetId);
}
