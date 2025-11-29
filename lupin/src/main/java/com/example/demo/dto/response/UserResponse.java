package com.example.demo.dto.response;

import com.example.demo.domain.entity.User;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserResponse {

    private Long id;
    private String userId;
    private String name;
    private Double height;
    private Double weight;
    private String gender;
    private String avatar;

    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .userId(user.getUserId())
                .name(user.getName())
                .height(user.getHeight())
                .weight(user.getWeight())
                .gender(user.getGender())
                .avatar(user.getAvatar())
                .build();
    }
}
