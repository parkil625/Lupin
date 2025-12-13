package com.example.demo.dto;

import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;

import java.util.List;

/**
 * 피드 이미지 업로드 결과를 캡슐화하는 Value Object
 * List<String> 인덱스 의존성 제거 및 타입 안전성 보장
 */
public record FeedImageUploadResult(
        String startImageKey,
        String endImageKey,
        List<String> otherImageKeys
) {
    /**
     * Compact constructor - 유효성 검증
     */
    public FeedImageUploadResult {
        if (startImageKey == null || endImageKey == null) {
            throw new BusinessException(ErrorCode.FEED_IMAGES_REQUIRED);
        }
        if (otherImageKeys == null) {
            otherImageKeys = List.of();
        }
    }

    /**
     * 시작/종료 이미지만으로 생성
     */
    public static FeedImageUploadResult of(String startImageKey, String endImageKey) {
        return new FeedImageUploadResult(startImageKey, endImageKey, List.of());
    }

    /**
     * 모든 이미지로 생성
     */
    public static FeedImageUploadResult of(String startImageKey, String endImageKey, List<String> otherImageKeys) {
        return new FeedImageUploadResult(startImageKey, endImageKey, otherImageKeys);
    }

    /**
     * List<String>으로부터 생성 (하위 호환용)
     * @param s3Keys [startImageKey, endImageKey, ...otherImageKeys]
     */
    public static FeedImageUploadResult fromList(List<String> s3Keys) {
        if (s3Keys == null || s3Keys.size() < 2) {
            throw new BusinessException(ErrorCode.FEED_IMAGES_REQUIRED);
        }
        String startImageKey = s3Keys.get(0);
        String endImageKey = s3Keys.get(1);
        List<String> otherImageKeys = s3Keys.size() > 2 ? s3Keys.subList(2, s3Keys.size()) : List.of();
        return new FeedImageUploadResult(startImageKey, endImageKey, otherImageKeys);
    }
}
