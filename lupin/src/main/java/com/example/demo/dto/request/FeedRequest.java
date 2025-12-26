package com.example.demo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class FeedRequest {

    @NotBlank(message = "활동은 필수입니다")
    private String activity;

    @Size(max = 50000, message = "피드 내용이 너무 깁니다")
    private String content;

    // 타입별 이미지 필드 (권장)
    private String startImage;
    private String endImage;
    private List<String> otherImages = new ArrayList<>();

    // 기존 images 필드 (하위 호환성)
    @Deprecated
    private List<String> images = new ArrayList<>();

    // [추가] 이미지 변경 여부 플래그 (기본형 boolean -> 참조형 Boolean으로 변경하여 매핑 안정성 확보)
    private Boolean imagesChanged;

    // [추가] 프론트에서 전달받는 EXIF 시간
    private LocalDateTime startAt;
    private LocalDateTime endAt;

    @Builder
    public FeedRequest(String activity, String content, String startImage, String endImage, List<String> otherImages, List<String> images, Boolean imagesChanged, LocalDateTime startAt, LocalDateTime endAt) {
        this.activity = activity;
        this.content = content;
        this.startImage = startImage;
        this.endImage = endImage;
        this.otherImages = otherImages != null ? otherImages : new ArrayList<>();
        this.images = images != null ? images : new ArrayList<>();
        this.imagesChanged = imagesChanged;
        this.startAt = startAt;
        this.endAt = endAt;
    }

    /**
     * 타입별 필드가 있으면 그걸 사용하고, 없으면 기존 images 배열 사용 (하위 호환)
     */
    public List<String> getAllImagesInOrder() {
        // 타입별 필드가 있으면 그걸 사용
        if (startImage != null || endImage != null) {
            List<String> result = new ArrayList<>();
            if (startImage != null) result.add(startImage);
            if (endImage != null) result.add(endImage);
            if (otherImages != null) result.addAll(otherImages);
            return result;
        }
        // 기존 images 배열 사용 (하위 호환)
        return images != null ? images : new ArrayList<>();
    }

    /**
     * 시작 이미지 가져오기 (타입별 필드 우선)
     */
    public String getStartImageKey() {
        if (startImage != null) return startImage;
        return images != null && !images.isEmpty() ? images.get(0) : null;
    }

    /**
     * 끝 이미지 가져오기 (타입별 필드 우선)
     */
    public String getEndImageKey() {
        if (endImage != null) return endImage;
        return images != null && images.size() > 1 ? images.get(1) : null;
    }

    /**
     * 기타 이미지 가져오기 (타입별 필드 우선)
     */
    public List<String> getOtherImageKeys() {
        if (otherImages != null && !otherImages.isEmpty()) return otherImages;
        if (images != null && images.size() > 2) return images.subList(2, images.size());
        return new ArrayList<>();
    }
}
