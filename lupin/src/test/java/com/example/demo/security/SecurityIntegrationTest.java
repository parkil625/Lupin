package com.example.demo.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Security 통합 테스트")
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    // 인증 없이 접근 가능한 엔드포인트 테스트
    @Test
    @DisplayName("인증 없이 헬스 체크 접근 가능")
    void healthCheck_NoAuth_Success() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("인증 없이 루트 엔드포인트 접근 가능")
    void root_NoAuth_Success() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("인증 없이 로그인 엔드포인트 접근 가능")
    void login_NoAuth_Accessible() throws Exception {
        // POST to login endpoint should be accessible (not 401)
        int status = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andReturn().getResponse().getStatus();
        assertThat(status).isNotEqualTo(401);
    }

    @Test
    @DisplayName("인증 없이 회원가입 엔드포인트 접근 가능")
    void register_NoAuth_Accessible() throws Exception {
        int status = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andReturn().getResponse().getStatus();
        assertThat(status).isNotEqualTo(401);
    }

    // 인증이 필요한 엔드포인트 테스트
    @Test
    @DisplayName("인증 없이 피드 조회 시 401")
    void feeds_NoAuth_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/feeds"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("인증 없이 사용자 정보 조회 시 401")
    void users_NoAuth_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("인증 없이 알림 조회 시 401")
    void notifications_NoAuth_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("인증 없이 복권 조회 시 401")
    void lottery_NoAuth_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/lotteries"))
                .andExpect(status().isUnauthorized());
    }

    // 인증된 사용자 테스트
    @Test
    @WithMockUser
    @DisplayName("인증된 사용자 피드 조회 접근 가능")
    void feeds_WithAuth_Accessible() throws Exception {
        int status = mockMvc.perform(get("/api/feeds"))
                .andReturn().getResponse().getStatus();
        // Should not be 401 or 403 (authenticated user has access)
        assertThat(status).isNotEqualTo(401);
        assertThat(status).isNotEqualTo(403);
    }

    @Test
    @WithMockUser
    @DisplayName("인증된 사용자 헬스 체크 접근 가능")
    void healthCheck_WithAuth_Success() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk());
    }

    // JWT 토큰 테스트
    @Test
    @DisplayName("잘못된 JWT 토큰으로 접근 시 401")
    void invalidToken_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/feeds")
                        .header("Authorization", "Bearer invalid.token.here"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("만료된 형식의 토큰으로 접근 시 401")
    void malformedToken_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/feeds")
                        .header("Authorization", "Bearer malformed"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Bearer 없이 토큰만 전송 시 401")
    void noBearerPrefix_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/feeds")
                        .header("Authorization", "some.token.here"))
                .andExpect(status().isUnauthorized());
    }

    // CORS preflight 테스트
    @Test
    @DisplayName("OPTIONS 요청은 인증 없이 허용")
    void options_NoAuth_Allowed() throws Exception {
        mockMvc.perform(options("/api/feeds")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk());
    }

    // 다양한 HTTP 메서드 테스트
    @Test
    @DisplayName("인증 없이 POST 요청 시 401")
    void post_NoAuth_Unauthorized() throws Exception {
        mockMvc.perform(post("/api/feeds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("인증 없이 PUT 요청 시 401")
    void put_NoAuth_Unauthorized() throws Exception {
        mockMvc.perform(put("/api/feeds/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("인증 없이 DELETE 요청 시 401")
    void delete_NoAuth_Unauthorized() throws Exception {
        mockMvc.perform(delete("/api/feeds/1"))
                .andExpect(status().isUnauthorized());
    }

    // OAuth 엔드포인트 테스트
    @Test
    @DisplayName("OAuth 로그인 엔드포인트는 인증 없이 접근 가능")
    void oauthLogin_NoAuth_Accessible() throws Exception {
        // OAuth login endpoints should be accessible (not 401)
        int status = mockMvc.perform(get("/api/oauth/google/login"))
                .andReturn().getResponse().getStatus();
        assertThat(status).isNotEqualTo(401);
    }

    // WebSocket 엔드포인트 테스트
    @Test
    @DisplayName("WebSocket 엔드포인트는 인증 없이 접근 가능")
    void websocket_NoAuth_Accessible() throws Exception {
        int status = mockMvc.perform(get("/ws/info"))
                .andReturn().getResponse().getStatus();
        assertThat(status).isNotEqualTo(401);
    }

    // 존재하지 않는 엔드포인트
    @Test
    @DisplayName("존재하지 않는 엔드포인트 인증 없이 접근 시 401")
    void notFound_NoAuth_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/nonexistent"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    @DisplayName("존재하지 않는 엔드포인트 인증 후 접근 시 401이 아님")
    void notFound_WithAuth_NotUnauthorized() throws Exception {
        int status = mockMvc.perform(get("/api/nonexistent"))
                .andReturn().getResponse().getStatus();
        // With auth, should not return 401
        assertThat(status).isNotEqualTo(401);
    }
}
