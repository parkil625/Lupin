package com.example.demo.repository;

import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.Role;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
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

    // 진료과별 의사 조회
    List<User> findByRoleAndDepartment(Role role, String department);
}
