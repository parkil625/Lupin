package com.example.demo.service;
import com.example.demo.domain.entity.Appointment;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.Role;
import com.example.demo.domain.enums.AppointmentStatus;
import com.example.demo.dto.request.AppointmentRequest;
import com.example.demo.repository.AppointmentRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final ChatService chatService;

    @Transactional
    public Long createAppointment(AppointmentRequest request) {
        // 1. 환자 & 의사 존재 여부 확인
        User patient = userRepository.findById(request.getPatientId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "존재하지 않는 환자입니다."));

        User doctor = userRepository.findById(request.getDoctorId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "존재하지 않는 의사입니다."));

        if (doctor.getRole() != Role.DOCTOR) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "해당 사용자는 의사가 아닙니다.");
        }

        if (appointmentRepository.existsByDoctorIdAndDate(doctor.getId(), request.getDate())) {
            throw new BusinessException(ErrorCode.APPOINTMENT_ALREADY_EXISTS, "해당 시간에 예약이 이미 꽉 찼습니다.");
        }

        if (appointmentRepository.existsByPatientIdAndDate(patient.getId(), request.getDate())) {
            throw new BusinessException(ErrorCode.APPOINTMENT_ALREADY_EXISTS, "같은 시간에 다른 예약이 잡혀 있습니다.");
        }

        Appointment appointment = Appointment.builder()
                .patient(patient)
                .doctor(doctor)
                .date(request.getDate())
                .status(AppointmentStatus.SCHEDULED)
                .departmentName(doctor.getDepartement())
                .build();

        Appointment savedAppointment = appointmentRepository.save(appointment);

        // 예약 생성 시 자동으로 채팅방 생성 (메시지 없이)
        String roomId = chatService.createChatRoomForAppointment(savedAppointment.getId());
        log.info("예약 ID {}에 대한 채팅방 생성 완료: {}", savedAppointment.getId(), roomId);

        return savedAppointment.getId();
    }

    public List<Appointment> getDoctorAppointments(Long doctorId) {
        return appointmentRepository.findByDoctorIdOrderByDateDesc(doctorId);
    }

    public List<Appointment> getPatientAppointments(Long patientId) {
        return appointmentRepository.findByPatientIdOrderByDateDesc(patientId);
    }

    @Transactional
    public void cancelAppointment(Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPOINTMENT_NOT_FOUND, "존재하지 않는 예약입니다."));

        // 엔티티 내부의 비즈니스 로직 호출 (상태 변경 검증 포함)
        appointment.cancel();
    }

    @Transactional
    public void startConsultation(Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPOINTMENT_NOT_FOUND, "존재하지 않는 예약입니다."));

        appointment.startConsultation();
    }

    @Transactional
    public void completeConsultation(Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPOINTMENT_NOT_FOUND, "존재하지 않는 예약입니다."));

        appointment.complete();
    }

    public List<String> getBookedTimesByDoctorAndDate(Long doctorId, LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();

        List<Appointment> appointments = appointmentRepository.findByDoctorIdAndDateBetween(
                doctorId, startOfDay, endOfDay
        );

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        return appointments.stream()
                .filter(apt -> apt.getStatus() != AppointmentStatus.CANCELLED)
                .map(apt -> apt.getDate().format(timeFormatter))
                .collect(Collectors.toList());
    }
}
