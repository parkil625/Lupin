package com.example.demo.service;

import com.example.demo.component.ImageProcessor;
import com.example.demo.config.ImagePolicyProperties;
import com.example.demo.infrastructure.FileStorage;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private static final String S3_THUMBNAIL_RETRY = "s3-thumbnail";

    private final FileStorage fileStorage;
    private final ImageProcessor imageProcessor;
    private final ImageMetadataService imageMetadataService;
    private final ImagePolicyProperties imagePolicy;

    public String uploadImage(MultipartFile file) throws IOException {
        return uploadImage(file, null);
    }

    public String uploadImage(MultipartFile file, String prefix) throws IOException {
        byte[] originalBytes = file.getBytes();
        String uuid = UUID.randomUUID().toString();

        var original = imagePolicy.original();
        byte[] processedBytes = imageProcessor.convertToWebp(
                originalBytes, original.maxWidth(), original.maxHeight(), original.quality());
        log.info("Image converted: {} -> {} bytes", file.getOriginalFilename(), processedBytes.length);

        String fileName = buildFileName(prefix, uuid, EXTENSION_WEBP);
        fileStorage.upload(fileName, processedBytes, CONTENT_TYPE_WEBP);

        if ("feed".equals(prefix)) {
            imageMetadataService.extractAndCache(originalBytes, fileName);
        }

        if ("feed".equals(prefix) || "profiles".equals(prefix)) {
            generateThumbnailAsync(originalBytes, prefix, uuid);
        }

        return fileStorage.getPublicUrl(fileName);
    }

    @Async
    @Retry(name = S3_THUMBNAIL_RETRY, fallbackMethod = "recoverThumbnailGeneration")
    public void generateThumbnailAsync(byte[] originalBytes, String prefix, String uuid) throws IOException {
        byte[] thumbBytes;

        if ("feed".equals(prefix)) {
            var feedThumb = imagePolicy.feedThumbnail();
            thumbBytes = imageProcessor.createFeedThumbnail(
                    originalBytes, feedThumb.width(), feedThumb.height(), feedThumb.quality());
            log.info("Feed thumbnail created async: {}x{}", feedThumb.width(), feedThumb.height());
        } else {
            var profileThumb = imagePolicy.profileThumbnail();
            thumbBytes = imageProcessor.createSquareThumbnail(
                    originalBytes, profileThumb.width(), profileThumb.quality());
            log.info("Profile thumbnail created async: {}x{}", profileThumb.width(), profileThumb.width());
        }

        String thumbFileName = prefix + "/thumb/" + uuid + EXTENSION_WEBP;
        fileStorage.upload(thumbFileName, thumbBytes, CONTENT_TYPE_WEBP);
        log.info("Thumbnail uploaded async: {}", thumbFileName);
    }

    /**
     * 썸네일 생성 재시도 실패 시 복구 메서드
     */
    @SuppressWarnings("unused")
    private void recoverThumbnailGeneration(byte[] originalBytes, String prefix, String uuid, Throwable throwable) {
        log.error("Async thumbnail generation failed after retries: prefix={}, uuid={}, error={}",
                prefix, uuid, throwable.getMessage());
        // TODO: 실패한 작업을 DB나 별도 로그에 기록하여 추후 처리할 수 있도록 구현
    }

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
