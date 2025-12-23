package com.example.demo.repository;

import com.example.demo.domain.entity.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {

    @Query("SELECT DISTINCT p FROM Prescription p " +
           "LEFT JOIN FETCH p.patient " +
           "LEFT JOIN FETCH p.doctor " +
           "LEFT JOIN FETCH p.appointment " +
           "LEFT JOIN FETCH p.medicines m " +
           "LEFT JOIN FETCH m.medicine " +
           "WHERE p.patient.id = :patientId " +
           "ORDER BY p.date DESC")
    List<Prescription> findByPatientIdOrderByDateDesc(@Param("patientId") Long patientId);

    @Query("SELECT DISTINCT p FROM Prescription p " +
           "LEFT JOIN FETCH p.patient " +
           "LEFT JOIN FETCH p.doctor " +
           "LEFT JOIN FETCH p.appointment " +
           "LEFT JOIN FETCH p.medicines m " +
           "LEFT JOIN FETCH m.medicine " +
           "WHERE p.doctor.id = :doctorId " +
           "ORDER BY p.date DESC")
    List<Prescription> findByDoctorIdOrderByDateDesc(@Param("doctorId") Long doctorId);

    List<Prescription> findByPatientIdAndDateBetweenOrderByDateDesc(Long patientId, LocalDate startDate, LocalDate endDate);

    Optional<Prescription> findByAppointmentId(Long appointmentId);
}
