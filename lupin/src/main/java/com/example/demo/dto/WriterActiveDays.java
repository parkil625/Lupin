package com.example.demo.dto;

/**
 * 작성자별 활동 일수를 담는 타입 안전한 DTO
 * Object[] 대신 사용하여 타입 안전성 보장
 */
public record WriterActiveDays(
        Long writerId,
        Long activeDays
) {
}
