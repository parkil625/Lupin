package com.example.demo.controller;

import com.example.demo.config.TestRedisConfig;
import com.example.demo.config.TestSecurityConfig;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.Role;
import com.example.demo.dto.request.AppointmentRequest;
import com.example.demo.service.AppointmentService;
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

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AppointmentController.class)
@ActiveProfiles("test")
@Import({TestSecurityConfig.class, TestRedisConfig.class}) // ★★★ 친구들 다 모여!
class AppointmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AppointmentService appointmentService;

    private User patient;

    @BeforeEach
    void setUp() {
        patient = User.builder()
                .id(1L)
                .userId("patient")
                .name("Patient")
                .role(Role.MEMBER)
                .build();
    }

    @Test
    @WithUserDetails(value = "patient", userDetailsServiceBeanName = "userDetailsService")
    @DisplayName("예약 생성 성공")
    void createAppointmentTest() throws Exception {
        AppointmentRequest request = new AppointmentRequest(1L, 2L, LocalDateTime.now().plusDays(1));
        given(appointmentService.createAppointment(any())).willReturn(1L);

        mockMvc.perform(post("/api/appointment")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}
