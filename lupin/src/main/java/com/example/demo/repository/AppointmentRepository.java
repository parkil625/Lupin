package com.example.demo.repository;

import com.example.demo.domain.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    // @Query를 이용하여 환자와 의사 정보를 같이 가져옴
    @Query("SELECT a FROM Appointment a " +
       "JOIN FETCH a.patient " +
       "JOIN FETCH a.doctor " +
       "WHERE a.patient.id = :patientId " +
       "ORDER BY a.date DESC")
List<Appointment> findByPatientIdOrderByDateDesc(@Param("patientId") Long patientId);

    List<Appointment> findByDoctorIdOrderByDateDesc(Long doctorId);

    boolean existsByDoctorIdAndDate(Long doctorId, LocalDateTime date);

    boolean existsByPatientIdAndDate(Long patientId, LocalDateTime date);

    // Patient와 Doctor를 Eager Loading하여 조회 (Lazy Loading 에러 방지)
    @Query("SELECT a FROM Appointment a " +
           "JOIN FETCH a.patient " +
           "JOIN FETCH a.doctor " +
           "WHERE a.id = :id")
    Optional<Appointment> findByIdWithPatientAndDoctor(@Param("id") Long id);
}
