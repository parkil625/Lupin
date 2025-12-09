package com.example.demo.service;

import io.awspring.cloud.s3.S3Resource;
import io.awspring.cloud.s3.S3Template;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ImageService 테스트")
class ImageServiceTest {

    @Mock
    private S3Template s3Template;

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Resource s3Resource;

    @InjectMocks
    private ImageService imageService;

    private static final String BUCKET = "test-bucket";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(imageService, "bucket", BUCKET);
    }

    @Test
    @DisplayName("이미지를 업로드한다")
    void uploadImageTest() throws Exception {
        // given
        MockMultipartFile file = new MockMultipartFile(
                "image",
                "test.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );
        // S3Client putObject mock 설정
        given(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .willReturn(PutObjectResponse.builder().build());

        // when
        String result = imageService.uploadImage(file);

        // then
        assertThat(result).startsWith("https://" + BUCKET + ".s3.ap-northeast-2.amazonaws.com/");
        assertThat(result).endsWith(".jpg");
        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("여러 이미지를 업로드한다")
    void uploadImagesTest() throws Exception {
        // given
        MockMultipartFile file1 = new MockMultipartFile(
                "files",
                "test1.jpg",
                "image/jpeg",
                "test image 1".getBytes()
        );
        MockMultipartFile file2 = new MockMultipartFile(
                "files",
                "test2.jpg",
                "image/jpeg",
                "test image 2".getBytes()
        );
        // S3Client putObject mock 설정
        given(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .willReturn(PutObjectResponse.builder().build());

        // when
        List<String> results = imageService.uploadImages(List.of(file1, file2));

        // then
        assertThat(results).hasSize(2);
        assertThat(results.get(0)).startsWith("https://" + BUCKET + ".s3.ap-northeast-2.amazonaws.com/");
        assertThat(results.get(1)).startsWith("https://" + BUCKET + ".s3.ap-northeast-2.amazonaws.com/");
    }

    @Test
    @DisplayName("이미지를 삭제한다")
    void deleteImageTest() {
        // given
        String imageId = "test-image-id.jpg";

        // when
        imageService.deleteImage(imageId);

        // then
        verify(s3Template).deleteObject(BUCKET, imageId);
    }
}
