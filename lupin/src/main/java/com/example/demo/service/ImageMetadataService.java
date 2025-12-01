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
    public Optional<LocalDateTime> extractPhotoDateTime(String s3Key) {
        try {
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
            log.error("Failed to extract metadata from image: {}", s3Key, e);
            return Optional.empty();
        }
    }
}
