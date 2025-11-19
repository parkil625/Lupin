package com.example.demo.controller;

import com.example.demo.dto.request.ChatMessageCreateRequest;
import com.example.demo.dto.response.ChatMessageResponse;
import com.example.demo.dto.response.ChatRoomResponse;
import com.example.demo.service.ChatMessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 채팅 메시지 관련 API
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatMessageController {

    private final ChatMessageService chatMessageService;

    /**
     * 채팅 메시지 전송
     */
    @PostMapping("/messages")
    public ResponseEntity<ChatMessageResponse> sendMessage(
            @Valid @RequestBody ChatMessageCreateRequest request) {
        ChatMessageResponse response = chatMessageService.sendMessage(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 특정 채팅방의 메시지 목록 조회 (페이징)
     */
    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<Page<ChatMessageResponse>> getMessagesByRoomId(
            @PathVariable String roomId,
            @PageableDefault(size = 50, sort = "sentAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ChatMessageResponse> messages = chatMessageService.getMessagesByRoomId(roomId, pageable);
        return ResponseEntity.ok(messages);
    }

    /**
     * 특정 채팅방의 메시지 목록 조회 (전체)
     */
    @GetMapping("/rooms/{roomId}/messages/all")
    public ResponseEntity<List<ChatMessageResponse>> getAllMessagesByRoomId(@PathVariable String roomId) {
        List<ChatMessageResponse> messages = chatMessageService.getAllMessagesByRoomId(roomId);
        return ResponseEntity.ok(messages);
    }

    /**
     * 환자와 의사 간의 메시지 조회
     */
    @GetMapping("/messages/between")
    public ResponseEntity<List<ChatMessageResponse>> getMessagesBetweenUsers(
            @RequestParam Long patientId,
            @RequestParam Long doctorId) {
        List<ChatMessageResponse> messages = chatMessageService.getMessagesBetweenUsers(patientId, doctorId);
        return ResponseEntity.ok(messages);
    }

    /**
     * 특정 채팅방의 읽지 않은 메시지 수 조회
     */
    @GetMapping("/rooms/{roomId}/unread-count")
    public ResponseEntity<Long> getUnreadMessageCount(
            @PathVariable String roomId,
            @RequestParam Long userId) {
        Long count = chatMessageService.getUnreadMessageCount(roomId, userId);
        return ResponseEntity.ok(count);
    }

    /**
     * 특정 채팅방의 메시지 전체 읽음 처리
     */
    @PatchMapping("/rooms/{roomId}/read-all")
    public ResponseEntity<Void> markAllAsRead(
            @PathVariable String roomId,
            @RequestParam Long userId) {
        chatMessageService.markAllAsRead(roomId, userId);
        return ResponseEntity.ok().build();
    }

    /**
     * 특정 사용자가 참여한 채팅방 목록 조회
     */
    @GetMapping("/rooms/users/{userId}")
    public ResponseEntity<List<ChatRoomResponse>> getChatRoomsByUserId(@PathVariable Long userId) {
        List<ChatRoomResponse> chatRooms = chatMessageService.getChatRoomsByUserId(userId);
        return ResponseEntity.ok(chatRooms);
    }

    /**
     * 메시지 상세 조회
     */
    @GetMapping("/messages/{messageId}")
    public ResponseEntity<ChatMessageResponse> getMessageDetail(@PathVariable Long messageId) {
        ChatMessageResponse message = chatMessageService.getMessageDetail(messageId);
        return ResponseEntity.ok(message);
    }

    /**
     * 메시지 삭제
     */
    @DeleteMapping("/messages/{messageId}")
    public ResponseEntity<Void> deleteMessage(
            @PathVariable Long messageId,
            @RequestParam Long userId) {
        chatMessageService.deleteMessage(messageId, userId);
        return ResponseEntity.noContent().build();
    }
}
