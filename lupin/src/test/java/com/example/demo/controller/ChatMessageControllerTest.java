package com.example.demo.controller;

import com.example.demo.config.TestRedisConfig;
import com.example.demo.dto.response.ChatMessageResponse;
import com.example.demo.dto.response.ChatRoomResponse;
import com.example.demo.service.ChatMessageService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Import(TestRedisConfig.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser
@DisplayName("ChatMessageController 테스트")
class ChatMessageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ChatMessageService chatMessageService;

    @Test
    @DisplayName("채팅 메시지 전송 성공")
    void sendMessage_Success() throws Exception {
        // given
        ChatMessageResponse response = ChatMessageResponse.builder()
                .id(1L)
                .roomId("room-123")
                .senderId(1L)
                .content("안녕하세요")
                .sentAt(LocalDateTime.now())
                .build();

        given(chatMessageService.sendMessage(any())).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/chat/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"안녕하세요\", \"senderId\": 1, \"patientId\": 10, \"doctorId\": 5}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.roomId").value("room-123"))
                .andExpect(jsonPath("$.content").value("안녕하세요"));
    }

    @Test
    @DisplayName("특정 채팅방의 메시지 목록 조회 - 페이징")
    void getMessagesByRoomId_Success() throws Exception {
        // given
        ChatMessageResponse response1 = ChatMessageResponse.builder()
                .id(1L)
                .roomId("room-123")
                .content("메시지 1")
                .build();
        ChatMessageResponse response2 = ChatMessageResponse.builder()
                .id(2L)
                .roomId("room-123")
                .content("메시지 2")
                .build();

        Page<ChatMessageResponse> page = new PageImpl<>(Arrays.asList(response1, response2), PageRequest.of(0, 50), 2);
        given(chatMessageService.getMessagesByRoomId(eq("room-123"), any())).willReturn(page);

        // when & then
        mockMvc.perform(get("/api/chat/rooms/room-123/messages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @DisplayName("특정 채팅방의 메시지 목록 조회 - 전체")
    void getAllMessagesByRoomId_Success() throws Exception {
        // given
        ChatMessageResponse response1 = ChatMessageResponse.builder()
                .id(1L)
                .roomId("room-123")
                .build();
        ChatMessageResponse response2 = ChatMessageResponse.builder()
                .id(2L)
                .roomId("room-123")
                .build();

        List<ChatMessageResponse> responses = Arrays.asList(response1, response2);
        given(chatMessageService.getAllMessagesByRoomId("room-123")).willReturn(responses);

        // when & then
        mockMvc.perform(get("/api/chat/rooms/room-123/messages/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("환자와 의사 간의 메시지 조회")
    void getMessagesBetweenUsers_Success() throws Exception {
        // given
        ChatMessageResponse response1 = ChatMessageResponse.builder()
                .id(1L)
                .senderId(1L)
                .build();

        List<ChatMessageResponse> responses = Arrays.asList(response1);
        given(chatMessageService.getMessagesBetweenUsers(1L, 2L)).willReturn(responses);

        // when & then
        mockMvc.perform(get("/api/chat/messages/between")
                        .param("patientId", "1")
                        .param("doctorId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("특정 채팅방의 읽지 않은 메시지 수 조회")
    void getUnreadMessageCount_Success() throws Exception {
        // given
        given(chatMessageService.getUnreadMessageCount("room-123", 1L)).willReturn(5L);

        // when & then
        mockMvc.perform(get("/api/chat/rooms/room-123/unread-count")
                        .param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(5));
    }

    @Test
    @DisplayName("특정 채팅방의 메시지 전체 읽음 처리")
    void markAllAsRead_Success() throws Exception {
        // when & then
        mockMvc.perform(patch("/api/chat/rooms/room-123/read-all")
                        .param("userId", "1"))
                .andExpect(status().isOk());

        then(chatMessageService).should().markAllAsRead("room-123", 1L);
    }

    @Test
    @DisplayName("특정 사용자가 참여한 채팅방 목록 조회")
    void getChatRoomsByUserId_Success() throws Exception {
        // given
        ChatRoomResponse response1 = ChatRoomResponse.builder()
                .roomId("room-123")
                .patientId(1L)
                .doctorId(2L)
                .build();

        List<ChatRoomResponse> responses = Arrays.asList(response1);
        given(chatMessageService.getChatRoomsByUserId(1L)).willReturn(responses);

        // when & then
        mockMvc.perform(get("/api/chat/rooms/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("메시지 상세 조회")
    void getMessageDetail_Success() throws Exception {
        // given
        ChatMessageResponse response = ChatMessageResponse.builder()
                .id(1L)
                .roomId("room-123")
                .content("메시지 내용")
                .build();

        given(chatMessageService.getMessageDetail(1L)).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/chat/messages/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.content").value("메시지 내용"));
    }

    @Test
    @DisplayName("메시지 삭제")
    void deleteMessage_Success() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/chat/messages/1")
                        .param("userId", "1"))
                .andExpect(status().isNoContent());

        then(chatMessageService).should().deleteMessage(1L, 1L);
    }
}
