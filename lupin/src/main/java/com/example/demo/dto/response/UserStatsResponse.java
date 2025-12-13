package com.example.demo.dto.response;

/**
 * 사용자 통계 정보를 담는 응답 DTO
 * Map<String, Object> 대신 타입 안전한 Record 사용
 */
public record UserStatsResponse(
        Long userId,
        long totalPoints,
        long feedCount,
        long commentCount
) {
    public static UserStatsResponse of(Long userId, Long totalPoints, long feedCount, long commentCount) {
        return new UserStatsResponse(
                userId,
                totalPoints != null ? totalPoints : 0L,
                feedCount,
                commentCount
        );
    }
}
