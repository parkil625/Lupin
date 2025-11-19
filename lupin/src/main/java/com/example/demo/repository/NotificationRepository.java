package com.example.demo.repository;

import com.example.demo.domain.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * 특정 사용자의 알림 목록 조회 (페이징)
     */
    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId ORDER BY n.createdAt DESC")
    Page<Notification> findByUserId(@Param("userId") Long userId, Pageable pageable);

    /**
     * 특정 사용자의 알림 목록 조회 (전체)
     */
    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId ORDER BY n.createdAt DESC")
    List<Notification> findByUserId(@Param("userId") Long userId);

    /**
     * 특정 사용자의 읽지 않은 알림 조회
     */
    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId AND n.isRead = 'N' ORDER BY n.createdAt DESC")
    List<Notification> findUnreadNotificationsByUserId(@Param("userId") Long userId);

    /**
     * 특정 사용자의 읽지 않은 알림 수 조회
     */
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.user.id = :userId AND n.isRead = 'N'")
    Long countUnreadByUserId(@Param("userId") Long userId);

    /**
     * 특정 사용자의 알림 전체 읽음 처리
     */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = 'Y' WHERE n.user.id = :userId AND n.isRead = 'N'")
    void markAllAsReadByUserId(@Param("userId") Long userId);

    /**
     * 특정 타입의 알림 조회
     */
    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId AND n.type = :type ORDER BY n.createdAt DESC")
    Page<Notification> findByUserIdAndType(@Param("userId") Long userId, @Param("type") String type, Pageable pageable);

    /**
     * 특정 사용자의 오래된 읽은 알림 삭제 (특정 날짜 이전)
     */
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.user.id = :userId AND n.isRead = 'Y' AND n.createdAt < :beforeDate")
    void deleteOldReadNotificationsByUserId(@Param("userId") Long userId, @Param("beforeDate") LocalDateTime beforeDate);

    /**
     * 특정 날짜 이전의 모든 알림 삭제 (15일 이상 된 알림 자동 삭제용)
     */
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.createdAt < :beforeDate")
    int deleteByCreatedAtBefore(@Param("beforeDate") LocalDateTime beforeDate);
}
