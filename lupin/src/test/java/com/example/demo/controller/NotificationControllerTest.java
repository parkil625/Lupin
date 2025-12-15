package com.example.demo.controller;

import com.example.demo.config.TestRedisConfig;
import com.example.demo.config.TestSecurityConfig;
import com.example.demo.domain.entity.Notification;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.NotificationType;
import com.example.demo.domain.enums.Role;
import com.example.demo.service.NotificationCommandService;
import com.example.demo.service.NotificationReadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
@ActiveProfiles("test")
@Import({TestSecurityConfig.class, TestRedisConfig.class}) // ★★★ 친구들 다 모여!
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationReadService notificationReadService;

    @MockitoBean
    private NotificationCommandService notificationCommandService;

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
    @DisplayName("알림 목록 조회 성공")
    void getNotificationsTest() throws Exception {
        Notification notification = Notification.builder()
                .user(user)
                .type(NotificationType.COMMENT)
                .title("title")
                .content("content")
                .build();
        given(notificationReadService.getNotifications(any(User.class))).willReturn(List.of(notification));

        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isOk());
    }
}