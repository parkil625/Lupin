package com.example.demo.repository;

import com.example.demo.domain.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByIdPatient_id(Long appointment);

    List<Appointment> findByIdDoctor_id(Long appointment);
}
