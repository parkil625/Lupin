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

    @Test
    @DisplayName("각 채팅방의 최근 메시지 조회")
    void getLatestMessageForEachRoom() {
        // Given
        String roomId1 = "1:21";
        String roomId2 = "2:21";

        ChatMessage latestMessage1 = ChatMessage.builder()
                .id(2L)
                .roomId(roomId1)
                .sender(doctor)
                .content("네, 환자분")
                .time(LocalDateTime.now())
                .build();

        ChatMessage latestMessage2 = ChatMessage.builder()
                .id(4L)
                .roomId(roomId2)
                .sender(patient)
                .content("안녕하세요")
                .time(LocalDateTime.now().minusMinutes(5))
                .build();

        given(chatRepository.findByRoomIdOrderByTimeAsc(roomId1))
                .willReturn(Arrays.asList(message1, latestMessage1));
        given(chatRepository.findByRoomIdOrderByTimeAsc(roomId2))
                .willReturn(List.of(latestMessage2));

        // When
        ChatMessage result1 = chatService.getLatestMessageInRoom(roomId1);
        ChatMessage result2 = chatService.getLatestMessageInRoom(roomId2);

        // Then
        assertThat(result1.getContent()).isEqualTo("네, 환자분");
        assertThat(result2.getContent()).isEqualTo("안녕하세요");
        verify(chatRepository, times(1)).findByRoomIdOrderByTimeAsc(roomId1);
        verify(chatRepository, times(1)).findByRoomIdOrderByTimeAsc(roomId2);
    }

    @Test
    @DisplayName("채팅방 목록이 최근 메시지 시간순 정렬")
    void getChatRoomsSortedByLatestMessage() {
        // Given
        Long doctorId = 21L;
        LocalDateTime now = LocalDateTime.now();

        // roomId1의 최근 메시지는 5분 전
        ChatMessage room1Message = ChatMessage.builder()
                .id(1L)
                .roomId("1:21")
                .sender(patient)
                .content("메시지1")
                .time(now.minusMinutes(5))
                .build();

        // roomId2의 최근 메시지는 지금
        ChatMessage room2Message = ChatMessage.builder()
                .id(2L)
                .roomId("2:21")
                .sender(patient)
                .content("메시지2")
                .time(now)
                .build();

        // roomId3의 최근 메시지는 10분 전
        ChatMessage room3Message = ChatMessage.builder()
                .id(3L)
                .roomId("3:21")
                .sender(patient)
                .content("메시지3")
                .time(now.minusMinutes(10))
                .build();

        List<ChatMessage> allMessages = Arrays.asList(room1Message, room2Message, room3Message);
        given(chatRepository.findAll()).willReturn(allMessages);

        given(chatRepository.findByRoomIdOrderByTimeAsc("1:21")).willReturn(List.of(room1Message));
        given(chatRepository.findByRoomIdOrderByTimeAsc("2:21")).willReturn(List.of(room2Message));
        given(chatRepository.findByRoomIdOrderByTimeAsc("3:21")).willReturn(List.of(room3Message));

        // When
        List<String> sortedRoomIds = chatService.getChatRoomsSortedByLatestMessage(doctorId);

        // Then
        assertThat(sortedRoomIds).hasSize(3);
        assertThat(sortedRoomIds.get(0)).isEqualTo("2:21");  // 가장 최근
        assertThat(sortedRoomIds.get(1)).isEqualTo("1:21");  // 5분 전
        assertThat(sortedRoomIds.get(2)).isEqualTo("3:21");  // 10분 전
    }

    @Test
    @DisplayName("특정 채팅방의 읽지 않은 메시지 개수")
    void getUnreadMessageCount() {
        // Given
        String roomId = "1:21";
        Long userId = 21L;  // doctor ID

        ChatMessage unreadMessage1 = ChatMessage.builder()
                .id(1L)
                .roomId(roomId)
                .sender(patient)
                .content("안읽은 메시지1")
                .isRead(false)
                .build();

        ChatMessage unreadMessage2 = ChatMessage.builder()
                .id(2L)
                .roomId(roomId)
                .sender(patient)
                .content("안읽은 메시지2")
                .isRead(false)
                .build();

        List<ChatMessage> unreadMessages = Arrays.asList(unreadMessage1, unreadMessage2);
        given(chatRepository.findUnreadMessages(roomId, userId))
                .willReturn(unreadMessages);

        // When
        int count = chatService.getUnreadMessageCount(roomId, userId);

        // Then
        assertThat(count).isEqualTo(2);
        verify(chatRepository, times(1)).findUnreadMessages(roomId, userId);
    }

    @Test
    @DisplayName("의사의 모든 채팅방 읽지 않은 메시지 총합")
    void getTotalUnreadMessageCountForDoctor() {
        // Given
        Long doctorId = 21L;

        // Room 1:21 - 2개 안읽음
        ChatMessage unread1 = ChatMessage.builder()
                .id(1L)
                .roomId("1:21")
                .sender(patient)
                .content("메시지1")
                .isRead(false)
                .build();

        ChatMessage unread2 = ChatMessage.builder()
                .id(2L)
                .roomId("1:21")
                .sender(patient)
                .content("메시지2")
                .isRead(false)
                .build();

        // Room 2:21 - 1개 안읽음
        ChatMessage unread3 = ChatMessage.builder()
                .id(3L)
                .roomId("2:21")
                .sender(patient)
                .content("메시지3")
                .isRead(false)
                .build();

        List<ChatMessage> allMessages = Arrays.asList(unread1, unread2, unread3);
        given(chatRepository.findAll()).willReturn(allMessages);

        given(chatRepository.findUnreadMessages("1:21", doctorId))
                .willReturn(Arrays.asList(unread1, unread2));
        given(chatRepository.findUnreadMessages("2:21", doctorId))
                .willReturn(List.of(unread3));

        // When
        int totalCount = chatService.getTotalUnreadMessageCountForDoctor(doctorId);

        // Then
        assertThat(totalCount).isEqualTo(3);  // 2 + 1 = 3
    }

    @Test
    @DisplayName("읽음 처리 후 카운트 감소")
    void shouldDecreaseUnreadCountAfterMarkingAsRead() {
        // Given
        String roomId = "1:21";
        Long userId = 21L;  // doctor ID

        ChatMessage unread1 = ChatMessage.builder()
                .id(1L)
                .roomId(roomId)
                .sender(patient)
                .content("안읽은 메시지1")
                .isRead(false)
                .build();

        ChatMessage unread2 = ChatMessage.builder()
                .id(2L)
                .roomId(roomId)
                .sender(patient)
                .content("안읽은 메시지2")
                .isRead(false)
                .build();

        // 읽기 전: 2개
        given(chatRepository.findUnreadMessages(roomId, userId))
                .willReturn(Arrays.asList(unread1, unread2));

        int countBefore = chatService.getUnreadMessageCount(roomId, userId);

        // When: 읽음 처리
        chatService.markAsRead(roomId, userId);

        // 읽음 처리 후: 0개
        given(chatRepository.findUnreadMessages(roomId, userId))
                .willReturn(List.of());

        int countAfter = chatService.getUnreadMessageCount(roomId, userId);

        // Then
        assertThat(countBefore).isEqualTo(2);
        assertThat(countAfter).isEqualTo(0);
        verify(chatRepository, times(1)).markAllAsReadInRoom(roomId, userId);
    }

    @Test
    @DisplayName("채팅방 ID로 환자 정보 조회")
    void getPatientInfoFromRoomId() {
        // Given
        String roomId = "1:21";  // patientId:doctorId
        Long patientId = 1L;

        given(userRepository.findById(patientId))
                .willReturn(java.util.Optional.of(patient));

        // When
        User result = chatService.getPatientFromRoomId(roomId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(patientId);
        assertThat(result.getUserId()).isEqualTo("patient01");
        assertThat(result.getName()).isEqualTo("환자1");
        verify(userRepository, times(1)).findById(patientId);
    }
}
