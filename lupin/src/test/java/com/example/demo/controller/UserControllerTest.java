package com.example.demo.controller;

import com.example.demo.dto.response.UserProfileResponse;
import com.example.demo.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.example.demo.config.TestRedisConfig;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Import(TestRedisConfig.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser
@DisplayName("UserController 테스트")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Test
    @DisplayName("사용자 프로필 조회 성공")
    void getUserProfile_Success() throws Exception {
        // given
        UserProfileResponse profile = UserProfileResponse.builder()
                .id(1L)
                .email("user01@test.com")
                .realName("테스트유저")
                .currentPoints(100L)
                .monthlyPoints(50L)
                .build();

        given(userService.getUserProfile(1L)).willReturn(profile);

        // when & then
        mockMvc.perform(get("/api/users/1/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.realName").value("테스트유저"));
    }

    @Test
    @DisplayName("상위 사용자 조회 성공")
    void getTopUsersByPoints_Success() throws Exception {
        // given
        Map<String, Object> user1 = new HashMap<>();
        user1.put("id", 1L);
        user1.put("name", "유저1");
        user1.put("points", 100L);

        List<Map<String, Object>> topUsers = Arrays.asList(user1);

        given(userService.getTopUsersByPoints(10)).willReturn(topUsers);

        // when & then
        mockMvc.perform(get("/api/users/top"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("유저1"));
    }

    @Test
    @DisplayName("포인트 적립 성공")
    void addPoints_Success() throws Exception {
        // when & then
        mockMvc.perform(post("/api/users/1/points/add")
                        .param("amount", "10")
                        .param("reason", "운동"))
                .andExpect(status().isOk());

        then(userService).should().addPoints(eq(1L), eq(10L), eq("운동"), any());
    }

    @Test
    @DisplayName("통계 조회 성공")
    void getStatistics_Success() throws Exception {
        // given
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", 100);
        stats.put("activeUsersThisMonth", 50L);

        given(userService.getStatistics()).willReturn(stats);

        // when & then
        mockMvc.perform(get("/api/users/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(100));
    }
}
