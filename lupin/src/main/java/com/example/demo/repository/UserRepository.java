package com.example.demo.repository;

import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.Role;
import com.example.demo.domain.enums.SocialProvider;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUserId(String userId);

    Optional<User> findByProviderEmail(String providerEmail);

    Optional<User> findByProviderAndProviderId(SocialProvider provider, String providerId);

    // [최적화] 포인트 수정 시 비관적 락 (동시성 제어)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") Long id);

    // [최적화] userId 중복 확인
    boolean existsByUserId(String userId);

    // 진료과별 의사 조회
    List<User> findByRoleAndDepartment(Role role, String department);

    /**
     * 모든 유저의 totalPoints를 point_logs에서 일괄 동기화
     * 반정규화 필드 초기 동기화 또는 복구용
     */
    @Modifying
    @Query(value = """
        UPDATE users u
        SET total_points = COALESCE((
            SELECT SUM(p.points)
            FROM point_logs p
            WHERE p.user_id = u.id
        ), 0)
        """, nativeQuery = true)
    int syncAllUserTotalPoints();
}
