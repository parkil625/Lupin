package com.example.demo.component;

import com.sksamuel.scrimage.ImmutableImage;
import com.sksamuel.scrimage.webp.WebpWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * 이미지 처리 전용 컴포넌트
 * - 리사이징, WebP 변환 등 순수 이미지 가공 로직만 담당
 * - 저장소(S3 등)에 대한 의존성 없음 -> 단위 테스트 용이
 */
@Slf4j
@Component
public class ImageProcessor {

    private static final int DEFAULT_WEBP_QUALITY = 60;

    /**
     * 이미지를 리사이징하고 WebP로 변환
     *
     * @param originalBytes 원본 이미지 바이트
     * @param maxWidth 최대 너비
     * @param maxHeight 최대 높이
     * @param quality WebP 품질 (1-100)
     * @return WebP 변환된 바이트 배열
     */
    public byte[] convertToWebp(byte[] originalBytes, int maxWidth, int maxHeight, int quality) throws IOException {
        try {
            ImmutableImage image = ImmutableImage.loader().fromStream(
                    new ByteArrayInputStream(originalBytes)
            );

            // 리사이징 필요 시 수행 (비율 유지)
            if (image.width > maxWidth || image.height > maxHeight) {
                image = image.bound(maxWidth, maxHeight);
            }

            byte[] result = image.bytes(WebpWriter.DEFAULT.withQ(quality));
            log.debug("Image converted to WebP: {}x{} -> {}x{}, {} bytes",
                    image.width, image.height, maxWidth, maxHeight, result.length);
            return result;

        } catch (Exception e) {
            log.warn("WebP conversion failed, returning original bytes. Error: {}", e.getMessage());
            return originalBytes;
        }
    }

    /**
     * 기본 품질로 WebP 변환
     */
    public byte[] convertToWebp(byte[] originalBytes, int maxWidth, int maxHeight) throws IOException {
        return convertToWebp(originalBytes, maxWidth, maxHeight, DEFAULT_WEBP_QUALITY);
    }

    /**
     * 정사각형 썸네일 생성 (중앙 크롭)
     */
    public byte[] createSquareThumbnail(byte[] originalBytes, int size, int quality) throws IOException {
        try {
            ImmutableImage image = ImmutableImage.loader().fromStream(
                    new ByteArrayInputStream(originalBytes)
            );

            // 중앙 크롭 후 리사이징
            image = image.cover(size, size);
            byte[] result = image.bytes(WebpWriter.DEFAULT.withQ(quality));

            log.debug("Square thumbnail created: {}x{}, {} bytes", size, size, result.length);
            return result;

        } catch (Exception e) {
            log.warn("Square thumbnail creation failed. Error: {}", e.getMessage());
            throw new IOException("Thumbnail creation failed", e);
        }
    }

    /**
     * 피드 썸네일 생성 (3:4 비율 크롭)
     */
    public byte[] createFeedThumbnail(byte[] originalBytes, int width, int height, int quality) throws IOException {
        try {
            ImmutableImage image = ImmutableImage.loader().fromStream(
                    new ByteArrayInputStream(originalBytes)
            );

            // 지정된 비율로 크롭 후 리사이징
            image = image.cover(width, height);
            byte[] result = image.bytes(WebpWriter.DEFAULT.withQ(quality));

            log.debug("Feed thumbnail created: {}x{}, {} bytes", width, height, result.length);
            return result;

        } catch (Exception e) {
            log.warn("Feed thumbnail creation failed. Error: {}", e.getMessage());
            throw new IOException("Thumbnail creation failed", e);
        }
    }
}
