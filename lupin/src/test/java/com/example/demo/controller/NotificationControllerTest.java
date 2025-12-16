package com.example.demo.controller;

import com.example.demo.config.TestRedisConfiguration;
import com.example.demo.domain.entity.Notification;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.NotificationType;
import com.example.demo.domain.enums.Role;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
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
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private UserRepository userRepository;

    private User testUser;
    private Notification testNotification;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .userId("testuser")
                .name("테스트유저")
                .role(Role.MEMBER)
                .build();

        testNotification = Notification.builder()
                .id(1L)
                .user(testUser)
                .type(NotificationType.COMMENT)
                .title("새 댓글")
                .content("누군가 댓글을 달았습니다")
                .isRead(false)
                .build();

        given(userRepository.findByUserId("testuser")).willReturn(Optional.of(testUser));
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("GET /api/notifications - 알림 목록 조회 성공")
    void getNotifications_Success() throws Exception {
        // given
        given(notificationService.getNotifications(any(User.class)))
                .willReturn(List.of(testNotification));

        // when & then
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].type").value("COMMENT"));
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("PATCH /api/notifications/{notificationId}/read - 알림 읽음 처리 성공")
    void markAsRead_Success() throws Exception {
        // when & then
        mockMvc.perform(patch("/api/notifications/1/read"))
                .andExpect(status().isOk());

        verify(notificationService).markAsRead(1L);
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("GET /api/notifications/unread - 읽지 않은 알림 존재 여부 확인 성공")
    void hasUnreadNotifications_Success() throws Exception {
        // given
        given(notificationService.hasUnreadNotifications(any(User.class))).willReturn(true);

        // when & then
        mockMvc.perform(get("/api/notifications/unread"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasUnread").value(true));
    }
}
