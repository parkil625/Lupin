package com.example.demo.scheduler;

import com.example.demo.repository.FeedImageRepository;
import io.awspring.cloud.s3.S3Template;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * S3 고아 이미지 정리 스케줄러
 * DB에 참조가 없는 오래된 S3 이미지를 정리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class S3OrphanImageCleanupScheduler {

    private static final String FEED_PREFIX = "feed/";
    private static final int ORPHAN_AGE_HOURS = 24; // 24시간 이상 된 고아 이미지만 삭제

    private final FeedImageRepository feedImageRepository;
    private final S3Template s3Template;
    private final S3Client s3Client;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucket;

    /**
     * 매일 새벽 5시에 고아 이미지 정리 실행
     */
    @Scheduled(cron = "0 0 5 * * *")
    public void cleanupOrphanImages() {
        log.info("S3 고아 이미지 정리 시작");

        try {
            // 1. DB에 저장된 모든 S3 키 조회
            Set<String> dbS3Keys = new HashSet<>(feedImageRepository.findAllS3Keys());
            log.info("DB에 등록된 이미지 수: {}", dbS3Keys.size());

            // 2. S3에서 feed/ 프리픽스의 모든 객체 조회
            int orphanCount = 0;
            int checkedCount = 0;
            Instant cutoffTime = Instant.now().minus(ORPHAN_AGE_HOURS, ChronoUnit.HOURS);

            String continuationToken = null;
            do {
                ListObjectsV2Request.Builder requestBuilder = ListObjectsV2Request.builder()
                        .bucket(bucket)
                        .prefix(FEED_PREFIX)
                        .maxKeys(1000);

                if (continuationToken != null) {
                    requestBuilder.continuationToken(continuationToken);
                }

                ListObjectsV2Response response = s3Client.listObjectsV2(requestBuilder.build());
                List<S3Object> objects = response.contents();

                for (S3Object s3Object : objects) {
                    String key = s3Object.key();
                    checkedCount++;

                    // 썸네일은 원본과 함께 관리되므로 스킵
                    if (key.contains("/thumb/")) {
                        continue;
                    }

                    // DB에 없고, 24시간 이상 된 파일만 삭제 (업로드 중인 파일 보호)
                    if (!dbS3Keys.contains(key) && !isUrlFormat(key, dbS3Keys)) {
                        if (s3Object.lastModified().isBefore(cutoffTime)) {
                            deleteWithThumbnail(key);
                            orphanCount++;
                        }
                    }
                }

                continuationToken = response.isTruncated() ? response.nextContinuationToken() : null;
            } while (continuationToken != null);

            if (orphanCount > 0) {
                log.info("S3 고아 이미지 정리 완료 - 검사: {}개, 삭제: {}개", checkedCount, orphanCount);
            } else {
                log.info("S3 고아 이미지 정리 완료 - 검사: {}개, 삭제할 이미지 없음", checkedCount);
            }

        } catch (Exception e) {
            log.error("S3 고아 이미지 정리 중 오류 발생", e);
        }
    }

    /**
     * URL 형식으로 DB에 저장되어 있는지 확인
     * (s3Key가 URL 형태로 저장된 경우 대응)
     */
    private boolean isUrlFormat(String s3Key, Set<String> dbS3Keys) {
        // URL 형식: https://cdn.lupin-care.com/feed/xxx.webp
        String urlFormat = "https://cdn.lupin-care.com/" + s3Key;
        return dbS3Keys.contains(urlFormat);
    }

    /**
     * 이미지와 해당 썸네일을 함께 삭제
     */
    private void deleteWithThumbnail(String key) {
        try {
            // 원본 삭제
            s3Template.deleteObject(bucket, key);
            log.debug("고아 이미지 삭제: {}", key);

            // 썸네일도 삭제 (존재하는 경우)
            String thumbKey = key.replace(FEED_PREFIX, FEED_PREFIX + "thumb/");
            try {
                s3Template.deleteObject(bucket, thumbKey);
                log.debug("썸네일 삭제: {}", thumbKey);
            } catch (Exception ignored) {
                // 썸네일이 없을 수 있음
            }
        } catch (Exception e) {
            log.warn("이미지 삭제 실패: {} - {}", key, e.getMessage());
        }
    }
}
