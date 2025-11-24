package com.example.demo.controller;

import com.example.demo.service.ReportService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser
@DisplayName("ReportController 테스트")
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReportService reportService;

    @Test
    @DisplayName("피드 신고 성공")
    void reportFeed_Success() throws Exception {
        // when & then
        mockMvc.perform(post("/api/reports/feeds/1")
                        .param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("신고가 접수되었습니다."));

        then(reportService).should().reportFeed(1L, 1L);
    }

    @Test
    @DisplayName("댓글 신고 성공")
    void reportComment_Success() throws Exception {
        // when & then
        mockMvc.perform(post("/api/reports/comments/1")
                        .param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("신고가 접수되었습니다."));

        then(reportService).should().reportComment(1L, 1L);
    }

    @Test
    @DisplayName("피드 신고 상태 조회 성공")
    void getFeedReportStatus_Success() throws Exception {
        // given
        given(reportService.hasUserReportedFeed(1L, 1L)).willReturn(true);
        given(reportService.getFeedReportCount(1L)).willReturn(3L);

        // when & then
        mockMvc.perform(get("/api/reports/feeds/1/status")
                        .param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reported").value(true))
                .andExpect(jsonPath("$.reportCount").value(3));
    }

    @Test
    @DisplayName("댓글 신고 상태 조회 성공")
    void getCommentReportStatus_Success() throws Exception {
        // given
        given(reportService.hasUserReportedComment(1L, 1L)).willReturn(false);
        given(reportService.getCommentReportCount(1L)).willReturn(0L);

        // when & then
        mockMvc.perform(get("/api/reports/comments/1/status")
                        .param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reported").value(false))
                .andExpect(jsonPath("$.reportCount").value(0));
    }
}
