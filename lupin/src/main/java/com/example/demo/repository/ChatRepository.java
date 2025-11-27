package com.example.demo.repository;

import com.example.demo.domain.entity.ChatMessage;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatRepository extends JpaRepository<ChatMessage, Long> {

    /*
        메시지 가져오기 (기본)
     */
    List<ChatMessage> findByRoomIdOrderByTimeAsc(String roomId);

    /*
        메시지 페이징으로 가져오기
     */
    Slice<ChatMessage> findByRoomIdOrderByTimeDesc(String roomId, Pageable Pageable );

    /*
        안 읽은 대화 가져오기
     */
    @Query("SELECT m FROM ChatMessage m WHERE m.roomId = :roomId AND m.isRead = false" +
            " AND m.sender.id != :userId ORDER BY m.time ASC")
    List<ChatMessage> findUnreadMessages(@Param("roomId") String roomId, @Param("userId") Long userId);

    /*
        메시지 일괄 읽음 처리
     */
    @Modifying
    @Query("update ChatMessage m set m.isRead = true where m.roomId = :roomId and m.sender.id != :userId and m.isRead = false")
    int markAllAsReadInRoom(@Param("roomId") String roomId, @Param("userId") Long userId);



}