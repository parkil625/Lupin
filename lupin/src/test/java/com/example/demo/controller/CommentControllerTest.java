package com.example.demo.controller;

import com.example.demo.config.SecurityConfig;
import com.example.demo.config.TestRedisConfig;
import com.example.demo.config.TestSecurityConfig;
import com.example.demo.domain.entity.Comment;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.Role;
import com.example.demo.dto.request.CommentRequest;
import com.example.demo.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CommentController.class,
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {SecurityConfig.class})
        })
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Import({TestSecurityConfig.class, TestRedisConfig.class}) // ★★★ 친구들 다 모여!
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CommentService commentService;

    @MockitoBean
    private CommentReadService commentReadService;

    @MockitoBean
    private CommentLikeService commentLikeService;

    @MockitoBean
    private ReportService reportService;

    @MockitoBean
    private CommentDeleteFacade commentDeleteFacade;

    private User user;
    private Comment comment;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .userId("test@test.com")
                .name("testUser")
                .role(Role.MEMBER)
                .build();

        comment = Comment.builder()
                .id(1L)
                .writer(user)
                .content("테스트 댓글")
                .build();
    }

    @Test
    @WithMockUser
    @DisplayName("댓글 생성 성공")
    void createCommentTest() throws Exception {
        CommentRequest request = new CommentRequest("테스트 댓글", null);
        given(commentService.create(any(User.class), eq(1L), any(), eq("테스트 댓글")))
                .willReturn(comment);

        mockMvc.perform(post("/api/feeds/1/comments")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("답글 생성 성공")
    void createReplyTest() throws Exception {
        CommentRequest request = new CommentRequest("테스트 답글", 1L);
        given(commentService.create(any(User.class), eq(1L), any(), eq("테스트 답글")))
                .willReturn(comment);

        mockMvc.perform(post("/api/feeds/1/comments")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("댓글 수정 성공")
    void updateCommentTest() throws Exception {
        CommentRequest request = new CommentRequest("수정된 댓글", null);
        given(commentService.updateComment(any(User.class), eq(1L), eq("수정된 댓글")))
                .willReturn(comment);

        mockMvc.perform(put("/api/comments/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("댓글 삭제 성공")
    void deleteCommentTest() throws Exception {
        mockMvc.perform(delete("/api/comments/1")
                        .with(csrf()))
                .andExpect(status().isOk());

        verify(commentDeleteFacade).deleteComment(any(User.class), eq(1L));
    }
}