package com.example.demo.controller;

import com.example.demo.dto.request.CommentCreateRequest;
import com.example.demo.dto.response.CommentResponse;
import com.example.demo.service.CommentCommandService;
import com.example.demo.service.CommentQueryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.example.demo.config.TestRedisConfig;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Import(TestRedisConfig.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser
@DisplayName("CommentController 테스트")
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CommentCommandService commentCommandService;

    @MockBean
    private CommentQueryService commentQueryService;

    @Test
    @DisplayName("댓글 생성 성공")
    void createComment_Success() throws Exception {
        // given
        CommentCreateRequest request = CommentCreateRequest.builder()
                .content("테스트 댓글")
                .build();

        given(commentCommandService.createComment(eq(1L), eq(1L), any(CommentCreateRequest.class)))
                .willReturn(10L);

        // when & then
        mockMvc.perform(post("/api/comments/feeds/1")
                        .param("userId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.commentId").value(10));
    }

    @Test
    @DisplayName("댓글 상세 조회 성공")
    void getCommentDetail_Success() throws Exception {
        // given
        CommentResponse response = CommentResponse.builder()
                .id(1L)
                .content("테스트 댓글")
                .build();

        given(commentQueryService.getCommentDetail(1L)).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/comments/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("테스트 댓글"));
    }

    @Test
    @DisplayName("댓글 수 조회 성공")
    void getCommentCount_Success() throws Exception {
        // given
        given(commentQueryService.getCommentCountByFeedId(1L)).willReturn(5L);

        // when & then
        mockMvc.perform(get("/api/comments/feeds/1/count"))
                .andExpect(status().isOk())
                .andExpect(content().string("5"));
    }

    @Test
    @DisplayName("답글 조회 성공")
    void getReplies_Success() throws Exception {
        // given
        CommentResponse reply = CommentResponse.builder()
                .id(2L)
                .content("답글")
                .build();

        List<CommentResponse> replies = Arrays.asList(reply);

        given(commentQueryService.getRepliesByCommentId(1L)).willReturn(replies);

        // when & then
        mockMvc.perform(get("/api/comments/1/replies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].content").value("답글"));
    }

    @Test
    @DisplayName("댓글 좋아요 성공")
    void likeComment_Success() throws Exception {
        // when & then
        mockMvc.perform(post("/api/comments/1/like")
                        .param("userId", "1"))
                .andExpect(status().isOk());

        then(commentCommandService).should().likeComment(1L, 1L);
    }

    @Test
    @DisplayName("댓글 삭제 성공")
    void deleteComment_Success() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/comments/1")
                        .param("userId", "1"))
                .andExpect(status().isNoContent());

        then(commentCommandService).should().deleteComment(1L, 1L);
    }
}
