package com.example.demo.controller;

import com.example.demo.config.TestRedisConfig;
import com.example.demo.config.TestSecurityConfig;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.Role;
import com.example.demo.dto.request.UserProfileRequest;
import com.example.demo.service.PointService;
import com.example.demo.service.UserPenaltyService;
import com.example.demo.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@ActiveProfiles("test")
@Import({TestSecurityConfig.class, TestRedisConfig.class}) // ★★★ 친구들 다 모여!
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private PointService pointService;

    @MockitoBean
    private UserPenaltyService userPenaltyService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .userId("testuser")
                .name("testUser")
                .role(Role.MEMBER)
                .build();
    }

    @Test
    @WithUserDetails(value = "testuser", userDetailsServiceBeanName = "userDetailsService")
    @DisplayName("프로필 수정 성공")
    void updateProfileTest() throws Exception {
        UserProfileRequest request = new UserProfileRequest("newName", 180.0, 75.0);
        given(userService.updateProfile(any(), eq("newName"), eq(180.0), eq(75.0))).willReturn(user);
        given(pointService.getTotalPoints(any())).willReturn(100L);

        mockMvc.perform(put("/api/users/me/profile")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @WithUserDetails(value = "testuser", userDetailsServiceBeanName = "userDetailsService")
    @DisplayName("아바타 수정 성공")
    void updateAvatarTest() throws Exception {
        String jsonRequest = "{\"avatar\":\"newAvatar.jpg\"}";

        given(userService.updateAvatar(any(), eq("newAvatar.jpg"))).willReturn(user);
        given(pointService.getTotalPoints(any())).willReturn(100L);

        mockMvc.perform(put("/api/users/me/avatar")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isOk());
    }
}