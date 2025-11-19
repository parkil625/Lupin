package com.example.demo.repository;

import com.example.demo.domain.entity.PrescriptionMed;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PrescriptionMedRepository extends JpaRepository<PrescriptionMed, Long> {

    /**
     * 특정 처방전의 약품 목록 조회
     */
    @Query("SELECT pm FROM PrescriptionMed pm WHERE pm.prescription.id = :prescriptionId")
    List<PrescriptionMed> findByPrescriptionId(@Param("prescriptionId") Long prescriptionId);
}
