package com.example.demo.dto.response;

import com.example.demo.domain.entity.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class UserResponse {

    private Long id;
    private String userId;
    private String name;
    private Double height;
    private Double weight;
    private String gender;
    private LocalDate birthDate;
    private String avatar;
    private String department;
    private Long currentPoints;
    private String status;
    private boolean hasFeedPenalty; // [추가] 피드 작성 제한 여부

    public static UserResponse from(User user) {
        return from(user, 0L, false);
    }

    public static UserResponse from(User user, Long currentPoints) {
        return from(user, currentPoints, false);
    }

    // [수정] hasFeedPenalty를 포함한 오버로딩 메서드 추가
    public static UserResponse from(User user, Long currentPoints, boolean hasFeedPenalty) {
        return UserResponse.builder()
                .id(user.getId())
                .userId(user.getUserId())
                .name(user.getName())
                .height(user.getHeight())
                .weight(user.getWeight())
                .gender(user.getGender())
                .birthDate(user.getBirthDate())
                .avatar(user.getAvatar())
                .department(user.getDepartment())
                .currentPoints(currentPoints)
                .status(user.getStatus().name())
                .hasFeedPenalty(hasFeedPenalty) // [추가] 값 설정
                .build();
    }
}