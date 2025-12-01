package com.example.demo.repository;

import com.example.demo.domain.entity.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {

    List<Prescription> findByPatientIdOrderByDateDesc(Long patientId);

    List<Prescription> findByDoctorIdOrderByDateDesc(Long doctorId);

    List<Prescription> findByPatientIdAndDateBetweenOrderByDateDesc(Long patientId, LocalDate startDate, LocalDate endDate);
}
