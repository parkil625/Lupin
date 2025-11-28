package com.example.demo.controller;

import com.example.demo.domain.entity.ChatMessage;
import com.example.demo.domain.entity.User;
import com.example.demo.repository.ChatRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.ChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatService 테스트")
class ChatServiceTest {

    @Mock
    private ChatRepository chatRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ChatService chatService;

    private User patient;
    private User doctor;
    private ChatMessage message1;
    private ChatMessage message2;
    private String roomId;

    @BeforeEach
    void setUp() {
        patient = User.builder().id(1L).userId("patient01").name("환자1").build();
        doctor = User.builder().id(21L).userId("doctor01").name("의사1").build();

        // generateRoomId 메서드는 Entity에 static으로 구현되어 있다고 가정합니다.
        roomId = "1:21";

        message1 = ChatMessage.builder()
                .id(1L)
                .roomId(roomId)
                .sender(patient)
                .content("안녕하세요")
                .time(LocalDateTime.now().minusMinutes(10))
                .isRead(false)
                .build();

        message2 = ChatMessage.builder()
                .id(2L)
                .roomId(roomId)
                .sender(doctor)
                .content("네 안녕하세요")
                .time(LocalDateTime.now().minusMinutes(5))
                .isRead(false)
                .build();
    }

    @Test
    @DisplayName("메시지 저장 - 성공")
    void saveMessage_Success() {
        // Given
        given(userRepository.findById(patient.getId())).willReturn(Optional.of(patient));
        given(chatRepository.save(any(ChatMessage.class))).willReturn(message1);

        // When
        ChatMessage savedMessage = chatService.saveMessage(roomId, patient.getId(), "안녕하세요");

        // Then
        assertThat(savedMessage).isNotNull();
        assertThat(savedMessage.getSender()).isEqualTo(patient);

        verify(userRepository, times(1)).findById(patient.getId());
        verify(chatRepository, times(1)).save(any(ChatMessage.class));
    }

    @Test
    @DisplayName("메시지 저장 - 존재하지 않는 사용자 예외")
    void saveMessage_UserNotFound() {
        // Given
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> chatService.saveMessage(roomId, 999L, "메시지"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 사용자입니다.");

        verify(chatRepository, never()).save(any(ChatMessage.class));
    }

    @Test
    @DisplayName("채팅 기록 전체 조회 - N+1 최적화 메서드 호출 확인")
    void getChatHistory_Success() {
        // Given
        List<ChatMessage> messages = Arrays.asList(message1, message2);

        given(chatRepository.findByRoomIdWithSender(roomId)).willReturn(messages);

        // When
        List<ChatMessage> result = chatService.getChatHistory(roomId);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getContent()).isEqualTo("안녕하세요");

        // 🚨 검증: 최적화된 쿼리 메서드가 호출되었는지 확인
        verify(chatRepository, times(1)).findByRoomIdWithSender(roomId);
    }

    // ❌ getChatPageHistory_Pagination 테스트는 기능 삭제로 인해 제거됨

    @Test
    @DisplayName("안 읽은 메시지 조회")
    void getUnreadHistory_Success() {
        // Given
        List<ChatMessage> unreadMessages = List.of(message2);
        given(chatRepository.findUnreadMessages(roomId, patient.getId())).willReturn(unreadMessages);

        // When
        List<ChatMessage> result = chatService.getUnreadHistory(roomId, patient.getId());

        // Then
        assertThat(result).hasSize(1);
        verify(chatRepository, times(1)).findUnreadMessages(roomId, patient.getId());
    }

    @Test
    @DisplayName("메시지 읽음 처리")
    void markAsRead_Success() {
        // Given
        // 🚨 중요: Repository 메서드가 void를 반환하므로 willReturn(2)는 불가능합니다.
        // 그냥 아무 설정 없이 두거나, 명시적으로 doNothing()을 쓸 수 있습니다.

        // When
        chatService.markAsRead(roomId, patient.getId());

        // Then
        // 메서드가 정확한 파라미터로 호출되었는지만 검증하면 됩니다.
        verify(chatRepository, times(1)).markAllAsReadInRoom(roomId, patient.getId());
    }
}