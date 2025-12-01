package com.example.demo.controller;

import com.example.demo.config.TestRedisConfiguration;
import com.example.demo.domain.entity.Comment;
import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.Role;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.CommentLikeService;
import com.example.demo.service.CommentReportService;
import com.example.demo.service.CommentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
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

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestRedisConfiguration.class)
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CommentService commentService;

    @MockBean
    private CommentLikeService commentLikeService;

    @MockBean
    private CommentReportService commentReportService;

    @MockBean
    private UserRepository userRepository;

    private User testUser;
    private Feed testFeed;
    private Comment testComment;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .userId("testuser")
                .name("테스트유저")
                .role(Role.MEMBER)
                .build();

        testFeed = Feed.builder()
                .id(1L)
                .writer(testUser)
                .content("테스트 피드")
                .build();

        testComment = Comment.builder()
                .id(1L)
                .writer(testUser)
                .feed(testFeed)
                .content("테스트 댓글")
                .build();

        given(userRepository.findByUserId("testuser")).willReturn(Optional.of(testUser));
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("POST /api/feeds/{feedId}/comments - 댓글 작성 성공")
    void createComment_Success() throws Exception {
        // given
        given(commentService.createComment(any(User.class), eq(1L), eq("테스트 댓글")))
                .willReturn(testComment);

        // when & then
        mockMvc.perform(post("/api/feeds/1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"테스트 댓글\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.content").value("테스트 댓글"));
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("PUT /api/comments/{commentId} - 댓글 수정 성공")
    void updateComment_Success() throws Exception {
        // given
        Comment updatedComment = Comment.builder()
                .id(1L)
                .writer(testUser)
                .feed(testFeed)
                .content("수정된 댓글")
                .build();
        given(commentService.updateComment(any(User.class), eq(1L), eq("수정된 댓글")))
                .willReturn(updatedComment);

        // when & then
        mockMvc.perform(put("/api/comments/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"수정된 댓글\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("수정된 댓글"));
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("DELETE /api/comments/{commentId} - 댓글 삭제 성공")
    void deleteComment_Success() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/comments/1"))
                .andExpect(status().isOk());

        verify(commentService).deleteComment(any(User.class), eq(1L));
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("GET /api/comments/{commentId} - 댓글 단건 조회 성공")
    void getComment_Success() throws Exception {
        // given
        given(commentService.getComment(1L)).willReturn(testComment);

        // when & then
        mockMvc.perform(get("/api/comments/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.content").value("테스트 댓글"))
                .andExpect(jsonPath("$.feedId").value(1L));
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("GET /api/feeds/{feedId}/comments - 댓글 목록 조회 성공")
    void getComments_Success() throws Exception {
        // given
        given(commentService.getCommentsByFeed(1L)).willReturn(List.of(testComment));

        // when & then
        mockMvc.perform(get("/api/feeds/1/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].content").value("테스트 댓글"));
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("POST /api/comments/{commentId}/replies - 대댓글 작성 성공")
    void createReply_Success() throws Exception {
        // given
        Comment reply = Comment.builder()
                .id(2L)
                .writer(testUser)
                .feed(testFeed)
                .parent(testComment)
                .content("대댓글 내용")
                .build();
        given(commentService.createReply(any(User.class), eq(1L), eq(1L), eq("대댓글 내용")))
                .willReturn(reply);

        // when & then
        mockMvc.perform(post("/api/comments/1/replies")
                        .param("feedId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"대댓글 내용\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2L))
                .andExpect(jsonPath("$.content").value("대댓글 내용"));
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("GET /api/comments/{commentId}/replies - 대댓글 목록 조회 성공")
    void getReplies_Success() throws Exception {
        // given
        Comment reply = Comment.builder()
                .id(2L)
                .writer(testUser)
                .feed(testFeed)
                .parent(testComment)
                .content("대댓글 내용")
                .build();
        given(commentService.getReplies(1L)).willReturn(List.of(reply));

        // when & then
        mockMvc.perform(get("/api/comments/1/replies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2L))
                .andExpect(jsonPath("$[0].content").value("대댓글 내용"));
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("POST /api/comments/{commentId}/like - 댓글 좋아요 성공")
    void likeComment_Success() throws Exception {
        // when & then
        mockMvc.perform(post("/api/comments/1/like"))
                .andExpect(status().isOk());

        verify(commentLikeService).likeComment(any(User.class), eq(1L));
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("DELETE /api/comments/{commentId}/like - 댓글 좋아요 취소 성공")
    void unlikeComment_Success() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/comments/1/like"))
                .andExpect(status().isOk());

        verify(commentLikeService).unlikeComment(any(User.class), eq(1L));
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("POST /api/comments/{commentId}/report - 댓글 신고 토글 성공")
    void reportComment_Success() throws Exception {
        // when & then
        mockMvc.perform(post("/api/comments/1/report"))
                .andExpect(status().isOk());

        verify(commentReportService).toggleReport(any(User.class), eq(1L));
    }
}
