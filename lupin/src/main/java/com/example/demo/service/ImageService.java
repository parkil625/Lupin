package com.example.demo.service;

import io.awspring.cloud.s3.S3Template;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImageService {

    private final S3Template s3Template;
    private final S3Client s3Client;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucket;

    public String uploadImage(MultipartFile file) throws IOException {
        return uploadImage(file, null);
    }

    public String uploadImage(MultipartFile file, String prefix) throws IOException {
        String originalFileName = file.getOriginalFilename();
        // 확장자 추출
        String extension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }

        // 고유한 파일명 생성 (UUID)
        String fileName = UUID.randomUUID().toString() + extension;

        // prefix가 있으면 폴더 경로 추가
        if (prefix != null && !prefix.isEmpty()) {
            fileName = prefix + "/" + fileName;
        }

        // S3에 업로드 (Cache-Control 헤더 포함)
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(fileName)
                .contentType(file.getContentType())
                .cacheControl("max-age=31536000, immutable") // 1년 캐싱
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

        // URL 생성
        return String.format("https://%s.s3.ap-northeast-2.amazonaws.com/%s", bucket, fileName);
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
        String s3Key = extractS3Key(imageIdOrUrl);
        s3Template.deleteObject(bucket, s3Key);
    }

    /**
     * URL 또는 S3 키에서 S3 키만 추출
     * 예: https://lupin-storage.s3.ap-northeast-2.amazonaws.com/feed/abc123.jpg
     *     → feed/abc123.jpg
     */
    private String extractS3Key(String s3KeyOrUrl) {
        if (s3KeyOrUrl == null || s3KeyOrUrl.isEmpty()) {
            return s3KeyOrUrl;
        }

        // 이미 S3 키인 경우 (http로 시작하지 않음)
        if (!s3KeyOrUrl.startsWith("http")) {
            return s3KeyOrUrl;
        }

        // S3 URL에서 버킷 이름 이후의 전체 경로 추출
        // 예: https://lupin-storage.s3.ap-northeast-2.amazonaws.com/feed/abc123.jpg
        //     → feed/abc123.jpg
        String bucketPrefix = bucket + ".s3";
        int bucketIndex = s3KeyOrUrl.indexOf(bucketPrefix);

        if (bucketIndex >= 0) {
            // 버킷 이름 이후 첫 번째 슬래시 찾기
            int firstSlashAfterBucket = s3KeyOrUrl.indexOf('/', bucketIndex + bucketPrefix.length());
            if (firstSlashAfterBucket >= 0 && firstSlashAfterBucket < s3KeyOrUrl.length() - 1) {
                return s3KeyOrUrl.substring(firstSlashAfterBucket + 1);
            }
        }

        // 실패 시 기존 로직 (마지막 슬래시 이후)
        int lastSlashIndex = s3KeyOrUrl.lastIndexOf('/');
        if (lastSlashIndex >= 0 && lastSlashIndex < s3KeyOrUrl.length() - 1) {
            return s3KeyOrUrl.substring(lastSlashIndex + 1);
        }

        return s3KeyOrUrl;
    }
}