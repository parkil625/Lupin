package com.example.demo.infrastructure;

import com.example.demo.util.S3UrlUtils;
import io.awspring.cloud.s3.S3Template;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * S3 파일 저장소 구현체
 * - AWS S3에 파일 업로드/삭제 담당
 * - CDN URL 생성 담당
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class S3FileStorage implements FileStorage {

    private static final String CACHE_CONTROL = "max-age=31536000, immutable"; // 1년 캐싱
    private static final String CDN_URL_PREFIX = "https://cdn.lupin-care.com";

    private final S3Client s3Client;
    private final S3Template s3Template;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucket;

    @Override
    public void upload(String path, byte[] content, String contentType) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(path)
                .contentType(contentType)
                .cacheControl(CACHE_CONTROL)
                .build();

        s3Client.putObject(request, RequestBody.fromBytes(content));
        log.debug("File uploaded to S3: path={}, size={} bytes", path, content.length);
    }

    @Override
    public void delete(String pathOrUrl) {
        // URL인 경우 S3 키만 추출
        String s3Key = S3UrlUtils.extractPath(pathOrUrl, bucket);
        s3Template.deleteObject(bucket, s3Key);
        log.debug("File deleted from S3: {}", s3Key);
    }

    @Override
    public String getPublicUrl(String path) {
        return String.format("%s/%s", CDN_URL_PREFIX, path);
    }
}
