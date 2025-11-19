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
    private Long currentPoints;
    private Long totalPoints;
    private Integer feedCount;
    private Integer totalActivityMinutes;

    public static UserProfileResponse from(User user, Integer feedCount, Integer totalActivityMinutes) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .realName(user.getRealName())
                .role(user.getRole())
                .department(user.getDepartment())
                .currentPoints(user.getCurrentPoints())
                .totalPoints(user.getTotalPoints())
                .feedCount(feedCount)
                .totalActivityMinutes(totalActivityMinutes)
                .build();
    }
}
