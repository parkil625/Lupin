package com.example.demo.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages", indexes = {
    @Index(name = "idx_chat_room", columnList = "roomId"),
    @Index(name = "idx_chat_room_sent", columnList = "roomId, time DESC"),
    @Index(name = "idx_chat_sender", columnList = "senderId"),
    @Index(name = "idx_chat_unread", columnList = "roomId, isRead")
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

    @Column(nullable = false, insertable = false, updatable = false)
    private Long senderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "senderId", nullable = false)
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
