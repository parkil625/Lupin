package com.example.demo.repository;

import com.example.demo.domain.entity.ChatMessage;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;


import java.util.List;

@Repository
public interface ChatRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByRoomIdOrderByTimeAsc(String roomId);

    // Eager loading을 통한 LazyInitializationException 방지
    @Query("SELECT cm FROM ChatMessage cm JOIN FETCH cm.sender WHERE cm.roomId = :roomId ORDER BY cm.time ASC")
    List<ChatMessage> findByRoomIdWithSenderEagerly(@Param("roomId") String roomId);

    // 최신 메시지 1개만 조회 (성능 최적화)
    @Query("SELECT cm FROM ChatMessage cm JOIN FETCH cm.sender WHERE cm.roomId = :roomId ORDER BY cm.time DESC")
    List<ChatMessage> findTopByRoomIdOrderByTimeDesc(@Param("roomId") String roomId);

    @Query("select cm from ChatMessage cm where cm.roomId = :roomId and cm.sender.id != :userId and cm.isRead = false ")
    List<ChatMessage> findUnreadMessages(@Param("roomId") String roomId, @Param("userId") Long userId);

    // 안 읽은 메시지 개수만 조회 (성능 최적화)
    @Query("SELECT COUNT(cm) FROM ChatMessage cm WHERE cm.roomId = :roomId AND cm.sender.id != :userId AND cm.isRead = false")
    int countUnreadMessages(@Param("roomId") String roomId, @Param("userId") Long userId);

    @Modifying(clearAutomatically = true)
    @Query("update ChatMessage cm set cm.isRead = true where cm.roomId = :roomId and cm.sender.id != :userId and cm.isRead = false")
    void markAllAsReadInRoom(@Param("roomId") String roomId, @Param("userId") Long userId);
}