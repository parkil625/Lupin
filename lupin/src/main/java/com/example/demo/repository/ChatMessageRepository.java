package com.example.demo.repository;

import com.example.demo.domain.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /**
     * 특정 채팅방의 메시지 조회 (페이징)
     */
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.roomId = :roomId ORDER BY cm.sentAt DESC")
    Page<ChatMessage> findByRoomId(@Param("roomId") String roomId, Pageable pageable);

    /**
     * 특정 채팅방의 메시지 조회 (전체)
     */
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.roomId = :roomId ORDER BY cm.sentAt ASC")
    List<ChatMessage> findByRoomId(@Param("roomId") String roomId);

    /**
     * 특정 채팅방의 읽지 않은 메시지 수 조회
     */
    @Query("SELECT COUNT(cm) FROM ChatMessage cm WHERE cm.roomId = :roomId AND cm.isRead = 'N' AND cm.sender.id != :userId")
    Long countUnreadMessagesByRoomId(@Param("roomId") String roomId, @Param("userId") Long userId);

    /**
     * 특정 채팅방의 메시지 전체 읽음 처리
     */
    @Modifying
    @Query("UPDATE ChatMessage cm SET cm.isRead = 'Y' WHERE cm.roomId = :roomId AND cm.sender.id != :userId AND cm.isRead = 'N'")
    void markAllAsReadByRoomId(@Param("roomId") String roomId, @Param("userId") Long userId);

    /**
     * 특정 사용자가 참여한 채팅방 목록 조회
     */
    @Query("SELECT DISTINCT cm.roomId FROM ChatMessage cm WHERE cm.sender.id = :userId OR cm.roomId LIKE CONCAT('%', :userId, '%') ORDER BY cm.sentAt DESC")
    List<String> findRoomIdsByUserId(@Param("userId") Long userId);

    /**
     * 특정 채팅방의 최신 메시지 조회
     */
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.roomId = :roomId ORDER BY cm.sentAt DESC")
    List<ChatMessage> findLatestMessageByRoomId(@Param("roomId") String roomId, Pageable pageable);

    /**
     * 특정 사용자가 보낸 메시지 조회
     */
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.sender.id = :senderId ORDER BY cm.sentAt DESC")
    Page<ChatMessage> findBySenderId(@Param("senderId") Long senderId, Pageable pageable);
}
