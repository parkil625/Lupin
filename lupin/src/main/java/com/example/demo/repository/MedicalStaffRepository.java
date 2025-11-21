package com.example.demo.repository;

import com.example.demo.domain.entity.MedicalStaff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MedicalStaffRepository extends JpaRepository<MedicalStaff, Long> {
    Optional<MedicalStaff> findByUserId(String userId);
    Optional<MedicalStaff> findByEmail(String email);
    boolean existsByUserId(String userId);
    boolean existsByEmail(String email);
}
