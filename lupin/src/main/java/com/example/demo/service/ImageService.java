package com.example.demo.service;

import com.example.demo.util.S3UrlUtils;
import com.sksamuel.scrimage.ImmutableImage;
import com.sksamuel.scrimage.webp.WebpWriter;
import io.awspring.cloud.s3.S3Template;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageService {

    private static final int MAX_WIDTH = 800;
    private static final int MAX_HEIGHT = 800;
    private static final int WEBP_QUALITY = 60;

    // 피드 썸네일 설정
    private static final int THUMB_WIDTH = 300;
    private static final int THUMB_HEIGHT = 400;
    private static final int THUMB_QUALITY = 50;

    // 프로필 썸네일 설정 (작은 아바타용)
    private static final int PROFILE_THUMB_SIZE = 100;
    private static final int PROFILE_THUMB_QUALITY = 60;

    private final S3Template s3Template;
    private final S3Client s3Client;
    private final ImageMetadataService imageMetadataService;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucket;

    public String uploadImage(MultipartFile file) throws IOException {
        return uploadImage(file, null);
    }

    public String uploadImage(MultipartFile file, String prefix) throws IOException {
        // WebP 변환 시도
        byte[] imageBytes;
        byte[] originalBytes = file.getBytes(); // 비동기 썸네일 생성용 원본 저장
        String extension;
        String contentType;

        try {
            ImmutableImage image = ImmutableImage.loader().fromStream(
                new ByteArrayInputStream(originalBytes)
            );

            // 원본 리사이징 + WebP 변환
            if (image.width > MAX_WIDTH || image.height > MAX_HEIGHT) {
                image = image.bound(MAX_WIDTH, MAX_HEIGHT);
            }
            imageBytes = image.bytes(WebpWriter.DEFAULT.withQ(WEBP_QUALITY));

            extension = ".webp";
            contentType = "image/webp";
            log.info("Image converted to WebP: {} -> {} bytes", file.getOriginalFilename(), imageBytes.length);
        } catch (Exception e) {
            // WebP 변환 실패 시 원본 사용 (fallback)
            log.warn("WebP conversion failed, using original: {}", e.getMessage());
            imageBytes = originalBytes;
            extension = getExtension(file.getOriginalFilename());
            contentType = file.getContentType();
        }

        // 고유한 파일명 생성 (UUID)
        String uuid = UUID.randomUUID().toString();
        String fileName = uuid + extension;

        // prefix가 있으면 폴더 경로 추가
        if (prefix != null && !prefix.isEmpty()) {
            fileName = prefix + "/" + fileName;
        }

        // S3에 원본 업로드 (Cache-Control 헤더 포함)
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(fileName)
                .contentType(contentType)
                .cacheControl("max-age=31536000, immutable") // 1년 캐싱
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(imageBytes));

        // 피드 이미지인 경우 EXIF 추출 및 캐싱 (나중에 피드 생성 시 재사용)
        if ("feed".equals(prefix)) {
            imageMetadataService.extractAndCache(originalBytes, fileName);
        }

        // 썸네일 비동기 생성 (feed, profiles 폴더)
        if ("feed".equals(prefix) || "profiles".equals(prefix)) {
            generateThumbnailAsync(originalBytes, prefix, uuid, extension, contentType);
        }

        // URL 생성 (Cloudflare CDN 사용)
        return String.format("https://cdn.lupin-care.com/%s", fileName);
    }

    /**
     * 썸네일 비동기 생성 및 S3 업로드
     */
    @Async
    public void generateThumbnailAsync(byte[] originalBytes, String prefix, String uuid, String extension, String contentType) {
        try {
            byte[] thumbBytes;
            ImmutableImage thumbImage = ImmutableImage.loader().fromStream(
                new ByteArrayInputStream(originalBytes)
            );

            if ("feed".equals(prefix)) {
                thumbImage = thumbImage.cover(THUMB_WIDTH, THUMB_HEIGHT);
                thumbBytes = thumbImage.bytes(WebpWriter.DEFAULT.withQ(THUMB_QUALITY));
                log.info("Feed thumbnail created async: {}x{}, {} bytes", THUMB_WIDTH, THUMB_HEIGHT, thumbBytes.length);
            } else {
                // profiles
                thumbImage = thumbImage.cover(PROFILE_THUMB_SIZE, PROFILE_THUMB_SIZE);
                thumbBytes = thumbImage.bytes(WebpWriter.DEFAULT.withQ(PROFILE_THUMB_QUALITY));
                log.info("Profile thumbnail created async: {}x{}, {} bytes", PROFILE_THUMB_SIZE, PROFILE_THUMB_SIZE, thumbBytes.length);
            }

            String thumbFileName = prefix + "/thumb/" + uuid + extension;
            PutObjectRequest thumbRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(thumbFileName)
                    .contentType(contentType)
                    .cacheControl("max-age=31536000, immutable")
                    .build();
            s3Client.putObject(thumbRequest, RequestBody.fromBytes(thumbBytes));
            log.info("Thumbnail uploaded async: {}", thumbFileName);
        } catch (Exception e) {
            log.error("Async thumbnail generation failed: prefix={}, uuid={}", prefix, uuid, e);
        }
    }

    private String getExtension(String filename) {
        if (filename != null && filename.contains(".")) {
            return filename.substring(filename.lastIndexOf("."));
        }
        return ".jpg";
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
        // URL인 경우 S3 키만 추출
        String s3Key = S3UrlUtils.extractPath(imageIdOrUrl, bucket);
        s3Template.deleteObject(bucket, s3Key);
    }
}