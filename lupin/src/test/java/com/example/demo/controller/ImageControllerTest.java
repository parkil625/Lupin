package com.example.demo.controller;

import io.awspring.cloud.s3.S3Resource;
import io.awspring.cloud.s3.S3Template;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.io.InputStream;
import java.net.URL;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser
@DisplayName("ImageController 테스트")
class ImageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private S3Template s3Template;

    @Test
    @DisplayName("이미지 업로드 성공")
    void upload_Success() throws Exception {
        // given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        S3Resource mockResource = mock(S3Resource.class);
        given(mockResource.getURL()).willReturn(new URL("https://bucket.s3.amazonaws.com/feed/uuid_test.jpg"));
        given(s3Template.upload(anyString(), anyString(), any(InputStream.class))).willReturn(mockResource);

        // when & then
        mockMvc.perform(multipart("/api/images/upload")
                        .file(file)
                        .param("type", "feed"))
                .andExpect(status().isOk())
                .andExpect(content().string("https://bucket.s3.amazonaws.com/feed/uuid_test.jpg"));
    }

    @Test
    @DisplayName("프로필 이미지 업로드 성공")
    void uploadProfile_Success() throws Exception {
        // given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "profile.jpg",
                "image/jpeg",
                "profile image content".getBytes()
        );

        S3Resource mockResource = mock(S3Resource.class);
        given(mockResource.getURL()).willReturn(new URL("https://bucket.s3.amazonaws.com/profile/uuid_profile.jpg"));
        given(s3Template.upload(anyString(), anyString(), any(InputStream.class))).willReturn(mockResource);

        // when & then
        mockMvc.perform(multipart("/api/images/upload")
                        .file(file)
                        .param("type", "profile"))
                .andExpect(status().isOk())
                .andExpect(content().string("https://bucket.s3.amazonaws.com/profile/uuid_profile.jpg"));
    }

    @Test
    @DisplayName("다중 이미지 업로드 성공")
    void uploadMultiple_Success() throws Exception {
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

        S3Resource mockResource1 = mock(S3Resource.class);
        S3Resource mockResource2 = mock(S3Resource.class);
        given(mockResource1.getURL()).willReturn(new URL("https://bucket.s3.amazonaws.com/feed/uuid_test1.jpg"));
        given(mockResource2.getURL()).willReturn(new URL("https://bucket.s3.amazonaws.com/feed/uuid_test2.jpg"));
        given(s3Template.upload(anyString(), anyString(), any(InputStream.class)))
                .willReturn(mockResource1)
                .willReturn(mockResource2);

        // when & then
        mockMvc.perform(multipart("/api/images/upload/multiple")
                        .file(file1)
                        .file(file2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("이미지 삭제 성공")
    void delete_Success() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/images/delete")
                        .param("url", "https://bucket.s3.amazonaws.com/feed/uuid_test.jpg"))
                .andExpect(status().isOk());

        then(s3Template).should().deleteObject(anyString(), eq("feed/uuid_test.jpg"));
    }
}
