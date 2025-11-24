package com.example.demo.controller;

import com.example.demo.dto.LoginDto;
import com.example.demo.dto.response.OAuthConnectionResponse;
import com.example.demo.security.JwtTokenProvider;
import com.example.demo.service.OAuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser
@DisplayName("OAuthController 테스트")
class OAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OAuthService oAuthService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("네이버 로그인 성공")
    void naverLogin_Success() throws Exception {
        // given
        Map<String, String> request = new HashMap<>();
        request.put("code", "testCode");
        request.put("state", "testState");

        LoginDto loginDto = LoginDto.builder()
                .id(1L)
                .userId("user01")
                .accessToken("accessToken")
                .build();

        given(oAuthService.naverLogin("testCode", "testState")).willReturn(loginDto);

        // when & then
        mockMvc.perform(post("/api/oauth/naver/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("accessToken"));
    }

    @Test
    @DisplayName("카카오 로그인 성공")
    void kakaoLogin_Success() throws Exception {
        // given
        Map<String, String> request = new HashMap<>();
        request.put("code", "testCode");
        request.put("redirectUri", "http://localhost:3000/callback");

        LoginDto loginDto = LoginDto.builder()
                .id(1L)
                .userId("user01")
                .accessToken("accessToken")
                .build();

        given(oAuthService.kakaoLogin("testCode", "http://localhost:3000/callback")).willReturn(loginDto);

        // when & then
        mockMvc.perform(post("/api/oauth/kakao/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("accessToken"));
    }

    @Test
    @DisplayName("네이버 계정 연동 성공")
    void linkNaverAccount_Success() throws Exception {
        // given
        Map<String, String> request = new HashMap<>();
        request.put("code", "testCode");
        request.put("state", "testState");

        OAuthConnectionResponse response = OAuthConnectionResponse.builder()
                .provider("naver")
                .providerEmail("naver@test.com")
                .build();

        given(jwtTokenProvider.getEmail("testToken")).willReturn("user01");
        given(oAuthService.linkNaverAccount("user01", "testCode", "testState")).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/oauth/naver/link")
                        .header("Authorization", "Bearer testToken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("naver"));
    }

    @Test
    @DisplayName("카카오 계정 연동 성공")
    void linkKakaoAccount_Success() throws Exception {
        // given
        Map<String, String> request = new HashMap<>();
        request.put("code", "testCode");
        request.put("redirectUri", "http://localhost:3000/callback");

        OAuthConnectionResponse response = OAuthConnectionResponse.builder()
                .provider("kakao")
                .providerEmail("kakao@test.com")
                .build();

        given(jwtTokenProvider.getEmail("testToken")).willReturn("user01");
        given(oAuthService.linkKakaoAccount("user01", "testCode", "http://localhost:3000/callback")).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/oauth/kakao/link")
                        .header("Authorization", "Bearer testToken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("kakao"));
    }

    @Test
    @DisplayName("구글 계정 연동 성공")
    void linkGoogleAccount_Success() throws Exception {
        // given
        Map<String, String> request = new HashMap<>();
        request.put("token", "googleToken");

        OAuthConnectionResponse response = OAuthConnectionResponse.builder()
                .provider("google")
                .providerEmail("google@test.com")
                .build();

        given(jwtTokenProvider.getEmail("testToken")).willReturn("user01");
        given(oAuthService.linkGoogleAccount("user01", "googleToken")).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/oauth/google/link")
                        .header("Authorization", "Bearer testToken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("google"));
    }

    @Test
    @DisplayName("OAuth 연동 목록 조회 성공")
    void getConnections_Success() throws Exception {
        // given
        OAuthConnectionResponse naver = OAuthConnectionResponse.builder()
                .provider("naver")
                .providerEmail("naver@test.com")
                .build();
        OAuthConnectionResponse kakao = OAuthConnectionResponse.builder()
                .provider("kakao")
                .providerEmail("kakao@test.com")
                .build();

        List<OAuthConnectionResponse> connections = Arrays.asList(naver, kakao);

        given(jwtTokenProvider.getEmail("testToken")).willReturn("user01");
        given(oAuthService.getConnectionsByLoginId("user01")).willReturn(connections);

        // when & then
        mockMvc.perform(get("/api/oauth/connections")
                        .header("Authorization", "Bearer testToken"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].provider").value("naver"))
                .andExpect(jsonPath("$[1].provider").value("kakao"));
    }

    @Test
    @DisplayName("OAuth 연동 해제 성공")
    void unlinkOAuth_Success() throws Exception {
        // given
        given(jwtTokenProvider.getEmail("testToken")).willReturn("user01");

        // when & then
        mockMvc.perform(delete("/api/oauth/connections/naver")
                        .header("Authorization", "Bearer testToken"))
                .andExpect(status().isOk());

        then(oAuthService).should().unlinkOAuthByLoginId("user01", "naver");
    }
}
