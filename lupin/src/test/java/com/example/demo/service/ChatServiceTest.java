package com.example.demo.service;

import com.example.demo.domain.entity.ChatMessage;
import com.example.demo.domain.entity.User;
import com.example.demo.repository.ChatRepository;
import com.example.demo.repository.UserRepository;
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

/**
 * ChatService TDD 테스트
 */
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
        // Given: 테스트 데이터 준비
        patient = User.builder()
                .id(1L)
                .userId("patient01")
                .name("환자1")
                .build();

        doctor = User.builder()
                .id(21L)
                .userId("doctor01")
                .name("의사1")
                .build();

        roomId = ChatMessage.generateRoomId(patient.getId(), doctor.getId());

        message1 = ChatMessage.builder()
                .id(1L)
                .roomId(roomId)
                .sender(patient)
                .content("안녕하세요, 마음이 아파서 왔습니다.")
                .time(LocalDateTime.now().minusMinutes(10))
                .isRead(false)
                .build();

        message2 = ChatMessage.builder()
                .id(2L)
                .roomId(roomId)
                .sender(doctor)
                .content("네, 환자분")
                .time(LocalDateTime.now().minusMinutes(5))
                .isRead(false)
                .build();
    }

    @Test
    @DisplayName("메시지 저장 - 성공")
    void saveMessage_Success() {
        // Given
        given(userRepository.findById(patient.getId()))
                .willReturn(Optional.of(patient));
        given(chatRepository.save(any(ChatMessage.class)))
                .willReturn(message1);

        // When
        ChatMessage savedMessage = chatService.saveMessage(
                roomId,
                patient.getId(),
                "안녕하세요, 마음이 아파서 왔습니다."
        );

        // Then
        assertThat(savedMessage).isNotNull();
        assertThat(savedMessage.getSender()).isEqualTo(patient);


        verify(userRepository, times(1)).findById(patient.getId());
        verify(chatRepository, times(1)).save(any(ChatMessage.class));
    }

    @Test
    @DisplayName("메시지 저장 - 존재하지 않는 사용자")
    void saveMessage_UserNotFound() {
        // Given
        given(userRepository.findById(999L))
                .willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> chatService.saveMessage(roomId, 999L, "메시지"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 사용자입니다.");

        verify(chatRepository, never()).save(any(ChatMessage.class));
    }

    @Test
    @DisplayName("채팅 기록 조회 - 시간순 정렬")
    void getChatHistory_OrderByTimeAsc() {
        // Given
        List<ChatMessage> messages = Arrays.asList(message1, message2);
        given(chatRepository.findByRoomIdOrderByTimeAsc(roomId))
                .willReturn(messages);

        // When
        List<ChatMessage> result = chatService.getChatHistory(roomId);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getContent()).isEqualTo("안녕하세요, 마음이 아파서 왔습니다.");

        verify(chatRepository, times(1)).findByRoomIdOrderByTimeAsc(roomId);
    }

    @Test
    @DisplayName("안 읽은 메시지 조회")
    void getUnreadHistory_Success() {
        // Given
        List<ChatMessage> unreadMessages = List.of(message2);
        given(chatRepository.findUnreadMessages(roomId, patient.getId()))
                .willReturn(unreadMessages);

        // When
        List<ChatMessage> result = chatService.getUnreadHistory(roomId, patient.getId());

        // Then
        assertThat(result).hasSize(1);
        verify(chatRepository, times(1)).findUnreadMessages(roomId, patient.getId());
    }

    @Test
    @DisplayName("메시지 읽음 처리")
    void markAsRead_Success() {
        // When
        chatService.markAsRead(roomId, patient.getId());

        // Then
        verify(chatRepository, times(1)).markAllAsReadInRoom(roomId, patient.getId());
    }

    @Test
    @DisplayName("채팅방 ID 생성 테스트")
    void generateRoomId_Success() {
        // When
        String generatedRoomId = ChatMessage.generateRoomId(1L, 21L);

        // Then
        assertThat(generatedRoomId).isEqualTo("1:21");
    }

    @Test
    @DisplayName("의사 ID로 모든 채팅방 ID 조회")
    void getAllChatRoomsByDoctorId() {
        // Given
        Long doctorId = 21L;
        ChatMessage message3 = ChatMessage.builder()
                .id(3L)
                .roomId("2:21")  // patient 2와 doctor 21
                .sender(patient)
                .content("두 번째 채팅방 메시지")
                .build();

        List<ChatMessage> allMessages = Arrays.asList(message1, message2, message3);
        given(chatRepository.findAll())
                .willReturn(allMessages);

        // When
        List<String> roomIds = chatService.getAllChatRoomsByDoctorId(doctorId);

        // Then
        assertThat(roomIds).hasSize(2);
        assertThat(roomIds).containsExactlyInAnyOrder("1:21", "2:21");
        verify(chatRepository, times(1)).findAll();
    }
}
