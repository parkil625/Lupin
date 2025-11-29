package com.example.demo.security;

import com.example.demo.config.TestRedisConfiguration;
import com.example.demo.dto.request.FeedRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestRedisConfiguration.class)
class SecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("인증 없이 피드 생성 요청 시 401 반환")
    void createFeedWithoutAuth_returns401() throws Exception {
        FeedRequest request = FeedRequest.builder()
                .activity("RUNNING")
                .content("테스트")
                .build();

        mockMvc.perform(post("/api/feeds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("인증 없이 내 피드 조회 요청 시 401 반환")
    void getMyFeedsWithoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/feeds/my"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("인증 없이 댓글 생성 요청 시 401 반환")
    void createCommentWithoutAuth_returns401() throws Exception {
        mockMvc.perform(post("/api/feeds/1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"테스트\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("인증 없이 알림 조회 요청 시 401 반환")
    void getNotificationsWithoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("인증 없이 포인트 조회 요청 시 401 반환")
    void getPointsWithoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/users/points"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("인증 없이 프로필 수정 요청 시 401 반환")
    void updateProfileWithoutAuth_returns401() throws Exception {
        mockMvc.perform(put("/api/users/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"테스트\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("공개 엔드포인트는 인증 없이 접근 가능 - 헬스체크")
    void healthCheck_returns200() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("공개 엔드포인트는 인증 없이 접근 가능 - Swagger")
    void swaggerUI_returns200() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }
}
