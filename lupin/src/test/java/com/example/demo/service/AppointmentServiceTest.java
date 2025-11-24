package com.example.demo.service;

import com.example.demo.domain.entity.Appointment;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.AppointmentStatus;
import com.example.demo.exception.BusinessException;
import com.example.demo.repository.AppointmentRepository;
import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AppointmentService 테스트")
class AppointmentServiceTest {

    @InjectMocks
    private AppointmentService appointmentService;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private UserRepository userRepository;

    @Test
    @DisplayName("예약 생성 성공")
    void createAppointment_Success() {
        // given
        User patient = User.builder().id(1L).userId("patient01").build();
        User doctor = User.builder().id(2L).userId("doctor01").build();
        LocalDateTime apptDate = LocalDateTime.now().plusDays(1);

        given(userRepository.findById(1L)).willReturn(Optional.of(patient));
        given(userRepository.findById(2L)).willReturn(Optional.of(doctor));
        given(appointmentRepository.save(any(Appointment.class))).willAnswer(invocation -> {
            Appointment apt = invocation.getArgument(0);
            return Appointment.builder()
                    .id(1L)
                    .patient(apt.getPatient())
                    .doctor(apt.getDoctor())
                    .apptDate(apt.getApptDate())
                    .reason(apt.getReason())
                    .status(AppointmentStatus.SCHEDULED)
                    .build();
        });

        // when
        Appointment result = appointmentService.createAppointment(1L, 2L, apptDate, "건강검진");

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        then(appointmentRepository).should().save(any(Appointment.class));
    }

    @Test
    @DisplayName("예약 생성 - 의사 없이")
    void createAppointment_WithoutDoctor_Success() {
        // given
        User patient = User.builder().id(1L).userId("patient01").build();
        LocalDateTime apptDate = LocalDateTime.now().plusDays(1);

        given(userRepository.findById(1L)).willReturn(Optional.of(patient));
        given(appointmentRepository.save(any(Appointment.class))).willAnswer(invocation -> {
            Appointment apt = invocation.getArgument(0);
            return Appointment.builder()
                    .id(1L)
                    .patient(apt.getPatient())
                    .doctor(null)
                    .apptDate(apt.getApptDate())
                    .reason(apt.getReason())
                    .status(AppointmentStatus.SCHEDULED)
                    .build();
        });

        // when
        Appointment result = appointmentService.createAppointment(1L, null, apptDate, "상담");

        // then
        assertThat(result).isNotNull();
        assertThat(result.getDoctor()).isNull();
    }

    @Test
    @DisplayName("예약 생성 실패 - 환자 없음")
    void createAppointment_PatientNotFound() {
        // given
        given(userRepository.findById(1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> appointmentService.createAppointment(1L, 2L, LocalDateTime.now(), "건강검진"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("환자 예약 목록 조회")
    void getPatientAppointments_Success() {
        // given
        List<Appointment> appointments = Arrays.asList(
                Appointment.builder().id(1L).build(),
                Appointment.builder().id(2L).build()
        );
        given(appointmentRepository.findByPatientId(1L)).willReturn(appointments);

        // when
        List<Appointment> result = appointmentService.getPatientAppointments(1L);

        // then
        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("의사 예약 목록 조회")
    void getDoctorAppointments_Success() {
        // given
        List<Appointment> appointments = Arrays.asList(
                Appointment.builder().id(1L).build()
        );
        given(appointmentRepository.findByDoctorId(1L)).willReturn(appointments);

        // when
        List<Appointment> result = appointmentService.getDoctorAppointments(1L);

        // then
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("예약 상세 조회 성공")
    void getAppointmentDetail_Success() {
        // given
        Appointment appointment = Appointment.builder().id(1L).reason("검진").build();
        given(appointmentRepository.findById(1L)).willReturn(Optional.of(appointment));

        // when
        Appointment result = appointmentService.getAppointmentDetail(1L);

        // then
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("예약 상세 조회 실패 - 예약 없음")
    void getAppointmentDetail_NotFound() {
        // given
        given(appointmentRepository.findById(1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> appointmentService.getAppointmentDetail(1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("예약 완료 처리 성공")
    void completeAppointment_Success() {
        // given
        Appointment appointment = mock(Appointment.class);
        given(appointmentRepository.findById(1L)).willReturn(Optional.of(appointment));

        // when
        appointmentService.completeAppointment(1L);

        // then
        then(appointment).should().complete();
    }

    @Test
    @DisplayName("예약 완료 처리 실패 - 이미 취소됨")
    void completeAppointment_AlreadyCancelled() {
        // given
        Appointment appointment = mock(Appointment.class);
        given(appointmentRepository.findById(1L)).willReturn(Optional.of(appointment));
        willThrow(new IllegalStateException("이미 취소된 예약")).given(appointment).complete();

        // when & then
        assertThatThrownBy(() -> appointmentService.completeAppointment(1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("예약 취소 성공")
    void cancelAppointment_Success() {
        // given
        Appointment appointment = mock(Appointment.class);
        given(appointmentRepository.findById(1L)).willReturn(Optional.of(appointment));

        // when
        appointmentService.cancelAppointment(1L);

        // then
        then(appointment).should().cancel();
    }

    @Test
    @DisplayName("예약 취소 실패 - 이미 완료됨")
    void cancelAppointment_AlreadyCompleted() {
        // given
        Appointment appointment = mock(Appointment.class);
        given(appointmentRepository.findById(1L)).willReturn(Optional.of(appointment));
        willThrow(new IllegalStateException("이미 완료된 예약")).given(appointment).cancel();

        // when & then
        assertThatThrownBy(() -> appointmentService.cancelAppointment(1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("기간별 예약 조회")
    void getAppointmentsBetween_Success() {
        // given
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusDays(7);
        List<Appointment> appointments = Arrays.asList(
                Appointment.builder().id(1L).build()
        );
        given(appointmentRepository.findByApptDateBetweenOrderByApptDateAsc(start, end))
                .willReturn(appointments);

        // when
        List<Appointment> result = appointmentService.getAppointmentsBetween(start, end);

        // then
        assertThat(result).hasSize(1);
    }
}
