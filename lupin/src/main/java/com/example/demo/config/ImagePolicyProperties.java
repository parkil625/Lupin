package com.example.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 이미지 정책 설정 (12-Factor App - 코드와 설정의 분리)
 */
@ConfigurationProperties(prefix = "image.policy")
public record ImagePolicyProperties(
        OriginalPolicy original,
        ThumbnailPolicy feedThumbnail,
        ThumbnailPolicy profileThumbnail
) {
    public ImagePolicyProperties {
        // 기본값 설정
        if (original == null) {
            original = new OriginalPolicy(800, 800, 60);
        }
        if (feedThumbnail == null) {
            feedThumbnail = new ThumbnailPolicy(300, 400, 50);
        }
        if (profileThumbnail == null) {
            profileThumbnail = new ThumbnailPolicy(100, 100, 60);
        }
    }

    /**
     * 원본 이미지 정책
     */
    public record OriginalPolicy(
            int maxWidth,
            int maxHeight,
            int quality
    ) {}

    /**
     * 썸네일 정책
     */
    public record ThumbnailPolicy(
            int width,
            int height,
            int quality
    ) {}
}
