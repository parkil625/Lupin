package com.example.demo.infrastructure;

/**
 * 파일 저장소 추상화 인터페이스
 * - S3, 로컬, GCP 등 다양한 저장소로 교체 가능
 * - ImageService가 특정 저장소에 의존하지 않도록 함
 */
public interface FileStorage {

    /**
     * 파일 업로드
     *
     * @param path 저장 경로 (키)
     * @param content 파일 내용
     * @param contentType MIME 타입
     */
    void upload(String path, byte[] content, String contentType);

    /**
     * 파일 삭제
     *
     * @param path 저장 경로 (키) 또는 전체 URL
     */
    void delete(String path);

    /**
     * 공개 URL 반환
     *
     * @param path 저장 경로 (키)
     * @return CDN 또는 공개 URL
     */
    String getPublicUrl(String path);
}
