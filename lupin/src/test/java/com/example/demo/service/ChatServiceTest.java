package com.example.demo.service;

import com.example.demo.domain.entity.Appointment;
import com.example.demo.domain.entity.ChatMessage;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.AppointmentStatus;
import com.example.demo.repository.AppointmentRepository;
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

    @Mock
    private AppointmentRepository appointmentRepository;

    @InjectMocks
    private ChatService chatService;

    private User patient;
    private User doctor;
    private ChatMessage message1;
    private ChatMessage message2;
    private String roomId;
    private Long appointmentId;

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

        // 변경: roomId 형식을 appointment_{id}로 변경
        appointmentId = 1L;
        roomId = "appointment_" + appointmentId;

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
        assertThat(savedMessage.getRoomId()).isEqualTo(roomId);

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
    @DisplayName("채팅 기록 조회 - 시간순 정렬 (Eager Loading)")
    void getChatHistory_OrderByTimeAsc() {
        // Given
        List<ChatMessage> messages = Arrays.asList(message1, message2);
        given(chatRepository.findByRoomIdWithSenderEagerly(roomId))
                .willReturn(messages);

        // When
        List<ChatMessage> result = chatService.getChatHistory(roomId);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getContent()).isEqualTo("안녕하세요, 마음이 아파서 왔습니다.");

        verify(chatRepository, times(1)).findByRoomIdWithSenderEagerly(roomId);
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
    @DisplayName("의사 ID로 모든 채팅방 ID 조회 (appointment 기반)")
    void getAllChatRoomsByDoctorId() {
        // Given
        Long doctorId = 21L;

        // Appointment 생성
        Appointment appointment1 = Appointment.builder()
                .id(1L)
                .patient(patient)
                .doctor(doctor)
                .status(AppointmentStatus.SCHEDULED)
                .build();

        Appointment appointment2 = Appointment.builder()
                .id(2L)
                .patient(patient)
                .doctor(doctor)
                .status(AppointmentStatus.SCHEDULED)
                .build();

        given(appointmentRepository.findByDoctorIdOrderByDateDesc(doctorId))
                .willReturn(Arrays.asList(appointment1, appointment2));

        // 각 채팅방에 메시지가 있도록 모킹
        given(chatRepository.findByRoomIdOrderByTimeAsc("appointment_1"))
                .willReturn(List.of(message1));
        given(chatRepository.findByRoomIdOrderByTimeAsc("appointment_2"))
                .willReturn(List.of(message2));

        // When
        List<String> roomIds = chatService.getAllChatRoomsByDoctorId(doctorId);

        // Then
        assertThat(roomIds).hasSize(2);
        assertThat(roomIds).containsExactlyInAnyOrder("appointment_1", "appointment_2");
        verify(appointmentRepository, times(1)).findByDoctorIdOrderByDateDesc(doctorId);
    }

    @Test
    @DisplayName("각 채팅방의 최근 메시지 조회 (성능 최적화)")
    void getLatestMessageForEachRoom() {
        // Given
        String roomId1 = "appointment_1";
        String roomId2 = "appointment_2";

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

        // DESC 정렬이므로 최신 메시지가 첫 번째
        given(chatRepository.findTopByRoomIdOrderByTimeDesc(roomId1))
                .willReturn(List.of(latestMessage1));
        given(chatRepository.findTopByRoomIdOrderByTimeDesc(roomId2))
                .willReturn(List.of(latestMessage2));

        // When
        ChatMessage result1 = chatService.getLatestMessageInRoom(roomId1);
        ChatMessage result2 = chatService.getLatestMessageInRoom(roomId2);

        // Then
        assertThat(result1.getContent()).isEqualTo("네, 환자분");
        assertThat(result2.getContent()).isEqualTo("안녕하세요");
        verify(chatRepository, times(1)).findTopByRoomIdOrderByTimeDesc(roomId1);
        verify(chatRepository, times(1)).findTopByRoomIdOrderByTimeDesc(roomId2);
    }

    @Test
    @DisplayName("채팅방 목록이 최근 메시지 시간순 정렬 (appointment 기반)")
    void getChatRoomsSortedByLatestMessage() {
        // Given
        Long doctorId = 21L;
        LocalDateTime now = LocalDateTime.now();

        // appointment_1의 최근 메시지는 5분 전
        ChatMessage room1Message = ChatMessage.builder()
                .id(1L)
                .roomId("appointment_1")
                .sender(patient)
                .content("메시지1")
                .time(now.minusMinutes(5))
                .build();

        // appointment_2의 최근 메시지는 지금
        ChatMessage room2Message = ChatMessage.builder()
                .id(2L)
                .roomId("appointment_2")
                .sender(patient)
                .content("메시지2")
                .time(now)
                .build();

        // appointment_3의 최근 메시지는 10분 전
        ChatMessage room3Message = ChatMessage.builder()
                .id(3L)
                .roomId("appointment_3")
                .sender(patient)
                .content("메시지3")
                .time(now.minusMinutes(10))
                .build();

        Appointment appointment1 = Appointment.builder().id(1L).patient(patient).doctor(doctor).status(AppointmentStatus.SCHEDULED).build();
        Appointment appointment2 = Appointment.builder().id(2L).patient(patient).doctor(doctor).status(AppointmentStatus.SCHEDULED).build();
        Appointment appointment3 = Appointment.builder().id(3L).patient(patient).doctor(doctor).status(AppointmentStatus.SCHEDULED).build();

        given(appointmentRepository.findByDoctorIdOrderByDateDesc(doctorId))
                .willReturn(Arrays.asList(appointment1, appointment2, appointment3));

        given(chatRepository.findByRoomIdOrderByTimeAsc("appointment_1")).willReturn(List.of(room1Message));
        given(chatRepository.findByRoomIdOrderByTimeAsc("appointment_2")).willReturn(List.of(room2Message));
        given(chatRepository.findByRoomIdOrderByTimeAsc("appointment_3")).willReturn(List.of(room3Message));

        // 최신 메시지 조회 모킹 (성능 최적화)
        given(chatRepository.findTopByRoomIdOrderByTimeDesc("appointment_1")).willReturn(List.of(room1Message));
        given(chatRepository.findTopByRoomIdOrderByTimeDesc("appointment_2")).willReturn(List.of(room2Message));
        given(chatRepository.findTopByRoomIdOrderByTimeDesc("appointment_3")).willReturn(List.of(room3Message));

        // When
        List<String> sortedRoomIds = chatService.getChatRoomsSortedByLatestMessage(doctorId);

        // Then
        assertThat(sortedRoomIds).hasSize(3);
        assertThat(sortedRoomIds.get(0)).isEqualTo("appointment_2");  // 가장 최근
        assertThat(sortedRoomIds.get(1)).isEqualTo("appointment_1");  // 5분 전
        assertThat(sortedRoomIds.get(2)).isEqualTo("appointment_3");  // 10분 전
    }

    @Test
    @DisplayName("특정 채팅방의 읽지 않은 메시지 개수 (COUNT 최적화)")
    void getUnreadMessageCount() {
        // Given
        // roomId는 setUp에서 "appointment_1"로 설정됨
        Long userId = 21L;  // doctor ID

        // COUNT 쿼리로 직접 개수 반환
        given(chatRepository.countUnreadMessages(roomId, userId))
                .willReturn(2);

        // When
        int count = chatService.getUnreadMessageCount(roomId, userId);

        // Then
        assertThat(count).isEqualTo(2);
        verify(chatRepository, times(1)).countUnreadMessages(roomId, userId);
    }

    @Test
    @DisplayName("의사의 모든 채팅방 읽지 않은 메시지 총합 (appointment 기반)")
    void getTotalUnreadMessageCountForDoctor() {
        // Given
        Long doctorId = 21L;

        // Appointment 객체 생성
        Appointment appointment1 = Appointment.builder()
                .id(1L)
                .patient(patient)
                .doctor(doctor)
                .date(LocalDateTime.now())
                .status(AppointmentStatus.SCHEDULED) // PENDING -> SCHEDULED 수정
                .build();

        Appointment appointment2 = Appointment.builder()
                .id(2L)
                .patient(patient)
                .doctor(doctor)
                .date(LocalDateTime.now())
                .status(AppointmentStatus.SCHEDULED) // PENDING -> SCHEDULED 수정
                .build();

        // appointmentRepository 모킹
        given(appointmentRepository.findByDoctorIdOrderByDateDesc(doctorId))
                .willReturn(Arrays.asList(appointment1, appointment2));

        // Room appointment_1 - 2개 안읽음
        ChatMessage unread1 = ChatMessage.builder()
                .id(1L)
                .roomId("appointment_1")
                .sender(patient)
                .content("메시지1")
                .isRead(false)
                .build();

        ChatMessage unread2 = ChatMessage.builder()
                .id(2L)
                .roomId("appointment_1")
                .sender(patient)
                .content("메시지2")
                .isRead(false)
                .build();

        // Room appointment_2 - 1개 안읽음
        ChatMessage unread3 = ChatMessage.builder()
                .id(3L)
                .roomId("appointment_2")
                .sender(patient)
                .content("메시지3")
                .isRead(false)
                .build();

        // 각 방에 메시지가 존재하도록 모킹
        given(chatRepository.findByRoomIdOrderByTimeAsc("appointment_1"))
                .willReturn(Arrays.asList(unread1, unread2));
        given(chatRepository.findByRoomIdOrderByTimeAsc("appointment_2"))
                .willReturn(List.of(unread3));

        // COUNT 쿼리로 최적화된 모킹
        given(chatRepository.countUnreadMessages("appointment_1", doctorId))
                .willReturn(2);
        given(chatRepository.countUnreadMessages("appointment_2", doctorId))
                .willReturn(1);

        // When
        int totalCount = chatService.getTotalUnreadMessageCountForDoctor(doctorId);

        // Then
        assertThat(totalCount).isEqualTo(3);  // 2 + 1 = 3
    }

    @Test
    @DisplayName("읽음 처리 후 카운트 감소")
    void shouldDecreaseUnreadCountAfterMarkingAsRead() {
        // Given
        // roomId는 "appointment_1"
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
        given(chatRepository.countUnreadMessages(roomId, userId))
                .willReturn(2);

        int countBefore = chatService.getUnreadMessageCount(roomId, userId);

        // When: 읽음 처리
        chatService.markAsRead(roomId, userId);

        // 읽음 처리 후: 0개
        given(chatRepository.countUnreadMessages(roomId, userId))
                .willReturn(0);

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
        // roomId = "appointment_1"
        Appointment appointment = Appointment.builder()
                .id(appointmentId)
                .patient(patient)
                .doctor(doctor)
                .status(AppointmentStatus.SCHEDULED)
                .build();

        // Eager Loading으로 Patient와 Doctor를 함께 조회
        given(appointmentRepository.findByIdWithPatientAndDoctor(appointmentId))
                .willReturn(Optional.of(appointment));

        // When
        User result = chatService.getPatientFromRoomId(roomId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(patient.getId());
        assertThat(result.getUserId()).isEqualTo("patient01");
        assertThat(result.getName()).isEqualTo("환자1");

        verify(appointmentRepository, times(1)).findByIdWithPatientAndDoctor(appointmentId);
    }

    @Test
    @DisplayName("예약 확정 시 채팅방 자동 생성")
    void createChatRoomOnAppointmentConfirmation() {
        // Given
        Long aptId = 1L;
        // When
        String createdRoomId = chatService.createChatRoomForAppointment(aptId);

        // Then
        assertThat(createdRoomId).isEqualTo("appointment_1");
    }

    @Test
    @DisplayName("채팅방 ID 형식: appointment_{appointmentId}")
    void chatRoomIdFormatShouldBeValid() {
        // Given & When
        String roomId1 = chatService.createChatRoomForAppointment(1L);
        String roomId2 = chatService.createChatRoomForAppointment(123L);
        String roomId3 = chatService.createChatRoomForAppointment(999L);

        // Then
        assertThat(roomId1).matches("^appointment_\\d+$");
        assertThat(roomId2).matches("^appointment_\\d+$");
        assertThat(roomId3).matches("^appointment_\\d+$");

        assertThat(roomId1).isEqualTo("appointment_1");
        assertThat(roomId2).isEqualTo("appointment_123");
        assertThat(roomId3).isEqualTo("appointment_999");
    }

    @Test
    @DisplayName("이미 채팅방이 있으면 중복 생성 안 함")
    void shouldNotCreateDuplicateChatRoom() {
        // Given
        Long aptId = 1L;
        String expectedRoomId = "appointment_1";

        // 해당 appointmentId로 이미 메시지가 존재하는 경우를 시뮬레이션
        ChatMessage existingMessage = ChatMessage.builder()
                .id(1L)
                .roomId(expectedRoomId)
                .sender(patient)
                .content("기존 메시지")
                .time(LocalDateTime.now())
                .build();

        given(chatRepository.findByRoomIdOrderByTimeAsc(expectedRoomId))
                .willReturn(List.of(existingMessage));

        // When
        boolean exists = chatService.chatRoomExists(aptId);

        // Then
        assertThat(exists).isTrue();
        verify(chatRepository, times(1)).findByRoomIdOrderByTimeAsc(expectedRoomId);
    }

    @Test
    @DisplayName("채팅방 ID로 예약 정보 조회 (appointment_X 형식)")
    void getAppointmentFromAppointmentRoomId() {
        // Given
        String targetRoomId = "appointment_1";
        Long targetAptId = 1L;

        Appointment appointment = Appointment.builder()
                .id(targetAptId)
                .patient(patient)
                .doctor(doctor)
                .date(LocalDateTime.of(2025, 12, 1, 14, 0))
                .status(AppointmentStatus.SCHEDULED)
                .build();

        // Eager Loading으로 Patient와 Doctor를 함께 조회
        given(appointmentRepository.findByIdWithPatientAndDoctor(targetAptId))
                .willReturn(java.util.Optional.of(appointment));

        // When
        Appointment result = chatService.getAppointmentFromRoomId(targetRoomId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(targetAptId);
        assertThat(result.getPatient()).isEqualTo(patient);
        assertThat(result.getDoctor()).isEqualTo(doctor);
        verify(appointmentRepository, times(1)).findByIdWithPatientAndDoctor(targetAptId);
    }

    @Test
    @DisplayName("예약 ID로 채팅방 조회")
    void getRoomIdByAppointmentId() {
        // Given
        Long aptId = 1L;

        // When
        String foundRoomId = chatService.getRoomIdByAppointmentId(aptId);

        // Then
        assertThat(foundRoomId).isEqualTo("appointment_1");
    }

    @Test
    @DisplayName("예약 취소 시 채팅방 상태 변경")
    void shouldReflectCancelledAppointmentStatus() {
        // Given
        String targetRoomId = "appointment_1";
        Long targetAptId = 1L;

        Appointment cancelledAppointment = Appointment.builder()
                .id(targetAptId)
                .patient(patient)
                .doctor(doctor)
                .date(LocalDateTime.of(2025, 12, 1, 14, 0))
                .status(AppointmentStatus.CANCELLED)
                .build();

        // Eager Loading으로 Patient와 Doctor를 함께 조회
        given(appointmentRepository.findByIdWithPatientAndDoctor(targetAptId))
                .willReturn(java.util.Optional.of(cancelledAppointment));

        // When
        Appointment result = chatService.getAppointmentFromRoomId(targetRoomId);

        // Then
        assertThat(result.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        verify(appointmentRepository, times(1)).findByIdWithPatientAndDoctor(targetAptId);
    }

    @Test
    @DisplayName("빈 채팅방도 목록에 포함")
    void shouldIncludeEmptyChatRooms() {
        // Given
        Long doctorId = 21L;
        Long appointmentId1 = 1L;
        Long appointmentId2 = 2L;

        Appointment appointment1 = Appointment.builder()
                .id(appointmentId1)
                .patient(patient)
                .doctor(doctor)
                .status(AppointmentStatus.SCHEDULED)
                .build();

        Appointment appointment2 = Appointment.builder()
                .id(appointmentId2)
                .patient(patient)
                .doctor(doctor)
                .status(AppointmentStatus.SCHEDULED)
                .build();

        given(appointmentRepository.findByDoctorIdOrderByDateDesc(doctorId))
                .willReturn(Arrays.asList(appointment1, appointment2));

        // When
        List<String> roomIds = chatService.getAllChatRoomsIncludingEmpty(doctorId);

        // Then
        assertThat(roomIds).hasSize(2);
        assertThat(roomIds).containsExactlyInAnyOrder("appointment_1", "appointment_2");
        verify(appointmentRepository, times(1)).findByDoctorIdOrderByDateDesc(doctorId);
    }

    @Test
    @DisplayName("Eager Loading으로 환자 이름 조회 시 Lazy Loading 에러 없음")
    void getPatientNameWithoutLazyLoadingException() {
        // Given
        String targetRoomId = "appointment_1";
        Long targetAptId = 1L;

        Appointment appointment = Appointment.builder()
                .id(targetAptId)
                .patient(patient)
                .doctor(doctor)
                .date(LocalDateTime.of(2025, 12, 13, 14, 0))
                .status(AppointmentStatus.SCHEDULED)
                .build();

        // findByIdWithPatientAndDoctor()는 JOIN FETCH로 Patient와 Doctor를 즉시 로딩
        given(appointmentRepository.findByIdWithPatientAndDoctor(targetAptId))
                .willReturn(Optional.of(appointment));

        // When
        Appointment result = chatService.getAppointmentFromRoomId(targetRoomId);

        // Then - Lazy Loading 없이 Patient와 Doctor 정보에 접근 가능
        assertThat(result).isNotNull();
        assertThat(result.getPatient()).isNotNull();
        assertThat(result.getPatient().getName()).isEqualTo("환자1");
        assertThat(result.getDoctor()).isNotNull();
        assertThat(result.getDoctor().getName()).isEqualTo("의사1");

        // Eager Loading 메서드가 호출되었는지 검증
        verify(appointmentRepository, times(1)).findByIdWithPatientAndDoctor(targetAptId);
        // 일반 findById()는 호출되지 않아야 함
        verify(appointmentRepository, never()).findById(targetAptId);
    }

    @Test
    @DisplayName("채팅방 목록 조회 시 취소된 예약은 제외됨")
    void getAllChatRoomsIncludingEmpty_ShouldExcludeCancelledAppointments() {
        // Given
        Long doctorId = 21L;

        Appointment scheduledAppointment = Appointment.builder()
                .id(1L)
                .patient(patient)
                .doctor(doctor)
                .status(AppointmentStatus.SCHEDULED)
                .build();

        Appointment cancelledAppointment = Appointment.builder()
                .id(2L)
                .patient(patient)
                .doctor(doctor)
                .status(AppointmentStatus.CANCELLED)
                .build();

        Appointment completedAppointment = Appointment.builder()
                .id(3L)
                .patient(patient)
                .doctor(doctor)
                .status(AppointmentStatus.COMPLETED)
                .build();

        given(appointmentRepository.findByDoctorIdOrderByDateDesc(doctorId))
                .willReturn(Arrays.asList(scheduledAppointment, cancelledAppointment, completedAppointment));

        // When
        List<String> roomIds = chatService.getAllChatRoomsIncludingEmpty(doctorId);

        // Then
        assertThat(roomIds).hasSize(2); // CANCELLED는 제외
        assertThat(roomIds).containsExactlyInAnyOrder("appointment_1", "appointment_3");
        verify(appointmentRepository, times(1)).findByDoctorIdOrderByDateDesc(doctorId);
    }

    @Test
    @DisplayName("IN_PROGRESS 상태의 채팅방만 필터링")
    void getAllChatRoomsIncludingEmpty_ShouldFilterByInProgress() {
        // Given
        Long doctorId = 21L;

        Appointment scheduledAppointment = Appointment.builder()
                .id(1L)
                .patient(patient)
                .doctor(doctor)
                .status(AppointmentStatus.SCHEDULED)
                .build();

        Appointment inProgressAppointment = Appointment.builder()
                .id(2L)
                .patient(patient)
                .doctor(doctor)
                .status(AppointmentStatus.IN_PROGRESS)
                .build();

        Appointment completedAppointment = Appointment.builder()
                .id(3L)
                .patient(patient)
                .doctor(doctor)
                .status(AppointmentStatus.COMPLETED)
                .build();

        given(appointmentRepository.findByDoctorIdOrderByDateDesc(doctorId))
                .willReturn(Arrays.asList(scheduledAppointment, inProgressAppointment, completedAppointment));

        // When
        List<String> allRoomIds = chatService.getAllChatRoomsIncludingEmpty(doctorId);

        // 실제 프론트엔드에서 IN_PROGRESS만 필터링하는 것을 시뮬레이션
        List<Appointment> appointments = Arrays.asList(scheduledAppointment, inProgressAppointment, completedAppointment);
        List<String> inProgressRoomIds = appointments.stream()
                .filter(apt -> apt.getStatus() == AppointmentStatus.IN_PROGRESS)
                .map(apt -> "appointment_" + apt.getId())
                .collect(java.util.stream.Collectors.toList());

        // Then
        assertThat(allRoomIds).hasSize(3);
        assertThat(inProgressRoomIds).hasSize(1);
        assertThat(inProgressRoomIds).containsExactly("appointment_2");
        verify(appointmentRepository, times(1)).findByDoctorIdOrderByDateDesc(doctorId);
    }
}