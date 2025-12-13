package com.example.demo.service;
import com.example.demo.domain.entity.Appointment;
import com.example.demo.domain.entity.ChatMessage;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.AppointmentStatus;
import com.example.demo.repository.AppointmentRepository;
import com.example.demo.repository.ChatRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Propagation;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

    private final ChatRepository chatRepository;
    private final UserRepository userRepository;
    private final AppointmentRepository appointmentRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ChatMessage saveMessage(String roomId, Long senderId, String content) {
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

                ChatMessage message = ChatMessage.builder()
                        .roomId(roomId)
                        .sender(sender)
                        .content(content)
                        .time(LocalDateTime.now())
                        .isRead(false)
                        .build();

        return chatRepository.save(message);
    }

    public List<ChatMessage> getChatHistory(String roomId){
        // Eager loading을 사용하여 LazyInitializationException 방지
        return chatRepository.findByRoomIdWithSenderEagerly(roomId);
    }

    public List<ChatMessage> getUnreadHistory(String roomId, Long userId) {
        return chatRepository.findUnreadMessages(roomId, userId);
    }

    @Transactional
    public void markAsRead(String roomId, Long userId) {
        chatRepository.markAllAsReadInRoom(roomId, userId);
    }

    public List<String> getAllChatRoomsByDoctorId(Long doctorId) {
        List<Appointment> appointments = appointmentRepository.findByDoctorIdOrderByDateDesc(doctorId);
        return appointments.stream()
                .map(appointment -> "appointment_" + appointment.getId())
                .filter(roomId -> !chatRepository.findByRoomIdOrderByTimeAsc(roomId).isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }

    public ChatMessage getLatestMessageInRoom(String roomId) {
        // 최신 메시지 1개만 조회 (성능 최적화)
        List<ChatMessage> messages = chatRepository.findTopByRoomIdOrderByTimeDesc(roomId);
        if (messages.isEmpty()) {
            return null;
        }
        return messages.get(0);  // DESC 정렬이므로 첫 번째가 최신
    }

    public List<String> getChatRoomsSortedByLatestMessage(Long doctorId) {
        List<String> roomIds = getAllChatRoomsByDoctorId(doctorId);

        return roomIds.stream()
                .sorted(Comparator.comparing(
                        roomId -> {
                            ChatMessage latestMessage = getLatestMessageInRoom(roomId);
                            return latestMessage != null ? latestMessage.getTime() : LocalDateTime.MIN;
                        },
                        Comparator.reverseOrder()
                ))
                .collect(Collectors.toList());
    }

    public int getUnreadMessageCount(String roomId, Long userId) {
        // COUNT 쿼리로 성능 최적화 (전체 목록 대신 개수만 조회)
        return chatRepository.countUnreadMessages(roomId, userId);
    }

    public int getTotalUnreadMessageCountForDoctor(Long doctorId) {
        List<String> roomIds = getAllChatRoomsByDoctorId(doctorId);
        return roomIds.stream()
                .mapToInt(roomId -> getUnreadMessageCount(roomId, doctorId))
                .sum();
    }

    public User getPatientFromRoomId(String roomId) {
        Appointment appointment = getAppointmentFromRoomId(roomId);
        return appointment.getPatient();
    }

    public List<Appointment> getAppointmentsFromRoomId(String roomId) {
        Appointment appointment = getAppointmentFromRoomId(roomId);
        return List.of(appointment);
    }

    public String createChatRoomForAppointment(Long appointmentId) {
        return "appointment_" + appointmentId;
    }

    public boolean chatRoomExists(Long appointmentId) {
        String roomId = "appointment_" + appointmentId;
        List<ChatMessage> messages = chatRepository.findByRoomIdOrderByTimeAsc(roomId);
        return !messages.isEmpty();
    }

    public Appointment getAppointmentFromRoomId(String roomId) {
        if (roomId.startsWith("appointment_")) {
            Long appointmentId = Long.parseLong(roomId.substring("appointment_".length()));
            return appointmentRepository.findByIdWithPatientAndDoctor(appointmentId)
                    .orElseThrow(() -> new IllegalArgumentException("예약을 찾을 수 없습니다."));
        }
        throw new IllegalArgumentException("유효하지 않은 채팅방 ID 형식입니다.");
    }

    public String getRoomIdByAppointmentId(Long appointmentId) {
        return "appointment_" + appointmentId;
    }

    public List<String> getAllChatRoomsIncludingEmpty(Long doctorId) {
        List<Appointment> appointments = appointmentRepository.findByDoctorIdOrderByDateDesc(doctorId);
        return appointments.stream()
                .filter(appointment -> appointment.getStatus() != AppointmentStatus.CANCELLED)
                .map(appointment -> getRoomIdByAppointmentId(appointment.getId()))
                .collect(Collectors.toList());
    }

}
