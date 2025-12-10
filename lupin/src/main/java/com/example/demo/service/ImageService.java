package com.example.demo.service;

import com.sksamuel.scrimage.ImmutableImage;
import com.sksamuel.scrimage.webp.WebpWriter;
import io.awspring.cloud.s3.S3Template;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageService {

    private static final int MAX_WIDTH = 800;
    private static final int MAX_HEIGHT = 800;
    private static final int WEBP_QUALITY = 60;

    // 썸네일 설정
    private static final int THUMB_WIDTH = 300;
    private static final int THUMB_HEIGHT = 400;
    private static final int THUMB_QUALITY = 50;

    private final S3Template s3Template;
    private final S3Client s3Client;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucket;

    public String uploadImage(MultipartFile file) throws IOException {
        return uploadImage(file, null);
    }

    public String uploadImage(MultipartFile file, String prefix) throws IOException {
        // WebP 변환 시도
        byte[] imageBytes;
        byte[] thumbBytes = null;
        String extension;
        String contentType;

        try {
            ImmutableImage image = ImmutableImage.loader().fromStream(file.getInputStream());

            // 원본 리사이징 + WebP 변환
            if (image.width > MAX_WIDTH || image.height > MAX_HEIGHT) {
                image = image.bound(MAX_WIDTH, MAX_HEIGHT);
            }
            imageBytes = image.bytes(WebpWriter.DEFAULT.withQ(WEBP_QUALITY));

            // 썸네일 생성 (feed 폴더인 경우만)
            if ("feed".equals(prefix)) {
                ImmutableImage thumbImage = ImmutableImage.loader().fromStream(
                    new ByteArrayInputStream(file.getBytes())
                );
                thumbImage = thumbImage.cover(THUMB_WIDTH, THUMB_HEIGHT);
                thumbBytes = thumbImage.bytes(WebpWriter.DEFAULT.withQ(THUMB_QUALITY));
                log.info("Thumbnail created: {}x{}, {} bytes", THUMB_WIDTH, THUMB_HEIGHT, thumbBytes.length);
            }

            extension = ".webp";
            contentType = "image/webp";
            log.info("Image converted to WebP: {} -> {} bytes", file.getOriginalFilename(), imageBytes.length);
        } catch (Exception e) {
            // WebP 변환 실패 시 원본 사용 (fallback)
            log.warn("WebP conversion failed, using original: {}", e.getMessage());
            imageBytes = file.getBytes();
            extension = getExtension(file.getOriginalFilename());
            contentType = file.getContentType();
        }

        // 고유한 파일명 생성 (UUID)
        String uuid = UUID.randomUUID().toString();
        String fileName = uuid + extension;

        // prefix가 있으면 폴더 경로 추가
        if (prefix != null && !prefix.isEmpty()) {
            fileName = prefix + "/" + fileName;
        }

        // S3에 원본 업로드 (Cache-Control 헤더 포함)
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(fileName)
                .contentType(contentType)
                .cacheControl("max-age=31536000, immutable") // 1년 캐싱
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(imageBytes));

        // 썸네일 업로드 (feed 폴더인 경우)
        if (thumbBytes != null && "feed".equals(prefix)) {
            String thumbFileName = prefix + "/thumb/" + uuid + extension;
            PutObjectRequest thumbRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(thumbFileName)
                    .contentType(contentType)
                    .cacheControl("max-age=31536000, immutable")
                    .build();
            s3Client.putObject(thumbRequest, RequestBody.fromBytes(thumbBytes));
            log.info("Thumbnail uploaded: {}", thumbFileName);
        }

        // URL 생성
        return String.format("https://%s.s3.ap-northeast-2.amazonaws.com/%s", bucket, fileName);
    }

    private String getExtension(String filename) {
        if (filename != null && filename.contains(".")) {
            return filename.substring(filename.lastIndexOf("."));
        }
        return ".jpg";
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