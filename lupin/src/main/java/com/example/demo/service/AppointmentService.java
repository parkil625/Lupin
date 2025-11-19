package com.example.demo.service;

import com.example.demo.domain.entity.Appointment;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.AppointmentStatus;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.AppointmentRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 예약 관련 비즈니스 로직을 처리하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;

    /**
     * 예약 생성
     */
    @Transactional
    public Appointment createAppointment(Long patientId, Long doctorId,
                                        LocalDateTime apptDate, String reason) {
        User patient = findUserById(patientId);
        User doctor = doctorId != null ? findUserById(doctorId) : null;

        Appointment appointment = Appointment.builder()
                .patient(patient)
                .doctor(doctor)
                .apptDate(apptDate)
                .reason(reason)
                .status(AppointmentStatus.SCHEDULED)
                .build();

        Appointment savedAppointment = appointmentRepository.save(appointment);

        log.info("예약 생성 완료 - appointmentId: {}, patientId: {}, doctorId: {}",
                savedAppointment.getId(), patientId, doctorId);

        return savedAppointment;
    }

    /**
     * 환자의 예약 목록 조회
     */
    public List<Appointment> getPatientAppointments(Long patientId) {
        return appointmentRepository.findByPatientId(patientId);
    }

    /**
     * 의사의 예약 목록 조회
     */
    public List<Appointment> getDoctorAppointments(Long doctorId) {
        return appointmentRepository.findByDoctorId(doctorId);
    }

    /**
     * 예약 상세 조회
     */
    public Appointment getAppointmentDetail(Long appointmentId) {
        return findAppointmentById(appointmentId);
    }

    /**
     * 예약 완료 처리
     */
    @Transactional
    public void completeAppointment(Long appointmentId) {
        Appointment appointment = findAppointmentById(appointmentId);

        try {
            appointment.complete();
            log.info("예약 완료 처리 - appointmentId: {}", appointmentId);
        } catch (IllegalStateException e) {
            throw new BusinessException(ErrorCode.APPOINTMENT_ALREADY_CANCELLED, e.getMessage());
        }
    }

    /**
     * 예약 취소
     */
    @Transactional
    public void cancelAppointment(Long appointmentId) {
        Appointment appointment = findAppointmentById(appointmentId);

        try {
            appointment.cancel();
            log.info("예약 취소 완료 - appointmentId: {}", appointmentId);
        } catch (IllegalStateException e) {
            throw new BusinessException(ErrorCode.APPOINTMENT_ALREADY_COMPLETED, e.getMessage());
        }
    }

    /**
     * 특정 기간의 예약 조회
     */
    public List<Appointment> getAppointmentsBetween(LocalDateTime start, LocalDateTime end) {
        return appointmentRepository.findByApptDateBetweenOrderByApptDateAsc(start, end);
    }

    /**
     * ID로 예약 조회 (내부 메서드)
     */
    private Appointment findAppointmentById(Long appointmentId) {
        return appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPOINTMENT_NOT_FOUND));
    }

    /**
     * ID로 사용자 조회 (내부 메서드)
     */
    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
