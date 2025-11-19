package com.example.demo.service;

import com.example.demo.domain.entity.ChatMessage;
import com.example.demo.domain.entity.User;
import com.example.demo.dto.request.ChatMessageCreateRequest;
import com.example.demo.dto.response.ChatMessageResponse;
import com.example.demo.dto.response.ChatRoomResponse;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.ChatMessageRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 채팅 메시지 관련 비즈니스 로직을 처리하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;

    /**
     * 채팅 메시지 전송
     */
    @Transactional
    public ChatMessageResponse sendMessage(ChatMessageCreateRequest request) {
        User sender = findUserById(request.getSenderId());
        User patient = findUserById(request.getPatientId());
        User doctor = findUserById(request.getDoctorId());

        // roomId 생성 (patient_id:doctor_id)
        String roomId = ChatMessage.generateRoomId(request.getPatientId(), request.getDoctorId());

        ChatMessage chatMessage = ChatMessage.builder()
                .roomId(roomId)
                .content(request.getContent())
                .sender(sender)
                .build();

        ChatMessage savedMessage = chatMessageRepository.save(chatMessage);

        log.info("채팅 메시지 전송 완료 - messageId: {}, roomId: {}, senderId: {}",
                savedMessage.getId(), roomId, request.getSenderId());

        return ChatMessageResponse.from(savedMessage);
    }

    /**
     * 특정 채팅방의 메시지 목록 조회 (페이징)
     */
    public Page<ChatMessageResponse> getMessagesByRoomId(String roomId, Pageable pageable) {
        return chatMessageRepository.findByRoomId(roomId, pageable)
                .map(ChatMessageResponse::from);
    }

    /**
     * 특정 채팅방의 메시지 목록 조회 (전체)
     */
    public List<ChatMessageResponse> getAllMessagesByRoomId(String roomId) {
        return chatMessageRepository.findByRoomId(roomId)
                .stream()
                .map(ChatMessageResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 특정 채팅방의 메시지 목록 조회 (환자 ID와 의사 ID로)
     */
    public List<ChatMessageResponse> getMessagesBetweenUsers(Long patientId, Long doctorId) {
        String roomId = ChatMessage.generateRoomId(patientId, doctorId);
        return getAllMessagesByRoomId(roomId);
    }

    /**
     * 특정 채팅방의 읽지 않은 메시지 수 조회
     */
    public Long getUnreadMessageCount(String roomId, Long userId) {
        return chatMessageRepository.countUnreadMessagesByRoomId(roomId, userId);
    }

    /**
     * 특정 채팅방의 메시지 전체 읽음 처리
     */
    @Transactional
    public void markAllAsRead(String roomId, Long userId) {
        chatMessageRepository.markAllAsReadByRoomId(roomId, userId);

        log.info("채팅방 메시지 읽음 처리 완료 - roomId: {}, userId: {}", roomId, userId);
    }

    /**
     * 특정 사용자가 참여한 채팅방 목록 조회
     */
    public List<ChatRoomResponse> getChatRoomsByUserId(Long userId) {
        List<String> roomIds = chatMessageRepository.findRoomIdsByUserId(userId);
        List<ChatRoomResponse> chatRooms = new ArrayList<>();

        for (String roomId : roomIds) {
            // roomId 파싱 (patient_id:doctor_id)
            String[] ids = roomId.split(":");
            if (ids.length != 2) continue;

            try {
                Long patientId = Long.parseLong(ids[0]);
                Long doctorId = Long.parseLong(ids[1]);

                User patient = findUserById(patientId);
                User doctor = findUserById(doctorId);

                // 최신 메시지 조회
                List<ChatMessage> latestMessages = chatMessageRepository.findLatestMessageByRoomId(
                        roomId, PageRequest.of(0, 1));

                ChatMessage latestMessage = latestMessages.isEmpty() ? null : latestMessages.get(0);

                // 읽지 않은 메시지 수 조회
                Long unreadCount = chatMessageRepository.countUnreadMessagesByRoomId(roomId, userId);

                ChatRoomResponse chatRoom = ChatRoomResponse.builder()
                        .roomId(roomId)
                        .patientId(patientId)
                        .patientName(patient.getName())
                        .patientProfileImage(patient.getProfileImage())
                        .doctorId(doctorId)
                        .doctorName(doctor.getName())
                        .doctorProfileImage(doctor.getProfileImage())
                        .lastMessage(latestMessage != null ? latestMessage.getContent() : null)
                        .lastMessageTime(latestMessage != null ? latestMessage.getSentAt() : null)
                        .unreadCount(unreadCount)
                        .build();

                chatRooms.add(chatRoom);
            } catch (NumberFormatException e) {
                log.warn("잘못된 roomId 형식 - roomId: {}", roomId);
            }
        }

        return chatRooms;
    }

    /**
     * 메시지 상세 조회
     */
    public ChatMessageResponse getMessageDetail(Long messageId) {
        ChatMessage message = findMessageById(messageId);
        return ChatMessageResponse.from(message);
    }

    /**
     * 메시지 삭제
     */
    @Transactional
    public void deleteMessage(Long messageId, Long userId) {
        ChatMessage message = findMessageById(messageId);

        // 발신자 확인
        if (!message.getSender().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "메시지를 삭제할 권한이 없습니다.");
        }

        chatMessageRepository.delete(message);

        log.info("채팅 메시지 삭제 완료 - messageId: {}, userId: {}", messageId, userId);
    }

    // === 헬퍼 메서드 ===

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private ChatMessage findMessageById(Long messageId) {
        return chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_MESSAGE_NOT_FOUND));
    }
}
