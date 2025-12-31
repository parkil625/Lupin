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

    // [최적화] Slice로 변경 - count 쿼리 제거 (userId만 사용하여 detached entity 문제 방지)
    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId ORDER BY n.createdAt DESC")
    Slice<Notification> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);

    // [N+1 해결] JOIN FETCH n.user 추가 - 알림 조회 시 사용자 정보까지 한방 쿼리로 로딩
    @Query("SELECT n FROM Notification n JOIN FETCH n.user WHERE n.user.id = :userId ORDER BY n.createdAt DESC, n.id DESC")
    List<Notification> findByUserIdOrderByCreatedAtDescIdDesc(@Param("userId") Long userId);

    // [최적화] 존재 확인 (userId만 사용하여 detached entity 문제 방지)
    @Query("SELECT COUNT(n) > 0 FROM Notification n WHERE n.user.id = :userId AND n.isRead = false")
    boolean existsByUserIdAndIsReadFalse(@Param("userId") Long userId);

    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId AND n.isRead = false")
    List<Notification> findByUserIdAndIsReadFalse(@Param("userId") Long userId);

    // [최적화] 전체 읽음 처리 - 벌크 업데이트 (userId만 사용하여 detached entity 문제 방지)
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.id = :userId AND n.isRead = false")
    int markAllAsReadByUserId(@Param("userId") Long userId);

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

    // [추가] targetId와 type으로 알림 삭제 (특정 댓글/대댓글 삭제 시 해당 알림만 제거)
    // 예: 댓글 A 삭제 -> "00님이 댓글 A를 남겼습니다" 알림 삭제
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.targetId = :targetId AND n.type = :type")
    void deleteByTargetIdAndType(@Param("targetId") String targetId, @Param("type") NotificationType type);

    // 특정 유저, 타입, refId로 알림 존재 여부 확인 (중복 알림 방지)
    @Query("SELECT COUNT(n) > 0 FROM Notification n WHERE n.user.id = :userId AND n.type = :type AND n.refId = :refId")
    boolean existsByUserIdAndTypeAndRefId(@Param("userId") Long userId, @Param("type") NotificationType type, @Param("refId") String refId);

    // [추가] 알림 뭉치기용: 특정 타겟(Feed 등)에 대해 읽지 않은 최신 알림 1건 조회
    // UserId(Long) 대신 User 객체로 조회하여 매핑 정확도 향상
    java.util.Optional<Notification> findTopByUserAndTypeAndRefIdAndIsReadFalseOrderByCreatedAtDesc(
            User user,
            NotificationType type,
            String refId
    );
}
