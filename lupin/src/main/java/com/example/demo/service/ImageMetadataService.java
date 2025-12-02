package com.example.demo.service;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import io.awspring.cloud.s3.S3Template;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageMetadataService {

    private final S3Template s3Template;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucket;

    /**
     * S3 이미지에서 촬영 시간 추출
     */
    public Optional<LocalDateTime> extractPhotoDateTime(String s3KeyOrUrl) {
        try {
            // URL인 경우 S3 키만 추출
            String s3Key = extractS3Key(s3KeyOrUrl);
            log.info("Extracting EXIF from S3 key: {}", s3Key);

            // S3에서 이미지 다운로드
            var s3Resource = s3Template.download(bucket, s3Key);

            try (InputStream inputStream = s3Resource.getInputStream()) {
                Metadata metadata = ImageMetadataReader.readMetadata(inputStream);

                // EXIF 디렉토리에서 촬영 시간 추출
                ExifSubIFDDirectory directory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);

                if (directory != null) {
                    Date dateOriginal = directory.getDateOriginal();
                    if (dateOriginal != null) {
                        LocalDateTime dateTime = dateOriginal.toInstant()
                                .atZone(ZoneId.systemDefault())
                                .toLocalDateTime();
                        log.debug("Extracted photo date time: {} from {}", dateTime, s3Key);
                        return Optional.of(dateTime);
                    }
                }

                log.warn("No EXIF date found in image: {}", s3Key);
                return Optional.empty();
            }
        } catch (Exception e) {
            log.error("Failed to extract metadata from image: {}", s3KeyOrUrl, e);
            return Optional.empty();
        }
    }

    /**
     * URL 또는 S3 키에서 S3 키만 추출
     * 예: https://bucket.s3.region.amazonaws.com/uuid.jpg → uuid.jpg
     */
    private String extractS3Key(String s3KeyOrUrl) {
        if (s3KeyOrUrl == null || s3KeyOrUrl.isEmpty()) {
            return s3KeyOrUrl;
        }

        // 이미 키만 있는 경우 (URL이 아닌 경우)
        if (!s3KeyOrUrl.startsWith("http")) {
            return s3KeyOrUrl;
        }

        // URL에서 마지막 / 이후의 파일명만 추출
        int lastSlashIndex = s3KeyOrUrl.lastIndexOf('/');
        if (lastSlashIndex >= 0 && lastSlashIndex < s3KeyOrUrl.length() - 1) {
            return s3KeyOrUrl.substring(lastSlashIndex + 1);
        }

        return s3KeyOrUrl;
    }
}
