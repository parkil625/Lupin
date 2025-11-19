package com.example.demo.repository;

import com.example.demo.domain.entity.Prescription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {

    /**
     * 특정 환자의 처방전 목록 조회 (페이징)
     */
    @Query("SELECT p FROM Prescription p WHERE p.patient.id = :patientId ORDER BY p.prescribedDate DESC")
    Page<Prescription> findByPatientId(@Param("patientId") Long patientId, Pageable pageable);

    /**
     * 특정 환자의 처방전 목록 조회 (전체)
     */
    @Query("SELECT p FROM Prescription p WHERE p.patient.id = :patientId ORDER BY p.prescribedDate DESC")
    List<Prescription> findByPatientId(@Param("patientId") Long patientId);

    /**
     * 특정 의사가 발행한 처방전 목록 조회 (페이징)
     */
    @Query("SELECT p FROM Prescription p WHERE p.doctor.id = :doctorId ORDER BY p.prescribedDate DESC")
    Page<Prescription> findByDoctorId(@Param("doctorId") Long doctorId, Pageable pageable);

    /**
     * 특정 의사가 발행한 처방전 목록 조회 (전체)
     */
    @Query("SELECT p FROM Prescription p WHERE p.doctor.id = :doctorId ORDER BY p.prescribedDate DESC")
    List<Prescription> findByDoctorId(@Param("doctorId") Long doctorId);

    /**
     * 특정 기간 내 처방전 조회
     */
    @Query("SELECT p FROM Prescription p WHERE p.prescribedDate BETWEEN :startDate AND :endDate ORDER BY p.prescribedDate DESC")
    List<Prescription> findByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * 특정 환자의 최근 처방전 조회
     */
    @Query("SELECT p FROM Prescription p WHERE p.patient.id = :patientId ORDER BY p.prescribedDate DESC")
    List<Prescription> findRecentPrescriptionsByPatientId(@Param("patientId") Long patientId, Pageable pageable);

    /**
     * 처방전 이름으로 검색
     */
    @Query("SELECT p FROM Prescription p WHERE p.prescriptionName LIKE %:keyword% ORDER BY p.prescribedDate DESC")
    Page<Prescription> searchByPrescriptionName(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 특정 환자의 처방전 수 조회
     */
    @Query("SELECT COUNT(p) FROM Prescription p WHERE p.patient.id = :patientId")
    Long countByPatientId(@Param("patientId") Long patientId);
}
