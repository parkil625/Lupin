package com.example.demo.repository;

import com.example.demo.domain.entity.Notification;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.NotificationType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // [최적화] Slice로 변경 - count 쿼리 제거
    Slice<Notification> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    // 기존 List 반환 메서드 유지 (하위 호환)
    List<Notification> findByUserOrderByCreatedAtDescIdDesc(User user);

    // [최적화] 존재 확인
    boolean existsByUserAndIsReadFalse(User user);

    List<Notification> findByUserAndIsReadFalse(User user);

    // [최적화] 전체 읽음 처리 - 벌크 업데이트
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user = :user AND n.isRead = false")
    int markAllAsRead(@Param("user") User user);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.refId = :refId AND n.type IN :types")
    void deleteByRefIdAndTypeIn(@Param("refId") String refId, @Param("types") List<NotificationType> types);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.refId = :refId AND n.type = :type")
    void deleteByRefIdAndType(@Param("refId") String refId, @Param("type") NotificationType type);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.refId IN :refIds AND n.type = :type")
    void deleteByRefIdInAndType(@Param("refIds") List<String> refIds, @Param("type") NotificationType type);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.createdAt < :cutoffDate")
    int deleteByCreatedAtBefore(@Param("cutoffDate") LocalDateTime cutoffDate);

    // [Last-Event-ID] 특정 ID 이후의 알림 조회 (재연결 시 사용)
    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId AND n.id > :lastEventId ORDER BY n.id ASC")
    List<Notification> findByUserIdAndIdGreaterThan(@Param("userId") Long userId, @Param("lastEventId") Long lastEventId);
}
