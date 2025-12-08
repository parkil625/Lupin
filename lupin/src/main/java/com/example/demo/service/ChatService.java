package com.example.demo.service;

import com.example.demo.domain.entity.Appointment;
import com.example.demo.domain.entity.ChatMessage;
import com.example.demo.domain.entity.User;
import com.example.demo.repository.AppointmentRepository;
import com.example.demo.repository.ChatRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
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
        return chatRepository.findByRoomIdOrderByTimeAsc(roomId);
    }

    public List<ChatMessage> getUnreadHistory(String roomId, Long userId) {
        return chatRepository.findUnreadMessages(roomId, userId);
    }

    @Transactional
    public void markAsRead(String roomId, Long userId) {
        chatRepository.markAllAsReadInRoom(roomId, userId);
    }

    public List<String> getAllChatRoomsByDoctorId(Long doctorId) {
        return chatRepository.findAll().stream()
                .map(ChatMessage::getRoomId)
                .filter(roomId -> roomId.endsWith(":" + doctorId))
                .distinct()
                .collect(Collectors.toList());
    }

    public ChatMessage getLatestMessageInRoom(String roomId) {
        List<ChatMessage> messages = chatRepository.findByRoomIdOrderByTimeAsc(roomId);
        if (messages.isEmpty()) {
            return null;
        }
        return messages.get(messages.size() - 1);
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
        return chatRepository.findUnreadMessages(roomId, userId).size();
    }

    public int getTotalUnreadMessageCountForDoctor(Long doctorId) {
        List<String> roomIds = getAllChatRoomsByDoctorId(doctorId);
        return roomIds.stream()
                .mapToInt(roomId -> getUnreadMessageCount(roomId, doctorId))
                .sum();
    }

    public User getPatientFromRoomId(String roomId) {
        String[] parts = roomId.split(":");
        Long patientId = Long.parseLong(parts[0]);
        return userRepository.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 환자입니다."));
    }

    public List<Appointment> getAppointmentsFromRoomId(String roomId) {
        String[] parts = roomId.split(":");
        Long patientId = Long.parseLong(parts[0]);
        return appointmentRepository.findByPatientIdOrderByDateDesc(patientId);
    }

    // --- 여기부터가 아까 누락되어 에러를 일으켰던 메서드들입니다 ---

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
            return appointmentRepository.findById(appointmentId)
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
                .map(appointment -> getRoomIdByAppointmentId(appointment.getId()))
                .collect(Collectors.toList());
    }
}