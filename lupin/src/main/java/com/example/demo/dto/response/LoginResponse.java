package com.example.demo.dto.response;

import com.example.demo.dto.LoginDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {

    private String accessToken;
    private String tokenType;
    private Long userId;
    private String email;
    private String name;
    private String role;

    public static LoginResponse from(LoginDto dto) {
        return LoginResponse.builder()
                .accessToken(dto.getAccessToken())
                .tokenType("Bearer")
                .userId(dto.getId())
                .email(dto.getEmail())
                .name(dto.getName())
                .role(dto.getRole())
                .build();
    }
}