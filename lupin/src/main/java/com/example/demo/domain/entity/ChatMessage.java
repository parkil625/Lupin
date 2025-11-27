package com.example.demo.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages", indexes = {
    @Index(name = "idx_chat_room", columnList = "room_id"),
    @Index(name = "idx_chat_room_sent", columnList = "room_id, time DESC"),
    @Index(name = "idx_chat_sender", columnList = "sender_id"),
    @Index(name = "idx_chat_unread", columnList = "room_id, is_read")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id", nullable = false, length = 100)
    private String roomId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "time", nullable = false)
    @Builder.Default
    private LocalDateTime time = LocalDateTime.now();

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    public void markAsRead() {
        this.isRead = true;
    }

    public static String generateRoomId(Long patientId, Long doctorId) {
        return patientId + ":" + doctorId;
    }
}
