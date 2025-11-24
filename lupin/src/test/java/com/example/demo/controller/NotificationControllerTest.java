package com.example.demo.controller;

import com.example.demo.dto.response.NotificationResponse;
import com.example.demo.service.NotificationService;
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
import java.util.List;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Import(TestRedisConfig.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser
@DisplayName("NotificationController 테스트")
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;

    @Test
    @DisplayName("읽지 않은 알림 조회 성공")
    void getUnreadNotifications_Success() throws Exception {
        // given
        NotificationResponse notification = NotificationResponse.builder()
                .id(1L)
                .type("like")
                .title("새로운 좋아요")
                .content("테스트님이 좋아요를 눌렀습니다.")
                .build();

        List<NotificationResponse> notifications = Arrays.asList(notification);

        given(notificationService.getUnreadNotificationsByUserId(1L)).willReturn(notifications);

        // when & then
        mockMvc.perform(get("/api/notifications/users/1/unread"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("like"));
    }

    @Test
    @DisplayName("읽지 않은 알림 수 조회 성공")
    void getUnreadCount_Success() throws Exception {
        // given
        given(notificationService.getUnreadCountByUserId(1L)).willReturn(5L);

        // when & then
        mockMvc.perform(get("/api/notifications/users/1/unread/count"))
                .andExpect(status().isOk())
                .andExpect(content().string("5"));
    }

    @Test
    @DisplayName("알림 읽음 처리 성공")
    void markAsRead_Success() throws Exception {
        // given
        NotificationResponse response = NotificationResponse.builder()
                .id(1L)
                .type("like")
                .isRead(true)
                .build();

        given(notificationService.markAsRead(1L, 1L)).willReturn(response);

        // when & then
        mockMvc.perform(patch("/api/notifications/1/read")
                        .param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isRead").value(true));
    }

    @Test
    @DisplayName("모든 알림 읽음 처리 성공")
    void markAllAsRead_Success() throws Exception {
        // when & then
        mockMvc.perform(patch("/api/notifications/users/1/read-all"))
                .andExpect(status().isOk());

        then(notificationService).should().markAllAsReadByUserId(1L);
    }

    @Test
    @DisplayName("알림 삭제 성공")
    void deleteNotification_Success() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/notifications/1")
                        .param("userId", "1"))
                .andExpect(status().isNoContent());

        then(notificationService).should().deleteNotification(1L, 1L);
    }
}
