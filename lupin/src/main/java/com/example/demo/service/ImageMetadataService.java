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
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageMetadataService {

    private final S3Template s3Template;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucket;

    // EXIF 캐시 (업로드 시 추출 -> 피드 생성 시 사용)
    private final Map<String, Optional<LocalDateTime>> exifCache = new ConcurrentHashMap<>();

    /**
     * 바이트 배열에서 EXIF 추출 후 캐시에 저장 (업로드 시 호출)
     * @param imageBytes 원본 이미지 바이트
     * @param fileName 캐시 키로 사용할 파일명 (UUID 포함)
     */
    public Optional<LocalDateTime> extractAndCache(byte[] imageBytes, String fileName) {
        try {
            Optional<LocalDateTime> result = extractFromStream(new ByteArrayInputStream(imageBytes));
            // 파일명에서 경로 제거하고 파일명만 추출
            String cacheKey = fileName.contains("/") ? fileName.substring(fileName.lastIndexOf('/') + 1) : fileName;
            exifCache.put(cacheKey, result);
            log.debug("EXIF cached for {}: {}", cacheKey, result.orElse(null));
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
     * S3 이미지에서 촬영 시간 추출 (캐시 우선 확인)
     */
    public Optional<LocalDateTime> extractPhotoDateTime(String s3KeyOrUrl) {
        // URL인 경우 파일명만 추출
        String s3Key = S3UrlUtils.extractFilename(s3KeyOrUrl);

        // 1. 캐시 확인 (업로드 시 이미 추출된 경우)
        if (exifCache.containsKey(s3Key)) {
            Optional<LocalDateTime> cached = exifCache.remove(s3Key); // 사용 후 제거
            log.debug("EXIF cache hit for {}: {}", s3Key, cached.orElse(null));
            return cached;
        }

        // 2. 캐시 미스 - S3에서 다운로드하여 추출
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
