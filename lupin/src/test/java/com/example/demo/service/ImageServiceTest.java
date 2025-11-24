package com.example.demo.service;

import com.example.demo.exception.BusinessException;
import io.awspring.cloud.s3.S3Template;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ImageService 테스트")
class ImageServiceTest {

    @InjectMocks
    private ImageService imageService;

    @Mock
    private S3Template s3Template;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(imageService, "bucket", "test-bucket");
        ReflectionTestUtils.setField(imageService, "region", "ap-northeast-2");
        ReflectionTestUtils.setField(imageService, "accessKey", "");
        ReflectionTestUtils.setField(imageService, "secretKey", "");
    }

    @Nested
    @DisplayName("이미지 업로드")
    class UploadImage {

        @Test
        @DisplayName("S3 미설정시 더미 URL 반환")
        void uploadImage_LocalMode_ReturnsDummyUrl() {
            // given
            MockMultipartFile file = new MockMultipartFile(
                    "file", "test.jpg", "image/jpeg", "test".getBytes()
            );

            // when
            String result = imageService.uploadImage(file, "feed");

            // then
            assertThat(result).contains("unsplash.com");
        }

        @Test
        @DisplayName("빈 파일 업로드 실패")
        void uploadImage_EmptyFile_ThrowsException() {
            // given
            MockMultipartFile file = new MockMultipartFile(
                    "file", "test.jpg", "image/jpeg", new byte[0]
            );

            // when & then
            assertThatThrownBy(() -> imageService.uploadImage(file, "feed"))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("잘못된 파일 타입 업로드 실패")
        void uploadImage_InvalidType_ThrowsException() {
            // given
            MockMultipartFile file = new MockMultipartFile(
                    "file", "test.txt", "text/plain", "test".getBytes()
            );

            // when & then
            assertThatThrownBy(() -> imageService.uploadImage(file, "feed"))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("파일 크기 초과 업로드 실패")
        void uploadImage_SizeExceeded_ThrowsException() {
            // given
            byte[] largeContent = new byte[11 * 1024 * 1024]; // 11MB
            MockMultipartFile file = new MockMultipartFile(
                    "file", "test.jpg", "image/jpeg", largeContent
            );

            // when & then
            assertThatThrownBy(() -> imageService.uploadImage(file, "feed"))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("다중 이미지 업로드")
    class UploadMultipleImages {

        @Test
        @DisplayName("다중 이미지 업로드 성공")
        void uploadMultipleImages_Success() {
            // given
            MockMultipartFile file1 = new MockMultipartFile(
                    "file", "test1.jpg", "image/jpeg", "test1".getBytes()
            );
            MockMultipartFile file2 = new MockMultipartFile(
                    "file", "test2.jpg", "image/jpeg", "test2".getBytes()
            );

            // when
            var result = imageService.uploadMultipleImages(new MockMultipartFile[]{file1, file2});

            // then
            assertThat(result).hasSize(2);
        }
    }

    @Nested
    @DisplayName("이미지 삭제")
    class DeleteImage {

        @Test
        @DisplayName("null URL 삭제시 무시")
        void deleteImage_NullUrl_NoException() {
            // when & then
            assertThatCode(() -> imageService.deleteImage(null))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("빈 URL 삭제시 무시")
        void deleteImage_EmptyUrl_NoException() {
            // when & then
            assertThatCode(() -> imageService.deleteImage(""))
                    .doesNotThrowAnyException();
        }
    }
}
