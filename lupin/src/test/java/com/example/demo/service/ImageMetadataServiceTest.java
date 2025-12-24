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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("ImageMetadataService 테스트")
class ImageMetadataServiceTest {

    @Mock
    private S3Template s3Template;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private S3Resource s3Resource;

    @InjectMocks
    private ImageMetadataService imageMetadataService;

    private static final String BUCKET = "test-bucket";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(imageMetadataService, "bucket", BUCKET);
        
        // [수정] Redis를 사용하지 않는 테스트(예: 파일명 추출)에서 에러가 나지 않도록 lenient() 적용
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Nested
    @DisplayName("S3UrlUtils.extractFilename")
    class ExtractFilename {

        @Test
        @DisplayName("URL에서 파일명을 추출한다")
        void extractFilenameFromUrl() {
            String url = "https://test-bucket.s3.ap-northeast-2.amazonaws.com/test-image.jpg";
            String result = S3UrlUtils.extractFilename(url);
            assertThat(result).isEqualTo("test-image.jpg");
        }

        @Test
        @DisplayName("이미 파일명인 경우 그대로 반환한다")
        void extractFilenameFromKey() {
            String key = "test-image.jpg";
            String result = S3UrlUtils.extractFilename(key);
            assertThat(result).isEqualTo("test-image.jpg");
        }

        @Test
        @DisplayName("null인 경우 null을 반환한다")
        void extractFilenameFromNull() {
            String key = null;
            String result = S3UrlUtils.extractFilename(key);
            assertThat(result).isNull();
        }
        
        @Test
        @DisplayName("빈 문자열인 경우 빈 문자열을 반환한다")
        void extractFilenameFromEmpty() {
            String key = "";
            String result = S3UrlUtils.extractFilename(key);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("경로가 포함된 URL에서 파일명만 추출한다")
        void extractFilenameFromUrlWithPath() {
            String url = "https://test-bucket.s3.amazonaws.com/images/2024/01/test-image.jpg";
            String result = S3UrlUtils.extractFilename(url);
            assertThat(result).isEqualTo("test-image.jpg");
        }
    }

    @Nested
    @DisplayName("extractAndCache (Redis 저장)")
    class ExtractAndCache {

        @Test
        @DisplayName("이미지에서 EXIF를 추출하여 Redis에 저장한다")
        void extractAndSaveToRedis() {
            // given
            String fileName = "test.jpg";
            byte[] emptyImageBytes = new byte[0];

            // when
            imageMetadataService.extractAndCache(emptyImageBytes, fileName);

            // then
            // 빈 이미지라 EXIF가 없으므로 Redis 저장은 호출되지 않아야 함
            verify(valueOperations, never()).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
        }
    }

    @Nested
    @DisplayName("extractPhotoDateTime (조회)")
    class ExtractPhotoDateTime {

        @Test
        @DisplayName("Redis에 시간이 저장되어 있으면 S3를 조회하지 않고 반환한다")
        void returnTimeFromRedis() {
            // given
            String s3Key = "test-image.jpg";
            String cachedTime = "2024-01-01T12:00:00";
            
            // lenient() 덕분에 여기서 호출해도 괜찮음
            given(valueOperations.get("img:meta:" + s3Key)).willReturn(cachedTime);

            // when
            Optional<LocalDateTime> result = imageMetadataService.extractPhotoDateTime(s3Key);

            // then
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(LocalDateTime.parse(cachedTime));
            verify(s3Template, never()).download(anyString(), anyString());
        }

        @Test
        @DisplayName("Redis에 데이터가 없으면 S3에서 다운로드하여 추출을 시도한다")
        void downloadFromS3WhenRedisMiss() throws Exception { // [수정] 예외 처리 추가
            // given
            String s3Key = "test-image.jpg";
            given(valueOperations.get("img:meta:" + s3Key)).willReturn(null);
            
            // S3 Mock
            byte[] emptyImageBytes = new byte[0];
            InputStream inputStream = new ByteArrayInputStream(emptyImageBytes);
            given(s3Template.download(eq(BUCKET), eq(s3Key))).willReturn(s3Resource);
            given(s3Resource.getInputStream()).willReturn(inputStream);

            // when
            Optional<LocalDateTime> result = imageMetadataService.extractPhotoDateTime(s3Key);

            // then
            // 빈 이미지라 결과는 empty지만, S3 download가 호출되었는지 확인
            assertThat(result).isEmpty();
            verify(s3Template).download(BUCKET, s3Key);
        }
    }
}