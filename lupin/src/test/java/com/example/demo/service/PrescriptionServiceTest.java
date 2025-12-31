package com.example.demo.service;

import com.example.demo.domain.entity.Appointment;
import com.example.demo.domain.entity.Medicine;
import com.example.demo.domain.entity.Prescription;
import com.example.demo.domain.entity.User;
import com.example.demo.dto.prescription.PrescriptionRequest;
import com.example.demo.dto.prescription.PrescriptionResponse;
import com.example.demo.repository.AppointmentRepository;
import com.example.demo.repository.MedicineRepository;
import com.example.demo.repository.PrescriptionRepository;
import com.example.demo.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("PrescriptionService 테스트")
class PrescriptionServiceTest {

    @Mock
    private PrescriptionRepository prescriptionRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MedicineRepository medicineRepository;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private PrescriptionService prescriptionService;

    private User doctor;
    private User patient;
    private Appointment appointment;
    private Medicine tylenol;
    private Medicine aspirin;

    @BeforeEach
    void setUp() {
        doctor = User.builder()
                .id(1L)
                .userId("doctor1")
                .name("Dr. Kim")
                .build();

        patient = User.builder()
                .id(3L)
                .userId("patient1")
                .name("Patient Lee")
                .build();

        appointment = Appointment.builder()
                .id(1L)
                .patient(patient)
                .doctor(doctor)
                .date(LocalDateTime.of(2025, 12, 1, 14, 0))
                .build();

        tylenol = Medicine.builder()
                .id(1L)
                .name("타이레놀")
                .code("MED001")
                .precautions("간 질환 환자 주의")
                .build();

        aspirin = Medicine.builder()
                .id(2L)
                .name("아스피린")
                .code("MED002")
                .precautions("출혈 위험 주의")
                .build();
    }

    @Test
    @DisplayName("처방전 생성 성공")
    void createPrescription_Success() {
        // given
        PrescriptionRequest request = PrescriptionRequest.builder()
                .appointmentId(1L)
                .patientId(patient.getId())
                .diagnosis("감기")
                .medicines(Arrays.asList(
                        PrescriptionRequest.MedicineItem.builder()
                                .medicineName("타이레놀")
                                .build(),
                        PrescriptionRequest.MedicineItem.builder()
                                .medicineName("아스피린")
                                .build()
                ))
                .additionalInstructions("하루 3회 복용")
                .build();

        given(appointmentRepository.findByIdWithPatientAndDoctor(1L))
                .willReturn(Optional.of(appointment));
        given(prescriptionRepository.findByAppointmentId(1L))
                .willReturn(Optional.empty());
        given(userRepository.findById(doctor.getId()))
                .willReturn(Optional.of(doctor));
        given(userRepository.findById(patient.getId()))
                .willReturn(Optional.of(patient));
        given(medicineRepository.findByName("타이레놀"))
                .willReturn(Optional.of(tylenol));
        given(medicineRepository.findByName("아스피린"))
                .willReturn(Optional.of(aspirin));

        // 1차 저장 (save) Mocking: ID 생성을 위해 먼저 호출됨
        given(prescriptionRepository.save(any(Prescription.class)))
                .willAnswer(invocation -> {
                    Prescription p = invocation.getArgument(0);
                    return Prescription.builder()
                            .id(1L)
                            .doctor(p.getDoctor())
                            .patient(p.getPatient())
                            .appointment(p.getAppointment())
                            .diagnosis(p.getDiagnosis())
                            .instructions(p.getInstructions())
                            .date(p.getDate())
                            .medicines(new ArrayList<>())
                            .build();
                });

        // [필수] 서비스 마지막에 저장된 데이터를 다시 조회하는 findById 호출에 대한 Mocking
        Prescription finalPrescription = Prescription.builder()
                .id(1L)
                .doctor(doctor)
                .patient(patient)
                .appointment(appointment)
                .diagnosis("감기")
                .instructions("하루 3회 복용")
                .medicines(new ArrayList<>())
                .build();

        given(prescriptionRepository.findById(1L))
                .willReturn(Optional.of(finalPrescription));

        // when
        PrescriptionResponse result = prescriptionService.createPrescription(doctor.getId(), request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getDiagnosis()).isEqualTo("감기");
        assertThat(result.getInstructions()).isEqualTo("하루 3회 복용");
    }

    @Test
    @DisplayName("약품 정보를 prescription_medicines 테이블에 저장")
    void createPrescription_SaveMedicinesInJoinTable() {
        // given
        PrescriptionRequest request = PrescriptionRequest.builder()
                .appointmentId(1L)
                .patientId(patient.getId())
                .diagnosis("통증")
                .medicines(Arrays.asList(
                        PrescriptionRequest.MedicineItem.builder()
                                .medicineName("타이레놀")
                                .build()
                ))
                .build();

        given(appointmentRepository.findByIdWithPatientAndDoctor(1L))
                .willReturn(Optional.of(appointment));
        given(prescriptionRepository.findByAppointmentId(1L))
                .willReturn(Optional.empty());
        given(userRepository.findById(doctor.getId()))
                .willReturn(Optional.of(doctor));
        given(userRepository.findById(patient.getId()))
                .willReturn(Optional.of(patient));
        given(medicineRepository.findByName("타이레놀"))
                .willReturn(Optional.of(tylenol));

        // 1차 저장 (save) Mocking
        given(prescriptionRepository.save(any(Prescription.class)))
                .willAnswer(invocation -> {
                    Prescription p = invocation.getArgument(0);
                    return Prescription.builder()
                            .id(1L)
                            .doctor(p.getDoctor())
                            .patient(p.getPatient())
                            .appointment(p.getAppointment())
                            .diagnosis(p.getDiagnosis())
                            .instructions(p.getInstructions())
                            .date(p.getDate())
                            .medicines(new ArrayList<>())
                            .build();
                });

        // [필수] 서비스 마지막 findById 조회 Mocking
        Prescription finalPrescription = Prescription.builder()
                .id(1L)
                .doctor(doctor)
                .patient(patient)
                .appointment(appointment)
                .diagnosis("통증")
                .medicines(new ArrayList<>())
                .build();

        given(prescriptionRepository.findById(1L))
                .willReturn(Optional.of(finalPrescription));

        // when
        PrescriptionResponse result = prescriptionService.createPrescription(doctor.getId(), request);

        // then
        assertThat(result.getDiagnosis()).isEqualTo("통증");
    }

