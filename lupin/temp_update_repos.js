const fs = require('fs');

// 1. FeedRepository - @EntityGraph에서 images 제거 (BatchSize가 처리)
const feedRepoPath = 'c:/Lupin/lupin/src/main/java/com/example/demo/repository/FeedRepository.java';
const feedRepoContent = `package com.example.demo.repository;

import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface FeedRepository extends JpaRepository<Feed, Long> {

    // [최적화] writer만 JOIN FETCH, images는 BatchSize(100)로 처리
    @Query("SELECT f FROM Feed f JOIN FETCH f.writer WHERE f.writer = :writer ORDER BY f.id DESC")
    Slice<Feed> findByWriterOrderByIdDesc(@Param("writer") User writer, Pageable pageable);

    // [최적화] 홈 피드 - 본인 제외
    @Query("SELECT f FROM Feed f JOIN FETCH f.writer WHERE f.writer <> :writer ORDER BY f.id DESC")
    Slice<Feed> findByWriterNotOrderByIdDesc(@Param("writer") User writer, Pageable pageable);

    // [최적화] 이름 검색
    @Query("SELECT f FROM Feed f JOIN FETCH f.writer WHERE f.writer.name LIKE %:name% ORDER BY f.id DESC")
    Slice<Feed> findByWriterNameContainingOrderByIdDesc(@Param("name") String name, Pageable pageable);

    // [최적화] 오늘 글 존재 여부 - exists 사용
    boolean existsByWriterAndCreatedAtBetween(User writer, LocalDateTime start, LocalDateTime end);

    // [최적화] 상세 조회 - writer만 페치
    @Query("SELECT f FROM Feed f JOIN FETCH f.writer WHERE f.id = :id")
    Optional<Feed> findByIdWithWriter(@Param("id") Long id);

    // 기본 findById는 JpaRepository에서 제공
}
`;
fs.writeFileSync(feedRepoPath, feedRepoContent);
console.log('FeedRepository updated!');

// 2. FeedLikeRepository - @Modifying 벌크 삭제 추가
const feedLikeRepoPath = 'c:/Lupin/lupin/src/main/java/com/example/demo/repository/FeedLikeRepository.java';
const feedLikeRepoContent = `package com.example.demo.repository;

import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.FeedLike;
import com.example.demo.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeedLikeRepository extends JpaRepository<FeedLike, Long> {

    // [최적화] 존재 확인 - LIMIT 1로 바로 종료
    boolean existsByUserAndFeed(User user, Feed feed);

    // ID로 존재 확인 (더 빠름)
    @Query("SELECT COUNT(fl) > 0 FROM FeedLike fl WHERE fl.user.id = :userId AND fl.feed.id = :feedId")
    boolean existsByUserIdAndFeedId(@Param("userId") Long userId, @Param("feedId") Long feedId);

    Optional<FeedLike> findByUserAndFeed(User user, Feed feed);

    // [최적화] 벌크 삭제 - 조회 없이 바로 삭제
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM FeedLike fl WHERE fl.user = :user AND fl.feed = :feed")
    void deleteByUserAndFeed(@Param("user") User user, @Param("feed") Feed feed);

    // [최적화] 피드 삭제 시 벌크 삭제
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM FeedLike fl WHERE fl.feed = :feed")
    void deleteByFeed(@Param("feed") Feed feed);

    long countByFeed(Feed feed);

    List<FeedLike> findByFeed(Feed feed);
}
`;
fs.writeFileSync(feedLikeRepoPath, feedLikeRepoContent);
console.log('FeedLikeRepository updated!');

// 3. NotificationRepository - Slice + markAllAsRead 추가
const notifRepoPath = 'c:/Lupin/lupin/src/main/java/com/example/demo/repository/NotificationRepository.java';
const notifRepoContent = `package com.example.demo.repository;

import com.example.demo.domain.entity.Notification;
import com.example.demo.domain.entity.User;
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
    void deleteByRefIdAndTypeIn(@Param("refId") String refId, @Param("types") List<String> types);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.refId = :refId AND n.type = :type")
    void deleteByRefIdAndType(@Param("refId") String refId, @Param("type") String type);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.refId IN :refIds AND n.type = :type")
    void deleteByRefIdInAndType(@Param("refIds") List<String> refIds, @Param("type") String type);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.createdAt < :cutoffDate")
    int deleteByCreatedAtBefore(@Param("cutoffDate") LocalDateTime cutoffDate);
}
`;
fs.writeFileSync(notifRepoPath, notifRepoContent);
console.log('NotificationRepository updated!');

// 4. UserRepository - 포인트용 Lock 메서드 추가
const userRepoPath = 'c:/Lupin/lupin/src/main/java/com/example/demo/repository/UserRepository.java';
const userRepoContent = `package com.example.demo.repository;

import com.example.demo.domain.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUserId(String userId);

    Optional<User> findByProviderEmail(String providerEmail);

    Optional<User> findByProviderAndProviderId(String provider, String providerId);

    // [최적화] 포인트 수정 시 비관적 락 (동시성 제어)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") Long id);

    // [최적화] userId 중복 확인
    boolean existsByUserId(String userId);
}
`;
fs.writeFileSync(userRepoPath, userRepoContent);
console.log('UserRepository updated!');

console.log('\\nAll repositories updated!');
