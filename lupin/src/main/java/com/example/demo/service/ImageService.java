package com.example.demo.service;

import com.example.demo.component.ImageProcessor;
import com.example.demo.config.ImagePolicyProperties;
import com.example.demo.infrastructure.FileStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext; // 추가
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 이미지 서비스 (Orchestrator)
 * - ImageProcessor와 FileStorage를 조율하여 이미지 업로드 흐름 관리
 * - 비즈니스 정책은 ImagePolicyProperties에서 주입 (12-Factor App)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageService {

    private static final String CONTENT_TYPE_WEBP = "image/webp";
    private static final String EXTENSION_WEBP = ".webp";

    private final FileStorage fileStorage;
    private final ImageProcessor imageProcessor;
    private final ImageMetadataService imageMetadataService;
    private final ImagePolicyProperties imagePolicy;
    private final ApplicationContext applicationContext; // 추가

    public String uploadImage(MultipartFile file) throws IOException {
        return uploadImage(file, null);
    }

    /**
     * 이미지 업로드 (메인 흐름)
     * 1. 원본 읽기
     * 2. WebP 변환 (ImageProcessor에 위임)
     * 3. S3 업로드 (FileStorage에 위임)
     * 4. EXIF 추출 (피드인 경우)
     * 5. 썸네일 비동기 생성
     */
    public String uploadImage(MultipartFile file, String prefix) throws IOException {
        byte[] originalBytes = file.getBytes();
        String uuid = UUID.randomUUID().toString();

        var original = imagePolicy.original();
        // 1. WebP 변환 (ImageProcessor에 위임)
        byte[] processedBytes = imageProcessor.convertToWebp(
                originalBytes, original.maxWidth(), original.maxHeight(), original.quality());
        log.info("Image converted: {} -> {} bytes", file.getOriginalFilename(), processedBytes.length);

        // 2. 파일명 생성 및 업로드 (FileStorage에 위임)
        String fileName = buildFileName(prefix, uuid, EXTENSION_WEBP);
        fileStorage.upload(fileName, processedBytes, CONTENT_TYPE_WEBP);

        // 3. 피드 이미지인 경우 EXIF 추출 및 캐싱
        if ("feed".equals(prefix)) {
            imageMetadataService.extractAndCache(originalBytes, fileName);
        }

        // 4. 썸네일 비동기 생성 (feed, profiles 폴더)
        if ("feed".equals(prefix) || "profiles".equals(prefix)) {
            // Self-invocation 문제 해결을 위해 프록시 빈을 통해 호출
            applicationContext.getBean(ImageService.class).generateThumbnailAsync(originalBytes, prefix, uuid);
        }

        return fileStorage.getPublicUrl(fileName);
    }

    /**
     * 썸네일 비동기 생성 및 업로드
     */
    @Async
    public void generateThumbnailAsync(byte[] originalBytes, String prefix, String uuid) {
        try {
            byte[] thumbBytes;

            if ("feed".equals(prefix)) {
                var feedThumb = imagePolicy.feedThumbnail();
                thumbBytes = imageProcessor.createFeedThumbnail(
                        originalBytes, feedThumb.width(), feedThumb.height(), feedThumb.quality());
                log.info("Feed thumbnail created async: {}x{}", feedThumb.width(), feedThumb.height());
            } else {
                // profiles
                var profileThumb = imagePolicy.profileThumbnail();
                thumbBytes = imageProcessor.createSquareThumbnail(
                        originalBytes, profileThumb.width(), profileThumb.quality());
                log.info("Profile thumbnail created async: {}x{}", profileThumb.width(), profileThumb.width());
            }

            String thumbFileName = prefix + "/thumb/" + uuid + EXTENSION_WEBP;
            fileStorage.upload(thumbFileName, thumbBytes, CONTENT_TYPE_WEBP);
            log.info("Thumbnail uploaded async: {}", thumbFileName);

        } catch (Exception e) {
            log.error("Async thumbnail generation failed: prefix={}, uuid={}", prefix, uuid, e);
        }
    }

    /**
     * 여러 이미지 업로드
     */
    public List<String> uploadImages(List<MultipartFile> files) throws IOException {
        return uploadImages(files, null);
    }

    public List<String> uploadImages(List<MultipartFile> files, String prefix) throws IOException {
        List<String> urls = new ArrayList<>();
        for (MultipartFile file : files) {
            urls.add(uploadImage(file, prefix));
        }
        return urls;
    }

    /**
     * 이미지 삭제 (FileStorage에 위임)
     */
    public void deleteImage(String imageIdOrUrl) {
        fileStorage.delete(imageIdOrUrl);
    }

    private String buildFileName(String prefix, String uuid, String extension) {
        if (prefix != null && !prefix.isEmpty()) {
            return prefix + "/" + uuid + extension;
        }
        return uuid + extension;
    }
}
