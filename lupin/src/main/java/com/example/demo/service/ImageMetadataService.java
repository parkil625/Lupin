package com.example.demo.service;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.example.demo.util.S3UrlUtils;
import io.awspring.cloud.s3.S3Template;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import org.springframework.data.redis.core.StringRedisTemplate;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageMetadataService {

    private final S3Template s3Template;
    private final StringRedisTemplate redisTemplate; // Redis 추가

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucket;

    /**
     * 바이트 배열에서 EXIF 추출 후 Redis에 임시 저장 (업로드 시 호출)
     */
    public Optional<LocalDateTime> extractAndCache(byte[] imageBytes, String fileName) {
        try {
            Optional<LocalDateTime> result = extractFromStream(new ByteArrayInputStream(imageBytes));
            
            if (result.isPresent()) {
                String cacheKey = fileName.contains("/") ? fileName.substring(fileName.lastIndexOf('/') + 1) : fileName;
                // [임시 저장] Redis에 24시간 동안 보관 (작성 취소하면 자동 만료됨)
                redisTemplate.opsForValue().set("img:meta:" + cacheKey, result.get().toString(), 24, TimeUnit.HOURS);
                log.info("EXIF saved to Redis (Temp) for {}: {}", cacheKey, result.get());
            }
            
            return result;
        } catch (Exception e) {
            log.warn("Failed to extract EXIF for caching: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * InputStream에서 촬영 시간 추출 (내부 사용)
     */
    private Optional<LocalDateTime> extractFromStream(InputStream inputStream) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(inputStream);
            ExifSubIFDDirectory directory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);

            if (directory != null) {
                Date dateOriginal = directory.getDateOriginal();
                if (dateOriginal != null) {
                    return Optional.of(dateOriginal.toInstant()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDateTime());
                }
            }
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Failed to extract EXIF from stream: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 이미지 촬영 시간 조회 (피드 저장 시 호출 - Redis 우선 확인)
     */
    public Optional<LocalDateTime> extractPhotoDateTime(String s3KeyOrUrl) {
        String s3Key = S3UrlUtils.extractFilename(s3KeyOrUrl);

        // 1. Redis(임시 저장소) 확인
        String cachedTime = redisTemplate.opsForValue().get("img:meta:" + s3Key);
        if (cachedTime != null) {
            log.info("EXIF Redis hit for {}: {}", s3Key, cachedTime);
            // 저장 완료 후에는 Redis에서 지워도 되지만, 24시간 뒤 자동 삭제되니 둬도 무방함
            return Optional.of(LocalDateTime.parse(cachedTime));
        }

        // 2. Redis 미스 - S3에서 다운로드하여 추출 (WebP 변환된 경우 실패 확률 높음)
        log.info("EXIF cache miss, downloading from S3: {}", s3Key);
        try {
            var s3Resource = s3Template.download(bucket, s3Key);

            try (InputStream inputStream = s3Resource.getInputStream()) {
                return extractFromStream(inputStream);
            }
        } catch (Exception e) {
            log.error("Failed to extract metadata from image: {}", s3KeyOrUrl, e);
            return Optional.empty();
        }
    }
}
