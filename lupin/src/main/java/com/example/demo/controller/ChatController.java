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
     * [N+1 문제 해결] 의사의 채팅방 목록 조회 (환자 프로필 포함)
     * GET /api/chat/rooms?userId={userId}
     *
     * 기존: 21회 API 호출 (1 + N×2 쿼리)
     * 최적화: 1회 쿼리 (JOIN FETCH 사용)
     */
    @GetMapping("/rooms")
    public ResponseEntity<List<Map<String, Object>>> getChatRooms(@RequestParam Long userId) {
        // [최적화] JOIN FETCH로 환자 정보를 함께 로드 (N+1 문제 해결)
        List<Appointment> appointments = chatService.getChatRoomsWithPatientProfiles(userId);

        List<Map<String, Object>> chatRooms = appointments.stream()
                .map(appointment -> {
                    Map<String, Object> room = new HashMap<>();
                    String roomId = "appointment_" + appointment.getId();
                    room.put("roomId", roomId);

                    // [최적화] 이미 JOIN FETCH로 로드된 환자 정보 사용 (추가 쿼리 없음)
                    room.put("patientId", appointment.getPatient().getId());
                    room.put("patientName", appointment.getPatient().getName());
                    room.put("patientAvatar", appointment.getPatient().getAvatar());
                    room.put("patientDepartment", appointment.getPatient().getDepartment());
                    room.put("doctorId", appointment.getDoctor().getId());
                    room.put("status", appointment.getStatus().toString());
                    room.put("appointmentTime", appointment.getDate().toString());

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