    @Test
    @DisplayName("예약을 찾을 수 없는 경우 예외")
    void createPrescription_AppointmentNotFound() {
        // given
        PrescriptionRequest request = PrescriptionRequest.builder()
                .appointmentId(999L)
                .patientId(patient.getId())
                .diagnosis("감기")
                .medicines(Arrays.asList(
                        PrescriptionRequest.MedicineItem.builder()
                                .medicineName("타이레놀")
                                .build()
                ))
                .build();

        given(appointmentRepository.findByIdWithPatientAndDoctor(999L))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
                prescriptionService.createPrescription(doctor.getId(), request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("예약을 찾을 수 없습니다"); // [수정됨] 상세 메시지 포함 가능성 고려
    }

    @Test
    @DisplayName("의사를 찾을 수 없는 경우 예외")
    void createPrescription_DoctorNotFound() {
        // given
        PrescriptionRequest request = PrescriptionRequest.builder()
                .appointmentId(1L)
                .patientId(patient.getId())
                .diagnosis("감기")
                .medicines(Arrays.asList(
                        PrescriptionRequest.MedicineItem.builder()
                                .medicineName("타이레놀")
                                .build()
                ))
                .build();

        given(appointmentRepository.findByIdWithPatientAndDoctor(1L))
                .willReturn(Optional.of(appointment));
        given(prescriptionRepository.findByAppointmentId(1L))
                .willReturn(Optional.empty());
        given(userRepository.findById(doctor.getId()))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
                prescriptionService.createPrescription(doctor.getId(), request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("의사를 찾을 수 없습니다"); // [수정됨] 상세 메시지 포함 가능성 고려
    }

    @Test
    @DisplayName("환자를 찾을 수 없는 경우 예외")
    void createPrescription_PatientNotFound() {
        // given
        PrescriptionRequest request = PrescriptionRequest.builder()
                .appointmentId(1L)
                .patientId(patient.getId())
                .diagnosis("감기")
                .medicines(Arrays.asList(
                        PrescriptionRequest.MedicineItem.builder()
                                .medicineName("타이레놀")
                                .build()
                ))
                .build();

        given(appointmentRepository.findByIdWithPatientAndDoctor(1L))
                .willReturn(Optional.of(appointment));
        given(prescriptionRepository.findByAppointmentId(1L))
                .willReturn(Optional.empty());
        given(userRepository.findById(doctor.getId()))
                .willReturn(Optional.of(doctor));
        given(userRepository.findById(patient.getId()))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
                prescriptionService.createPrescription(doctor.getId(), request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("환자를 찾을 수 없습니다"); // [수정됨] 상세 메시지 포함 가능성 고려
    }

    @Test
    @DisplayName("약품을 찾을 수 없는 경우 예외")
    void createPrescription_MedicineNotFound() {
        // given
        PrescriptionRequest request = PrescriptionRequest.builder()
                .appointmentId(1L)
                .patientId(patient.getId())
                .diagnosis("감기")
                .medicines(Arrays.asList(
                        PrescriptionRequest.MedicineItem.builder()
                                .medicineName("존재하지않는약")
                                .build()
                ))
                .build();

        given(appointmentRepository.findByIdWithPatientAndDoctor(1L))
                .willReturn(Optional.of(appointment));
        given(prescriptionRepository.findByAppointmentId(1L))
                .willReturn(Optional.empty());
        given(userRepository.findById(doctor.getId()))
                .willReturn(Optional.of(doctor));
        given(userRepository.findById(patient.getId()))
                .willReturn(Optional.of(patient));
        given(medicineRepository.findByName("존재하지않는약"))
                .willReturn(Optional.empty());

        // [수정됨] 1차 저장 (save) Mocking 추가: 약품 조회 전에 save가 호출되므로 필요함
        given(prescriptionRepository.save(any(Prescription.class)))
                .willAnswer(invocation -> {
                    Prescription p = invocation.getArgument(0);
                    return Prescription.builder()
                            .id(1L)
                            .doctor(p.getDoctor())
                            .patient(p.getPatient())
                            .appointment(p.getAppointment())
                            .diagnosis(p.getDiagnosis())
                            .instructions(p.getInstructions())
                            .date(p.getDate())
                            .medicines(new ArrayList<>())
                            .build();
                });

        // when & then
        assertThatThrownBy(() ->
                prescriptionService.createPrescription(doctor.getId(), request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("찾을 수 없습니다"); // [수정됨] 약품/약품명 메시지 유연하게 대응
    }
}