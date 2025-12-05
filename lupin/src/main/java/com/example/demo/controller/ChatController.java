package com.example.demo.controller;

import com.example.demo.domain.entity.Appointment;
import com.example.demo.domain.entity.ChatMessage;
import com.example.demo.dto.response.ChatMessageResponse;
import com.example.demo.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 채팅 REST API 컨트롤러
 * 채팅 기록 조회, 읽음 처리 등
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    /**
     * 채팅 기록 조회
     * GET /api/chat/history/{roomId}
     */
    @GetMapping("/history/{roomId}")
    public ResponseEntity<List<ChatMessageResponse>> getChatHistory(@PathVariable String roomId) {

        List<ChatMessageResponse> messages = chatService.getChatHistory(roomId)
                .stream()
                .map(ChatMessageResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(messages);
    }

    /**
     * 읽음 처리
     * PUT /api/chat/rooms/{roomId}/read?userId={userId}
     */
    @PutMapping("/rooms/{roomId}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable String roomId,
            @RequestParam Long userId
    ) {
        chatService.markAsRead(roomId, userId);
        return ResponseEntity.ok().build();
    }

    /**
     * 의사의 채팅방 목록 조회
     * GET /api/chat/rooms?userId={userId}
     */
    @GetMapping("/rooms")
    public ResponseEntity<List<Map<String, Object>>> getChatRooms(@RequestParam Long userId) {
        List<String> roomIds = chatService.getAllChatRoomsIncludingEmpty(userId);

        List<Map<String, Object>> chatRooms = roomIds.stream()
                .map(roomId -> {
                    Map<String, Object> room = new HashMap<>();
                    room.put("roomId", roomId);

                    // 예약 정보에서 환자 정보 가져오기
                    try {
                        Appointment appointment = chatService.getAppointmentFromRoomId(roomId);
                        room.put("patientId", appointment.getPatient().getId());
                        room.put("patientName", appointment.getPatient().getName());
                        room.put("doctorId", appointment.getDoctor().getId());
                    } catch (Exception e) {
                        log.warn("채팅방 {}에 대한 예약 정보를 찾을 수 없습니다", roomId);
                        room.put("patientId", 0);
                        room.put("patientName", "알 수 없음");
                        room.put("doctorId", userId);
                    }

                    // 마지막 메시지
                    ChatMessage lastMessage = chatService.getLatestMessageInRoom(roomId);
                    room.put("lastMessage", lastMessage != null ? lastMessage.getContent() : "");
                    room.put("lastMessageTime", lastMessage != null ? lastMessage.getTime().toString() : "");

                    // 읽지 않은 메시지 개수
                    int unreadCount = chatService.getUnreadMessageCount(roomId, userId);
                    room.put("unreadCount", unreadCount);

                    return room;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(chatRooms);
    }

}
