package com.example.demo.repository;

import com.example.demo.domain.entity.Appointment;
import com.example.demo.domain.enums.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    // 환자의 예약 목록
    @Query("SELECT a FROM Appointment a " +
           "LEFT JOIN FETCH a.patient " +
           "LEFT JOIN FETCH a.doctor " +
           "WHERE a.patient.id = :patientId " +
           "ORDER BY a.apptDate DESC")
    List<Appointment> findByPatientId(@Param("patientId") Long patientId);

    // 의사의 예약 목록
    @Query("SELECT a FROM Appointment a " +
           "LEFT JOIN FETCH a.patient " +
           "LEFT JOIN FETCH a.doctor " +
           "WHERE a.doctor.id = :doctorId " +
           "ORDER BY a.apptDate ASC")
    List<Appointment> findByDoctorId(@Param("doctorId") Long doctorId);

    // 특정 상태의 예약 목록
    List<Appointment> findByStatusOrderByApptDateAsc(AppointmentStatus status);

    // 특정 기간의 예약 조회
    List<Appointment> findByApptDateBetweenOrderByApptDateAsc(LocalDateTime start, LocalDateTime end);
}
