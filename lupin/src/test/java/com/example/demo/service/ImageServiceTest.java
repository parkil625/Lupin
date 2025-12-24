package com.example.demo.service;

import com.example.demo.component.ImageProcessor;
import com.example.demo.config.ImagePolicyProperties;
import com.example.demo.infrastructure.FileStorage;
import org.springframework.context.ApplicationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ImageService 테스트")
class ImageServiceTest {

    @Mock
    private FileStorage fileStorage;

    @Mock
    private ImageProcessor imageProcessor;

    @Mock
    private ImageMetadataService imageMetadataService;

    @Mock
    private ApplicationContext applicationContext;

    private ImagePolicyProperties imagePolicy;

    private ImageService imageService;

    @BeforeEach
    void setUp() {
        imagePolicy = new ImagePolicyProperties(
                new ImagePolicyProperties.OriginalPolicy(800, 800, 60),
                new ImagePolicyProperties.ThumbnailPolicy(300, 400, 50),
                new ImagePolicyProperties.ThumbnailPolicy(100, 100, 60)
        );
        imageService = new ImageService(fileStorage, imageProcessor, imageMetadataService, imagePolicy, applicationContext);

        // 비동기 메서드 호출 시 자기 자신을 참조(Proxy)하기 위해 getBean을 호출하는 경우를 대비
        lenient().when(applicationContext.getBean(ImageService.class)).thenReturn(imageService);
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

        byte[] processedBytes = "converted webp".getBytes();
        given(imageProcessor.convertToWebp(any(byte[].class), anyInt(), anyInt(), anyInt()))
                .willReturn(processedBytes);
        given(fileStorage.getPublicUrl(anyString()))
                .willAnswer(invocation -> "https://cdn.lupin-care.com/" + invocation.getArgument(0));

        // when
        String result = imageService.uploadImage(file);

        // then
        assertThat(result).startsWith("https://cdn.lupin-care.com/");
        assertThat(result).endsWith(".webp");
        verify(imageProcessor).convertToWebp(any(byte[].class), eq(800), eq(800), eq(60));
        verify(fileStorage).upload(anyString(), eq(processedBytes), eq("image/webp"));
    }

    @Test
    @DisplayName("피드 이미지 업로드 시 EXIF 추출 및 썸네일 생성")
    void uploadFeedImageTest() throws Exception {
        // given
        MockMultipartFile file = new MockMultipartFile(
                "image",
                "test.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        byte[] processedBytes = "converted webp".getBytes();
        given(imageProcessor.convertToWebp(any(byte[].class), anyInt(), anyInt(), anyInt()))
                .willReturn(processedBytes);
        given(fileStorage.getPublicUrl(anyString()))
                .willAnswer(invocation -> "https://cdn.lupin-care.com/" + invocation.getArgument(0));

        // when
        String result = imageService.uploadImage(file, "feed");

        // then
        assertThat(result).startsWith("https://cdn.lupin-care.com/feed/");
        verify(imageMetadataService).extractAndCache(any(byte[].class), anyString());
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

        byte[] processedBytes = "converted webp".getBytes();
        given(imageProcessor.convertToWebp(any(byte[].class), anyInt(), anyInt(), anyInt()))
                .willReturn(processedBytes);
        given(fileStorage.getPublicUrl(anyString()))
                .willAnswer(invocation -> "https://cdn.lupin-care.com/" + invocation.getArgument(0));

        // when
        List<String> results = imageService.uploadImages(List.of(file1, file2));

        // then
        assertThat(results).hasSize(2);
        assertThat(results.get(0)).startsWith("https://cdn.lupin-care.com/");
        assertThat(results.get(1)).startsWith("https://cdn.lupin-care.com/");
    }

    @Test
    @DisplayName("이미지를 삭제한다")
    void deleteImageTest() {
        // given
        String imageUrl = "https://cdn.lupin-care.com/feed/test-image.webp";

        // when
        imageService.deleteImage(imageUrl);

        // then
        verify(fileStorage).delete(imageUrl);
    }
}
