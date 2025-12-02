package com.example.demo.repository;

import com.example.demo.domain.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByPatientIdOrderByDateDesc(Long patientId);

    List<Appointment> findByDoctorIdOrderByDateDesc(Long doctorId);

    boolean existsByDoctorIdAndDate(Long doctorId, LocalDateTime date);

    boolean existsByPatientIdAndDate(Long patientId, LocalDateTime date);
}
