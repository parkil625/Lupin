package com.example.demo.controller;

import io.awspring.cloud.s3.S3Template;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 이미지 업로드 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageController {

    private final S3Template s3Template;

    @Value("${spring.cloud.aws.s3.bucket:lupin-images}")
    private String bucketName;

    /**
     * 이미지 업로드
     * POST /api/images/upload
     * @param type - "feed" 또는 "profile" (기본값: feed)
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "type", defaultValue = "feed") String type) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String uuid = UUID.randomUUID().toString();
        String folder = "profile".equals(type) ? "profile" : "feed";
        String key = folder + "/" + uuid + "_" + originalFilename;

        var resource = s3Template.upload(bucketName, key, file.getInputStream());

        String url = resource.getURL().toString();
        log.info("이미지 업로드 완료 ({}): {}", folder, url);

        return ResponseEntity.ok(url);
    }

    /**
     * 다중 이미지 업로드
     * POST /api/images/upload/multiple
     */
    @PostMapping(value = "/upload/multiple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<String>> uploadMultiple(@RequestParam("files") MultipartFile[] files) throws IOException {
        List<String> urls = new ArrayList<>();

        for (MultipartFile file : files) {
            String originalFilename = file.getOriginalFilename();
            String uuid = UUID.randomUUID().toString();
            String key = "feed/" + uuid + "_" + originalFilename;

            var resource = s3Template.upload(bucketName, key, file.getInputStream());
            urls.add(resource.getURL().toString());
        }

        log.info("다중 이미지 업로드 완료: {} 개", urls.size());
        return ResponseEntity.ok(urls);
    }

    /**
     * 이미지 삭제
     * DELETE /api/images/delete
     * @param url - 삭제할 S3 URL
     */
    @DeleteMapping("/delete")
    public ResponseEntity<Void> delete(@RequestParam("url") String url) {
        try {
            // URL에서 key 추출 (예: https://bucket.s3.region.amazonaws.com/feed/uuid_filename.jpg)
            String key = extractKeyFromUrl(url);
            if (key != null) {
                s3Template.deleteObject(bucketName, key);
                log.info("이미지 삭제 완료: {}", key);
            }
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("이미지 삭제 실패: {}", url, e);
            return ResponseEntity.ok().build(); // 삭제 실패해도 에러 안 던짐
        }
    }

    /**
     * S3 URL에서 key 추출
     */
    private String extractKeyFromUrl(String url) {
        if (url == null || url.isEmpty()) return null;
        try {
            // URL 형식: https://bucket.s3.region.amazonaws.com/key
            URL s3Url = new URL(url);
            String path = s3Url.getPath();
            // 앞의 '/' 제거
            return path.startsWith("/") ? path.substring(1) : path;
        } catch (Exception e) {
            log.error("URL 파싱 실패: {}", url);
            return null;
        }
    }
}
