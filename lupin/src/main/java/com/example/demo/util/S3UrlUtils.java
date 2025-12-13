package com.example.demo.util;

/**
 * S3 URL 처리 유틸리티 클래스
 * URL에서 S3 키 또는 파일명을 추출하는 공통 로직 제공
 */
public final class S3UrlUtils {

    private S3UrlUtils() {
        // 유틸리티 클래스는 인스턴스화 방지
    }

    /**
     * URL 또는 S3 키에서 파일명만 추출
     * 예: https://bucket.s3.region.amazonaws.com/path/uuid.jpg → uuid.jpg
     *
     * @param s3KeyOrUrl S3 URL 또는 키
     * @return 파일명 또는 원본 값
     */
    public static String extractFilename(String s3KeyOrUrl) {
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

    /**
     * URL 또는 S3 키에서 전체 S3 경로 추출
     * 예: https://bucket.s3.region.amazonaws.com/feed/abc123.jpg → feed/abc123.jpg
     *
     * @param s3KeyOrUrl S3 URL 또는 키
     * @param bucket S3 버킷명
     * @return S3 경로 또는 원본 값
     */
    public static String extractPath(String s3KeyOrUrl, String bucket) {
        if (s3KeyOrUrl == null || s3KeyOrUrl.isEmpty()) {
            return s3KeyOrUrl;
        }

        // 이미 S3 키인 경우 (http로 시작하지 않음)
        if (!s3KeyOrUrl.startsWith("http")) {
            return s3KeyOrUrl;
        }

        // S3 URL에서 버킷 이름 이후의 전체 경로 추출
        String bucketPrefix = bucket + ".s3";
        int bucketIndex = s3KeyOrUrl.indexOf(bucketPrefix);

        if (bucketIndex >= 0) {
            // 버킷 이름 이후 첫 번째 슬래시 찾기
            int firstSlashAfterBucket = s3KeyOrUrl.indexOf('/', bucketIndex + bucketPrefix.length());
            if (firstSlashAfterBucket >= 0 && firstSlashAfterBucket < s3KeyOrUrl.length() - 1) {
                return s3KeyOrUrl.substring(firstSlashAfterBucket + 1);
            }
        }

        // fallback: 마지막 슬래시 이후 반환
        return extractFilename(s3KeyOrUrl);
    }
}
