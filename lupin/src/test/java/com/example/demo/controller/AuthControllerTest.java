package com.example.demo.controller;

import com.example.demo.dto.LoginDto;
import com.example.demo.dto.request.LoginRequest;
import com.example.demo.service.AuthService;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Import(TestRedisConfig.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser
@DisplayName("AuthController 테스트")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @Test
    @DisplayName("로그인 성공")
    void login_Success() throws Exception {
        // given
        LoginRequest request = new LoginRequest("user01", "password");
        LoginDto loginDto = LoginDto.builder()
                .id(1L)
                .userId("user01")
                .email("test@test.com")
                .name("테스트유저")
                .role("MEMBER")
                .accessToken("accessToken")
                .refreshToken("refreshToken")
                .build();

        given(authService.login(any(LoginRequest.class))).willReturn(loginDto);

        // when & then
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("accessToken"));
    }

    @Test
    @DisplayName("토큰 재발급 - 쿠키 없음")
    void reissue_NoCookie_Returns401() throws Exception {
        // when & then
        mockMvc.perform(post("/api/auth/reissue")
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("토큰 재발급 성공")
    void reissue_Success() throws Exception {
        // given
        LoginDto newTokens = LoginDto.builder()
                .id(1L)
                .userId("user01")
                .accessToken("newAccessToken")
                .refreshToken("newRefreshToken")
                .build();

        given(authService.reissue("validRefreshToken")).willReturn(newTokens);

        // when & then
        mockMvc.perform(post("/api/auth/reissue")
                        .with(csrf())
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", "validRefreshToken")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("newAccessToken"));
    }

    @Test
    @DisplayName("로그아웃 성공")
    void logout_Success() throws Exception {
        // when & then
        mockMvc.perform(post("/api/auth/logout")
                        .with(csrf())
                        .header("Authorization", "Bearer accessToken"))
                .andExpect(status().isOk());

        then(authService).should().logout("Bearer accessToken");
    }
}
