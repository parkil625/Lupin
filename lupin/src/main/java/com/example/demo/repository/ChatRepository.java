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

    @Query("select cm from ChatMessage cm where cm.roomId = :roomId and cm.sender.id != :userId and cm.isRead = false ")
    List<ChatMessage> findUnreadMessages(@Param("roomId") String roomId, @Param("userId") Long userId);

    @Modifying(clearAutomatically = true)
    @Query("update ChatMessage cm set cm.isRead = true where cm.roomId = :roomId and cm.sender.id != :userId and cm.isRead = false")
    void markAllAsReadInRoom(@Param("roomId") String roomId, @Param("userId") Long userId);
}