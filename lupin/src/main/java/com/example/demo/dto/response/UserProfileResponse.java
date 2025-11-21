package com.example.demo.dto.response;

import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileResponse {

    private Long id;
    private String email;
    private String realName;
    private Role role;
    private String department;
    private Long currentPoints;    // 추첨권 계산용 잔여 포인트
    private Long monthlyPoints;    // 이번 달 누적 포인트 (랭킹용)
    private Integer feedCount;

    public static UserProfileResponse from(User user, Integer feedCount) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .realName(user.getRealName())
                .role(user.getRole())
                .department(user.getDepartment())
                .currentPoints(user.getCurrentPoints())
                .monthlyPoints(user.getMonthlyPoints())
                .feedCount(feedCount)
                .build();
    }
}
