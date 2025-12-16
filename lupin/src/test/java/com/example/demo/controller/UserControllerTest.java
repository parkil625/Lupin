package com.example.demo.controller;

import com.example.demo.config.TestRedisConfiguration;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.PenaltyType;
import com.example.demo.domain.enums.Role;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.PointService;
import com.example.demo.service.UserPenaltyService;
import com.example.demo.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestRedisConfiguration.class)
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

    @MockitoBean
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .userId("testuser")
                .name("테스트유저")
                .role(Role.MEMBER)
                .height(175.0)
                .weight(70.0)
                .birthDate(LocalDate.of(1990, 1, 1))
                .gender("남성")
                .build();

        given(userRepository.findByUserId("testuser")).willReturn(Optional.of(testUser));
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("GET /api/users/{userId} - 사용자 정보 조회 성공")
    void getUserInfo_Success() throws Exception {
        // given
        given(userService.getUserInfo(1L)).willReturn(testUser);

        // when & then
        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("테스트유저"))
                .andExpect(jsonPath("$.birthDate").value("1990-01-01"))
                .andExpect(jsonPath("$.gender").value("남성"));
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("PUT /api/users/{userId} - 사용자 정보 수정 성공 (생년월일, 성별 포함)")
    void updateUser_Success() throws Exception {
        // given
        // [핵심 수정] 컨트롤러가 응답을 만들기 위해 호출하는 getUserInfo 메서드 스터빙
        given(userService.getUserInfo(1L)).willReturn(testUser);

        String requestBody = """
            {
                "name": "수정된이름",
                "height": 180.5,
                "weight": 75.0,
                "birthDate": "1995-05-05",
                "gender": "남성"
            }
        """;

        // when
        mockMvc.perform(put("/api/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());

        // then: 서비스 호출 검증
        ArgumentCaptor<String> nameCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Double> heightCaptor = ArgumentCaptor.forClass(Double.class);
        ArgumentCaptor<Double> weightCaptor = ArgumentCaptor.forClass(Double.class);
        ArgumentCaptor<LocalDate> birthDateCaptor = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<String> genderCaptor = ArgumentCaptor.forClass(String.class);

        verify(userService).updateProfile(
                any(User.class),
                nameCaptor.capture(),
                heightCaptor.capture(),
                weightCaptor.capture(),
                birthDateCaptor.capture(),
                genderCaptor.capture()
        );

        assertThat(nameCaptor.getValue()).isEqualTo("수정된이름");
        assertThat(heightCaptor.getValue()).isEqualTo(180.5);
        assertThat(weightCaptor.getValue()).isEqualTo(75.0);
        assertThat(birthDateCaptor.getValue()).isEqualTo(LocalDate.of(1995, 5, 5));
        assertThat(genderCaptor.getValue()).isEqualTo("남성");
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("PUT /api/users/profile - 프로필 수정 성공")
    void updateProfile_Success() throws Exception {
        // given
        String requestBody = """
            {
                "name": "새이름",
                "height": 180.0,
                "weight": 75.0,
                "birthDate": "1990-01-01",
                "gender": "여성"
            }
        """;

        // when & then
        mockMvc.perform(put("/api/users/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());

        verify(userService).updateProfile(
                any(User.class),
                eq("새이름"),
                eq(180.0),
                eq(75.0),
                eq(LocalDate.of(1990, 1, 1)),
                eq("여성")
        );
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("GET /api/users/points - 내 포인트 조회 성공")
    void getMyPoints_Success() throws Exception {
        given(pointService.getTotalPoints(any(User.class))).willReturn(1000L);

        mockMvc.perform(get("/api/users/points"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPoints").value(1000L));
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("GET /api/users/points/monthly - 이번 달 포인트 조회 성공")
    void getMonthlyPoints_Success() throws Exception {
        given(pointService.getMonthlyPoints(any(User.class), any(YearMonth.class))).willReturn(500L);

        mockMvc.perform(get("/api/users/points/monthly"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthlyPoints").value(500L));
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("GET /api/users/penalty - 활성 제재 확인 성공")
    void checkPenalty_Success() throws Exception {
        given(userPenaltyService.hasActivePenalty(any(User.class), eq(PenaltyType.COMMENT)))
                .willReturn(true);

        mockMvc.perform(get("/api/users/penalty")
                        .param("type", "COMMENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasPenalty").value(true));
    }
}