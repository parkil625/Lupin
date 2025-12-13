package com.example.demo.dto.response;

import com.example.demo.domain.entity.User;

/**
 * 사용자 랭킹 정보를 담는 응답 DTO
 * Map<String, Object> 대신 타입 안전한 Record 사용
 */
public record UserRankingResponse(
        Long id,
        String name,
        String avatar,
        String department,
        long points,
        int rank
) {
    public static UserRankingResponse of(User user, long points, int rank) {
        return new UserRankingResponse(
                user.getId(),
                user.getName(),
                user.getAvatar(),
                user.getDepartment(),
                points,
                rank
        );
    }

    /**
     * Native Query 결과로부터 생성 (getUserRankingContext용)
     */
    public static UserRankingResponse fromNativeQuery(Object[] row) {
        return new UserRankingResponse(
                ((Number) row[0]).longValue(),
                (String) row[1],
                (String) row[2],
                (String) row[3],
                // total_points가 NULL일 경우 0으로 처리
                row[4] != null ? ((Number) row[4]).longValue() : 0L,
                ((Number) row[5]).intValue()
        );
    }
}
