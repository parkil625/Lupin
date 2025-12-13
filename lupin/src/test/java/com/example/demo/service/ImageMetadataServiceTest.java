package com.example.demo.service;

import com.example.demo.util.S3UrlUtils;
import io.awspring.cloud.s3.S3Resource;
import io.awspring.cloud.s3.S3Template;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("ImageMetadataService 테스트")
class ImageMetadataServiceTest {

    @Mock
    private S3Template s3Template;

    @Mock
    private S3Resource s3Resource;

    @InjectMocks
    private ImageMetadataService imageMetadataService;

    private static final String BUCKET = "test-bucket";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(imageMetadataService, "bucket", BUCKET);
    }

    @Nested
    @DisplayName("S3UrlUtils.extractFilename")
    class ExtractFilename {

        @Test
        @DisplayName("URL에서 파일명을 추출한다")
        void extractFilenameFromUrl() {
            // given
            String url = "https://test-bucket.s3.ap-northeast-2.amazonaws.com/test-image.jpg";

            // when
            String result = S3UrlUtils.extractFilename(url);

            // then
            assertThat(result).isEqualTo("test-image.jpg");
        }

        @Test
        @DisplayName("이미 파일명인 경우 그대로 반환한다")
        void extractFilenameFromKey() {
            // given
            String key = "test-image.jpg";

            // when
            String result = S3UrlUtils.extractFilename(key);

            // then
            assertThat(result).isEqualTo("test-image.jpg");
        }

        @Test
        @DisplayName("null인 경우 null을 반환한다")
        void extractFilenameFromNull() {
            // given
            String key = null;

            // when
            String result = S3UrlUtils.extractFilename(key);

            // then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("빈 문자열인 경우 빈 문자열을 반환한다")
        void extractFilenameFromEmpty() {
            // given
            String key = "";

            // when
            String result = S3UrlUtils.extractFilename(key);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("경로가 포함된 URL에서 파일명만 추출한다")
        void extractFilenameFromUrlWithPath() {
            // given
            String url = "https://test-bucket.s3.amazonaws.com/images/2024/01/test-image.jpg";

            // when
            String result = S3UrlUtils.extractFilename(url);

            // then
            assertThat(result).isEqualTo("test-image.jpg");
        }
    }

    @Nested
    @DisplayName("extractPhotoDateTime")
    class ExtractPhotoDateTime {

        @Test
        @DisplayName("EXIF가 없는 이미지에서는 빈 Optional을 반환한다")
        void extractPhotoDateTimeFromImageWithoutExif() throws Exception {
            // given
            String s3Key = "test-image.jpg";
            byte[] emptyImageBytes = new byte[0];
            InputStream inputStream = new ByteArrayInputStream(emptyImageBytes);

            given(s3Template.download(eq(BUCKET), eq(s3Key))).willReturn(s3Resource);
            given(s3Resource.getInputStream()).willReturn(inputStream);

            // when
            Optional<LocalDateTime> result = imageMetadataService.extractPhotoDateTime(s3Key);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("S3 다운로드 실패 시 빈 Optional을 반환한다")
        void extractPhotoDateTimeWhenS3DownloadFails() {
            // given
            String s3Key = "test-image.jpg";
            given(s3Template.download(eq(BUCKET), eq(s3Key))).willThrow(new RuntimeException("S3 error"));

            // when
            Optional<LocalDateTime> result = imageMetadataService.extractPhotoDateTime(s3Key);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("URL을 전달하면 S3 키를 추출하여 처리한다")
        void extractPhotoDateTimeFromUrl() throws Exception {
            // given
            String url = "https://test-bucket.s3.amazonaws.com/test-image.jpg";
            byte[] emptyImageBytes = new byte[0];
            InputStream inputStream = new ByteArrayInputStream(emptyImageBytes);

            given(s3Template.download(eq(BUCKET), eq("test-image.jpg"))).willReturn(s3Resource);
            given(s3Resource.getInputStream()).willReturn(inputStream);

            // when
            Optional<LocalDateTime> result = imageMetadataService.extractPhotoDateTime(url);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("InputStream 읽기 실패 시 빈 Optional을 반환한다")
        void extractPhotoDateTimeWhenInputStreamFails() throws Exception {
            // given
            String s3Key = "test-image.jpg";
            given(s3Template.download(eq(BUCKET), eq(s3Key))).willReturn(s3Resource);
            given(s3Resource.getInputStream()).willThrow(new RuntimeException("IO error"));

            // when
            Optional<LocalDateTime> result = imageMetadataService.extractPhotoDateTime(s3Key);

            // then
            assertThat(result).isEmpty();
        }
    }
}
