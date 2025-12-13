package com.example.demo.repository;

import com.example.demo.dto.WriterActiveDays;

import java.time.LocalDateTime;
import java.util.List;

/**
 * QueryDSL을 사용한 Feed Repository 커스텀 인터페이스
 */
public interface FeedRepositoryCustom {

    /**
     * 여러 작성자의 활동 일수를 조회
     * Object[] 대신 타입 안전한 WriterActiveDays DTO 반환
     */
    List<WriterActiveDays> findActiveDaysByWriterIds(List<Long> writerIds, LocalDateTime start, LocalDateTime end);
}
