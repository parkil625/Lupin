package com.example.demo.repository;

import com.example.demo.domain.entity.DoctorProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorProfileRepository extends JpaRepository<DoctorProfile, Long> {

    /**
     * User ID로 의사 프로필 조회
     */
    Optional<DoctorProfile> findByUserId(Long userId);

    /**
     * 전공(specialty)으로 의사 프로필 목록 조회
     */
    List<DoctorProfile> findBySpecialty(String specialty);

    /**
     * 모든 의사 프로필 조회 (User 정보 포함)
     */
    @Query("SELECT dp FROM DoctorProfile dp JOIN FETCH dp.user")
    List<DoctorProfile> findAllWithUser();

    /**
     * 전공별 의사 프로필 조회 (User 정보 포함)
     */
    @Query("SELECT dp FROM DoctorProfile dp JOIN FETCH dp.user WHERE dp.specialty = :specialty")
    List<DoctorProfile> findBySpecialtyWithUser(@Param("specialty") String specialty);
}
