package com.example.demo.service;
import com.example.demo.domain.entity.ChatMessage;
import com.example.demo.domain.entity.User;
import com.example.demo.repository.ChatRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

    private final ChatRepository chatRepository;
    private final UserRepository userRepository;

    @Transactional
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

}
