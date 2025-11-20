package com.example.demo.service;

import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import io.awspring.cloud.s3.S3Template;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 이미지 업로드 서비스 (S3)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageService {

    private final S3Template s3Template;

    @Value("${spring.cloud.aws.s3.bucket:lupin-images}")
    private String bucket;

    @Value("${cloud.aws.region.static:ap-northeast-2}")
    private String region;

    @Value("${cloud.aws.credentials.access-key:}")
    private String accessKey;

    @Value("${cloud.aws.credentials.secret-key:}")
    private String secretKey;

    private S3Client s3Client;

    @PostConstruct
    public void init() {
        if (!accessKey.isEmpty() && !secretKey.isEmpty()) {
            this.s3Client = S3Client.builder()
                    .region(Region.of(region))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(accessKey, secretKey)))
                    .build();
            log.info("S3 클라이언트 초기화 완료 - bucket: {}, region: {}", bucket, region);
        } else {
            log.warn("AWS 자격 증명이 설정되지 않았습니다. 로컬 모드로 동작합니다.");
        }
    }

    /**
     * 단일 이미지 업로드
     */
    public String uploadImage(MultipartFile file) {
        validateFile(file);

        // S3가 설정되지 않은 경우 더미 URL 반환 (개발용)
        if (s3Client == null) {
            log.info("S3 미설정 - 더미 URL 반환");
            return "https://images.unsplash.com/photo-1571019614242-c5c5dee9f50b?w=600&q=75";
        }

        try {
            String fileName = generateFileName(file);
            String key = "feeds/" + fileName;

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(putObjectRequest,
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            String imageUrl = String.format("https://%s.s3.%s.amazonaws.com/%s", bucket, region, key);
            log.info("이미지 업로드 완료: {}", imageUrl);

            return imageUrl;
        } catch (IOException e) {
            log.error("이미지 업로드 실패", e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    /**
     * 다중 이미지 업로드
     */
    public List<String> uploadMultipleImages(MultipartFile[] files) {
        List<String> urls = new ArrayList<>();
        for (MultipartFile file : files) {
            urls.add(uploadImage(file));
        }
        return urls;
    }

    /**
     * 파일 유효성 검사
     */
    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_FILE);
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException(ErrorCode.INVALID_FILE_TYPE);
        }

        // 10MB 제한
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new BusinessException(ErrorCode.FILE_SIZE_EXCEEDED);
        }
    }

    /**
     * 고유 파일명 생성
     */
    private String generateFileName(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return UUID.randomUUID().toString() + extension;
    }

    /**
     * S3 이미지 삭제
     */
    public void deleteImage(String url) {
        if (url == null || url.isEmpty()) return;

        try {
            String key = extractKeyFromUrl(url);
            if (key != null) {
                s3Template.deleteObject(bucket, key);
                log.info("이미지 삭제 완료: {}", key);
            }
        } catch (Exception e) {
            log.error("이미지 삭제 실패: {}", url, e);
            // 삭제 실패해도 예외 안 던짐
        }
    }

    /**
     * S3 URL에서 key 추출
     */
    private String extractKeyFromUrl(String url) {
        try {
            URL s3Url = new URL(url);
            String path = s3Url.getPath();
            return path.startsWith("/") ? path.substring(1) : path;
        } catch (Exception e) {
            log.error("URL 파싱 실패: {}", url);
            return null;
        }
    }
}
