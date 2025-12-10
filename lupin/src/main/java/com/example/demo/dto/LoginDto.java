package com.example.demo.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginDto {
    private Long id;        // 유저 DB PK (user_id)
    private String userId;
    private String email;
    private String name;    // 사용자 실명
    private String department;
    private String role;    // 권한 (ROLE_USER 등)

    private String accessToken;
    private String refreshToken;

}