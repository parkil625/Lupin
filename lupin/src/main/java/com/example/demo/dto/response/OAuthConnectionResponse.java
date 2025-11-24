package com.example.demo.dto.response;

import com.example.demo.domain.entity.UserOAuth;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OAuthConnectionResponse {

    private Long id;
    private String provider;
    private String providerEmail;
    private LocalDateTime connectedAt;

    public static OAuthConnectionResponse from(UserOAuth userOAuth) {
        return OAuthConnectionResponse.builder()
                .id(userOAuth.getId())
                .provider(userOAuth.getProvider().name())
                .providerEmail(userOAuth.getProviderEmail())
                .connectedAt(userOAuth.getCreatedAt())
                .build();
    }
}
