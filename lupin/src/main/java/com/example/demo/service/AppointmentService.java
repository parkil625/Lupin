package com.example.demo.service;
import com.example.demo.domain.entity.Appointment;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.AppointmentStatus;
import com.example.demo.dto.request.AppointmentRequest;
import com.example.demo.repository.AppointmentRepository;
import com.example.demo.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final ChatService chatService;

    @Transactional
    public Long createAppointment(AppointmentRequest request) {
        // 1. 환자 & 의사 존재 여부 확인
        User patient = userRepository.findById(request.getPatientId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 환자입니다."));

        User doctor = userRepository.findById(request.getDoctorId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 의사입니다."));

        if (appointmentRepository.existsByDoctorIdAndDate(doctor.getId(), request.getDate())) {
            throw new IllegalStateException("해당 시간에 예약이 이미 꽉 찼습니다.");
        }

        if (appointmentRepository.existsByPatientIdAndDate(patient.getId(), request.getDate())) {
            throw new IllegalStateException("같은 시간에 다른 예약이 잡혀 있습니다.");
        }

        Appointment appointment = Appointment.builder()
                .patient(patient)
                .doctor(doctor)
                .date(request.getDate())
                .status(AppointmentStatus.SCHEDULED)
                .build();

        Appointment savedAppointment = appointmentRepository.save(appointment);

        // 예약 생성 시 자동으로 채팅방 생성 (환영 메시지 전송)
        String roomId = chatService.createRoomIdForAppointment(savedAppointment.getId());
        log.info("예약 ID {}에 대한 채팅방 생성 완료: {}", savedAppointment.getId(), roomId);

        // 시스템 환영 메시지 전송
        try {
            chatService.saveMessage(
                    roomId,
                    doctor.getId(),
                    "안녕하세요, " + patient.getName() + "님. 예약이 확정되었습니다. 궁금하신 점이 있으시면 편하게 말씀해주세요."
            );
            log.info("채팅방 {}에 환영 메시지 전송 완료", roomId);
        } catch (Exception e) {
            log.warn("환영 메시지 전송 실패: {}", e.getMessage());
        }

        return savedAppointment.getId();
    }

    public List<Appointment> getDoctorAppointments(Long doctorId) {
        return appointmentRepository.findByDoctorIdOrderByDateDesc(doctorId);
    }

    @Transactional
    public void cancelAppointment(Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 예약입니다."));

        // 엔티티 내부의 비즈니스 로직 호출 (상태 변경 검증 포함)
        appointment.cancel();
    }

    @Transactional
    public void startConsultation(Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 예약입니다."));

        appointment.startConsultation();
    }

    @Transactional
    public void completeConsultation(Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 예약입니다."));

        appointment.complete();
    }
}
