package com.example.demo.oauth;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * OAuth 사용자 정보 통합 DTO
 */
@Getter
@AllArgsConstructor
public class OAuthUserInfo {
    private final String id;
    private final String email;
    private final String name;
}
