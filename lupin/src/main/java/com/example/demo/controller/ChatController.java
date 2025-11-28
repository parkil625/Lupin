package com.example.demo.controller;

import com.example.demo.dto.response.ChatMessageResponse;
import com.example.demo.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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

    @GetMapping("/history/{roomId}")
    public ResponseEntity<List<ChatMessageResponse>> getChatHistory(@PathVariable String roomId) {
        
        List<ChatMessageResponse> messages = chatService.getChatHistory(roomId)
                .stream()
                .map(ChatMessageResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(messages);
    }

}
