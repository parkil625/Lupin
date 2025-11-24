package com.example.demo.service;

import com.example.demo.domain.entity.ChatMessage;
import com.example.demo.domain.entity.User;
import com.example.demo.dto.request.ChatMessageCreateRequest;
import com.example.demo.dto.response.ChatMessageResponse;
import com.example.demo.dto.response.ChatRoomResponse;
import com.example.demo.exception.BusinessException;
import com.example.demo.repository.ChatMessageRepository;
import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatMessageService 테스트")
class ChatMessageServiceTest {

    @InjectMocks
    private ChatMessageService chatMessageService;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private UserRepository userRepository;

    @Test
    @DisplayName("메시지 전송 성공")
    void sendMessage_Success() {
        // given
        User sender = User.builder().id(1L).userId("sender").realName("발신자").build();
        User patient = User.builder().id(2L).userId("patient").realName("환자").build();
        User doctor = User.builder().id(3L).userId("doctor").realName("의사").build();

        ChatMessageCreateRequest request = ChatMessageCreateRequest.builder()
                .senderId(1L)
                .patientId(2L)
                .doctorId(3L)
                .content("테스트 메시지")
                .build();

        given(userRepository.findById(1L)).willReturn(Optional.of(sender));
        given(userRepository.findById(2L)).willReturn(Optional.of(patient));
        given(userRepository.findById(3L)).willReturn(Optional.of(doctor));
        given(chatMessageRepository.save(any(ChatMessage.class))).willAnswer(invocation -> {
            ChatMessage msg = invocation.getArgument(0);
            return ChatMessage.builder()
                    .id(1L)
                    .roomId(msg.getRoomId())
                    .content(msg.getContent())
                    .sender(msg.getSender())
                    .build();
        });

        // when
        ChatMessageResponse result = chatMessageService.sendMessage(request);

        // then
        assertThat(result).isNotNull();
        then(chatMessageRepository).should().save(any(ChatMessage.class));
    }

    @Test
    @DisplayName("메시지 전송 실패 - 발신자 없음")
    void sendMessage_SenderNotFound() {
        // given
        ChatMessageCreateRequest request = ChatMessageCreateRequest.builder()
                .senderId(1L)
                .patientId(2L)
                .doctorId(3L)
                .content("테스트")
                .build();

        given(userRepository.findById(1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> chatMessageService.sendMessage(request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("채팅방 메시지 목록 조회 - 페이징")
    void getMessagesByRoomId_Paged_Success() {
        // given
        User sender = User.builder().id(1L).userId("sender").realName("발신자").build();
        ChatMessage message = ChatMessage.builder()
                .id(1L)
                .roomId("1:2")
                .content("테스트")
                .sender(sender)
                .build();
        Page<ChatMessage> page = new PageImpl<>(Arrays.asList(message));
        Pageable pageable = PageRequest.of(0, 10);

        given(chatMessageRepository.findByRoomId("1:2", pageable)).willReturn(page);

        // when
        Page<ChatMessageResponse> result = chatMessageService.getMessagesByRoomId("1:2", pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("채팅방 메시지 목록 조회 - 전체")
    void getAllMessagesByRoomId_Success() {
        // given
        User sender = User.builder().id(1L).userId("sender").realName("발신자").build();
        ChatMessage message = ChatMessage.builder()
                .id(1L)
                .roomId("1:2")
                .content("테스트")
                .sender(sender)
                .build();

        given(chatMessageRepository.findByRoomId("1:2")).willReturn(Arrays.asList(message));

        // when
        List<ChatMessageResponse> result = chatMessageService.getAllMessagesByRoomId("1:2");

        // then
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("사용자 간 메시지 조회")
    void getMessagesBetweenUsers_Success() {
        // given
        User sender = User.builder().id(1L).userId("sender").realName("발신자").build();
        ChatMessage message = ChatMessage.builder()
                .id(1L)
                .roomId("1:2")
                .content("테스트")
                .sender(sender)
                .build();

        given(chatMessageRepository.findByRoomId("1:2")).willReturn(Arrays.asList(message));

        // when
        List<ChatMessageResponse> result = chatMessageService.getMessagesBetweenUsers(1L, 2L);

        // then
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("읽지 않은 메시지 수 조회")
    void getUnreadMessageCount_Success() {
        // given
        given(chatMessageRepository.countUnreadMessagesByRoomId("1:2", 1L)).willReturn(5L);

        // when
        Long result = chatMessageService.getUnreadMessageCount("1:2", 1L);

        // then
        assertThat(result).isEqualTo(5L);
    }

    @Test
    @DisplayName("메시지 전체 읽음 처리")
    void markAllAsRead_Success() {
        // when
        chatMessageService.markAllAsRead("1:2", 1L);

        // then
        then(chatMessageRepository).should().markAllAsReadByRoomId("1:2", 1L);
    }

    @Test
    @DisplayName("사용자 채팅방 목록 조회")
    void getChatRoomsByUserId_Success() {
        // given
        User patient = User.builder().id(1L).userId("patient").realName("환자").build();
        User doctor = User.builder().id(2L).userId("doctor").realName("의사").build();

        given(chatMessageRepository.findRoomIdsByUserId(1L)).willReturn(Arrays.asList("1:2"));
        given(userRepository.findById(1L)).willReturn(Optional.of(patient));
        given(userRepository.findById(2L)).willReturn(Optional.of(doctor));
        given(chatMessageRepository.findLatestMessageByRoomId(eq("1:2"), any(Pageable.class)))
                .willReturn(Collections.emptyList());
        given(chatMessageRepository.countUnreadMessagesByRoomId("1:2", 1L)).willReturn(0L);

        // when
        List<ChatRoomResponse> result = chatMessageService.getChatRoomsByUserId(1L);

        // then
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("메시지 상세 조회 성공")
    void getMessageDetail_Success() {
        // given
        User sender = User.builder().id(1L).userId("sender").realName("발신자").build();
        ChatMessage message = ChatMessage.builder()
                .id(1L)
                .roomId("1:2")
                .content("테스트")
                .sender(sender)
                .build();

        given(chatMessageRepository.findById(1L)).willReturn(Optional.of(message));

        // when
        ChatMessageResponse result = chatMessageService.getMessageDetail(1L);

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("메시지 상세 조회 실패 - 메시지 없음")
    void getMessageDetail_NotFound() {
        // given
        given(chatMessageRepository.findById(1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> chatMessageService.getMessageDetail(1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("메시지 삭제 성공")
    void deleteMessage_Success() {
        // given
        User sender = User.builder().id(1L).userId("sender").build();
        ChatMessage message = ChatMessage.builder()
                .id(1L)
                .sender(sender)
                .build();

        given(chatMessageRepository.findById(1L)).willReturn(Optional.of(message));

        // when
        chatMessageService.deleteMessage(1L, 1L);

        // then
        then(chatMessageRepository).should().delete(message);
    }

    @Test
    @DisplayName("메시지 삭제 실패 - 권한 없음")
    void deleteMessage_Forbidden() {
        // given
        User sender = User.builder().id(1L).userId("sender").build();
        ChatMessage message = ChatMessage.builder()
                .id(1L)
                .sender(sender)
                .build();

        given(chatMessageRepository.findById(1L)).willReturn(Optional.of(message));

        // when & then
        assertThatThrownBy(() -> chatMessageService.deleteMessage(1L, 2L))
                .isInstanceOf(BusinessException.class);
    }
}
