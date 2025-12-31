package com.example.demo.repository;

import com.example.demo.domain.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    // 취소되지 않은 예약만 체크 (CANCELLED 제외)
    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END " +
           "FROM Appointment a " +
           "WHERE a.doctor.id = :doctorId " +
           "AND a.date = :date " +
           "AND a.status != 'CANCELLED'")
    boolean existsByDoctorIdAndDate(@Param("doctorId") Long doctorId, @Param("date") LocalDateTime date);

    boolean existsByPatientIdAndDate(Long patientId, LocalDateTime date);

    // Patient와 Doctor를 Eager Loading하여 조회 (Lazy Loading 에러 방지)
    @Query("SELECT a FROM Appointment a " +
           "JOIN FETCH a.patient " +
           "JOIN FETCH a.doctor " +
           "WHERE a.id = :id")
    Optional<Appointment> findByIdWithPatientAndDoctor(@Param("id") Long id);

    // 특정 의사의 특정 날짜 범위 내 예약 목록 조회
    @Query("SELECT a FROM Appointment a " +
           "WHERE a.doctor.id = :doctorId " +
           "AND a.date BETWEEN :startDate AND :endDate")
    List<Appointment> findByDoctorIdAndDateBetween(
            @Param("doctorId") Long doctorId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    // 특정 상태와 날짜 범위 내 예약 목록 조회 (리마인더용)
    @Query("SELECT a FROM Appointment a " +
           "JOIN FETCH a.patient " +
           "JOIN FETCH a.doctor " +
           "WHERE a.status = :status " +
           "AND a.date BETWEEN :startDate AND :endDate")
    List<Appointment> findByStatusAndDateBetween(
            @Param("status") com.example.demo.domain.enums.AppointmentStatus status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    // 예약 시간이 5분 전인 예약들의 상태를 일괄적으로 진료 중(IN_PROGRESS)로 변경
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Appointment a SET a.status = 'IN_PROGRESS' " +
       "WHERE a.status = 'SCHEDULED' AND a.date <= :thresholdTime")
       int bulkUpdateStatusToInProgress(@Param("thresholdTime") LocalDateTime thresholdTime);
}
