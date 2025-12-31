package com.example.demo.service;

import com.example.demo.domain.entity.Appointment;
import com.example.demo.domain.entity.Prescription;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.AppointmentStatus;
import com.example.demo.dto.prescription.PrescriptionRequest;
import com.example.demo.dto.prescription.PrescriptionResponse;
import com.example.demo.repository.AppointmentRepository;
import com.example.demo.repository.PrescriptionRepository;
import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
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

    @InjectMocks
    private PrescriptionService prescriptionService;

    private User doctor;
    private User anotherDoctor;
    private User patient;
    private Prescription prescription;

    @BeforeEach
    void setUp() {
        doctor = User.builder()
                .id(1L)
                .userId("doctor1")
                .name("Dr. Kim")
                .build();

        anotherDoctor = User.builder()
                .id(2L)
                .userId("doctor2")
                .name("Dr. Park")
                .build();

        patient = User.builder()
                .id(3L)
                .userId("patient1")
                .name("Patient Lee")
                .build();

        prescription = Prescription.builder()
                .id(1L)
                .doctor(doctor)
                .patient(patient)
                .date(LocalDate.of(2025, 12, 1))
                .diagnosis("감기")
                .build();
    }

    @Test
    @DisplayName("타인의 처방전 수정 시 예외 발생")
    void shouldThrowExceptionWhenUnauthorizedDoctorTriesToUpdate() {
        // given
        Long prescriptionId = 1L;
        Long unauthorizedDoctorId = 2L;
        String newDiagnosis = "독감";

        given(prescriptionRepository.findById(prescriptionId))
                .willReturn(Optional.of(prescription));

        // when & then
        assertThatThrownBy(() ->
                prescriptionService.updateDiagnosis(prescriptionId, unauthorizedDoctorId, newDiagnosis))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("처방전을 수정할 권한이 없습니다.");
    }

    @Test
    @DisplayName("타인의 처방전 삭제 시 예외 발생")
    void shouldThrowExceptionWhenUnauthorizedDoctorTriesToDelete() {
        // given
        Long prescriptionId = 1L;
        Long unauthorizedDoctorId = 2L;

        given(prescriptionRepository.findById(prescriptionId))
                .willReturn(Optional.of(prescription));

        // when & then
        assertThatThrownBy(() ->
                prescriptionService.deletePrescription(prescriptionId, unauthorizedDoctorId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("처방전을 삭제할 권한이 없습니다.");
    }

    @Test
    @DisplayName("예약 ID로 처방전 조회")
    void findPrescriptionByAppointmentId() {
        // given
        Long appointmentId = 1L;

        Appointment appointment = Appointment.builder()
                .id(appointmentId)
                .patient(patient)
                .doctor(doctor)
                .date(LocalDateTime.of(2025, 12, 1, 14, 0))
                .status(AppointmentStatus.COMPLETED)
                .build();

        Prescription prescriptionWithAppointment = Prescription.builder()
                .id(1L)
                .doctor(doctor)
                .patient(patient)
                .appointment(appointment)
                .date(LocalDate.of(2025, 12, 1))
                .diagnosis("감기")
                .build();

        given(prescriptionRepository.findByAppointmentId(appointmentId))
                .willReturn(Optional.of(prescriptionWithAppointment));

        // when
        Optional<Prescription> result = prescriptionService.findByAppointmentId(appointmentId);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getAppointment().getId()).isEqualTo(appointmentId);
        assertThat(result.get().getDoctor()).isEqualTo(doctor);
        assertThat(result.get().getPatient()).isEqualTo(patient);
    }

    @Test
    @DisplayName("채팅방에서 발행된 처방전 목록 조회 (appointment 형식)")
    void findPrescriptionsByRoomId_AppointmentFormat() {
        // given
        String roomId = "appointment_1";  // appointment_{appointmentId}
        Long appointmentId = 1L;

        Appointment appointment = Appointment.builder()
                .id(appointmentId)
                .patient(patient)
                .doctor(doctor)
                .date(LocalDateTime.of(2025, 12, 1, 14, 0))
                .status(AppointmentStatus.COMPLETED)
                .build();

        Prescription prescription = Prescription.builder()
                .id(1L)
                .doctor(doctor)
                .patient(patient)
                .appointment(appointment)
                .date(LocalDate.of(2025, 12, 1))
                .diagnosis("감기")
                .build();

        given(prescriptionRepository.findByAppointmentId(appointmentId))
                .willReturn(Optional.of(prescription));

        // when
        Optional<Prescription> result = prescriptionService.findByRoomId(roomId);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getAppointment().getId()).isEqualTo(appointmentId);
    }

    @Test
    @DisplayName("진행 중인 예약에서 처방전 발행")
    void issuePrescriptionForOngoingAppointment() {
        // given
        Long appointmentId = 1L;
        Long doctorId = 1L;
        String diagnosis = "감기";

        Appointment appointment = Appointment.builder()
                .id(appointmentId)
                .patient(patient)
                .doctor(doctor)
                .date(LocalDateTime.of(2025, 12, 1, 14, 0))
                .status(AppointmentStatus.SCHEDULED)
                .build();

        Prescription newPrescription = Prescription.builder()
                .id(1L)
                .doctor(doctor)
                .patient(patient)
                .appointment(appointment)
                .date(LocalDate.of(2025, 12, 1))
                .diagnosis(diagnosis)
                .build();

        given(appointmentRepository.findByIdWithPatientAndDoctor(appointmentId))
                .willReturn(Optional.of(appointment));
        given(userRepository.findById(doctorId))
                .willReturn(Optional.of(doctor));
        given(userRepository.findById(patient.getId()))
                .willReturn(Optional.of(patient));
        given(prescriptionRepository.save(any(Prescription.class)))
                .willReturn(newPrescription);

        // when
        var result = prescriptionService.issuePrescription(appointmentId, doctorId, patient.getId(), diagnosis);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getDiagnosis()).isEqualTo(diagnosis);
        assertThat(result.getDoctorName()).isEqualTo(doctor.getName());
        assertThat(result.getPatientName()).isEqualTo(patient.getName());
    }

    @Test
    @DisplayName("처방전 발행 시 예약 ID와 연결")
    void shouldLinkPrescriptionWithAppointmentWhenIssued() {
        // given
        Long appointmentId = 1L;
        Long doctorId = 1L;
        String diagnosis = "감기";

        Appointment appointment = Appointment.builder()
                .id(appointmentId)
                .patient(patient)
                .doctor(doctor)
                .date(LocalDateTime.of(2025, 12, 1, 14, 0))
                .status(AppointmentStatus.SCHEDULED)
                .build();

        Prescription savedPrescription = Prescription.builder()
                .id(1L)
                .doctor(doctor)
                .patient(patient)
                .appointment(appointment)
                .date(LocalDate.of(2025, 12, 1))
                .diagnosis(diagnosis)
                .build();

        given(appointmentRepository.findByIdWithPatientAndDoctor(appointmentId))
                .willReturn(Optional.of(appointment));
        given(userRepository.findById(doctorId))
                .willReturn(Optional.of(doctor));
        given(userRepository.findById(patient.getId()))
                .willReturn(Optional.of(patient));
        given(prescriptionRepository.save(any(Prescription.class)))
                .willReturn(savedPrescription);

        // when
        var result = prescriptionService.issuePrescription(appointmentId, doctorId, patient.getId(), diagnosis);

        // then
        assertThat(result.getAppointmentId()).isNotNull();
        assertThat(result.getAppointmentId()).isEqualTo(appointmentId);
    }

    @Test
    @DisplayName("처방전 발행 시 예약 상태 'COMPLETED'로 변경")
    void shouldChangeAppointmentStatusToCompletedWhenIssuingPrescription() {
        // given
        Long appointmentId = 1L;
        Long doctorId = 1L;
        String diagnosis = "감기";

        Appointment appointment = Appointment.builder()
                .id(appointmentId)
                .patient(patient)
                .doctor(doctor)
                .date(LocalDateTime.of(2025, 12, 1, 14, 0))
                .status(AppointmentStatus.SCHEDULED)
                .build();

        Prescription savedPrescription = Prescription.builder()
                .id(1L)
                .doctor(doctor)
                .patient(patient)
                .appointment(appointment)
                .date(LocalDate.of(2025, 12, 1))
                .diagnosis(diagnosis)
                .build();

        given(appointmentRepository.findByIdWithPatientAndDoctor(appointmentId))
                .willReturn(Optional.of(appointment));
        given(userRepository.findById(doctorId))
                .willReturn(Optional.of(doctor));
        given(userRepository.findById(patient.getId()))
                .willReturn(Optional.of(patient));
        given(prescriptionRepository.save(any(Prescription.class)))
                .willReturn(savedPrescription);

        // when
        prescriptionService.issuePrescription(appointmentId, doctorId, patient.getId(), diagnosis);

        // then
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.COMPLETED);
    }

    @Test
    @DisplayName("issuePrescription: 완료된 예약에 중복 처방전 발행 불가")
    void shouldNotAllowDuplicatePrescriptionForCompletedAppointment() {
        // given
        Long appointmentId = 1L;
        Long doctorId = 1L;
        String diagnosis = "감기";

        Appointment completedAppointment = Appointment.builder()
                .id(appointmentId)
                .patient(patient)
                .doctor(doctor)
                .date(LocalDateTime.of(2025, 12, 1, 14, 0))
                .status(AppointmentStatus.COMPLETED)
                .build();

        Prescription existingPrescription = Prescription.builder()
                .id(1L)
                .doctor(doctor)
                .patient(patient)
                .appointment(completedAppointment)
                .date(LocalDate.of(2025, 12, 1))
                .diagnosis("기존 진단")
                .build();

        given(appointmentRepository.findByIdWithPatientAndDoctor(appointmentId))
                .willReturn(Optional.of(completedAppointment));
        given(prescriptionRepository.findByAppointmentId(appointmentId))
                .willReturn(Optional.of(existingPrescription));

        // when & then
        assertThatThrownBy(() ->
                prescriptionService.issuePrescription(appointmentId, doctorId, patient.getId(), diagnosis))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("이미 처방전이 발행된 예약입니다.");
    }

    @Test
    @DisplayName("담당 의사만 처방전 발행 가능")
    void shouldOnlyAllowAssignedDoctorToIssuePrescription() {
        // given
        Long appointmentId = 1L;
        Long unauthorizedDoctorId = 2L;  // anotherDoctor
        String diagnosis = "감기";

        Appointment appointment = Appointment.builder()
                .id(appointmentId)
                .patient(patient)
                .doctor(doctor)  // doctor(id=1)가 담당 의사
                .date(LocalDateTime.of(2025, 12, 1, 14, 0))
                .status(AppointmentStatus.SCHEDULED)
                .build();

        given(appointmentRepository.findByIdWithPatientAndDoctor(appointmentId))
                .willReturn(Optional.of(appointment));

        // when & then
        assertThatThrownBy(() ->
                prescriptionService.issuePrescription(appointmentId, unauthorizedDoctorId, patient.getId(), diagnosis))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("해당 예약의 담당 의사만 처방전을 발행할 수 있습니다.");
    }

    @Test
    @DisplayName("환자 확인 후 처방전 발행")
    void shouldVerifyPatientBeforeIssuingPrescription() {
        // given
        Long appointmentId = 1L;
        Long doctorId = 1L;
        Long wrongPatientId = 999L;  // 다른 환자 ID
        String diagnosis = "감기";

        Appointment appointment = Appointment.builder()
                .id(appointmentId)
                .patient(patient)  // patient(id=3)가 예약 환자
                .doctor(doctor)
                .date(LocalDateTime.of(2025, 12, 1, 14, 0))
                .status(AppointmentStatus.SCHEDULED)
                .build();

        given(appointmentRepository.findByIdWithPatientAndDoctor(appointmentId))
                .willReturn(Optional.of(appointment));

        // when & then
        assertThatThrownBy(() ->
                prescriptionService.issuePrescription(appointmentId, doctorId, wrongPatientId, diagnosis))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("해당 예약의 환자 정보가 일치하지 않습니다.");
    }

    @Test
    @DisplayName("필수 필드 누락 시 예외 발생 - 진단명 null")
    void shouldThrowExceptionWhenDiagnosisIsNull() {
        // given
        Long appointmentId = 1L;
        Long doctorId = 1L;
        String diagnosis = null;

        Appointment appointment = Appointment.builder()
                .id(appointmentId)
                .patient(patient)
                .doctor(doctor)
                .date(LocalDateTime.of(2025, 12, 1, 14, 0))
                .status(AppointmentStatus.SCHEDULED)
                .build();

        given(appointmentRepository.findByIdWithPatientAndDoctor(appointmentId))
                .willReturn(Optional.of(appointment));

        // when & then
        assertThatThrownBy(() ->
                prescriptionService.issuePrescription(appointmentId, doctorId, patient.getId(), diagnosis))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("진단명은 필수입니다.");
    }

    @Test
    @DisplayName("필수 필드 누락 시 예외 발생 - 진단명 empty")
    void shouldThrowExceptionWhenDiagnosisIsEmpty() {
        // given
        Long appointmentId = 1L;
        Long doctorId = 1L;
        String diagnosis = "";

        Appointment appointment = Appointment.builder()
                .id(appointmentId)
                .patient(patient)
                .doctor(doctor)
                .date(LocalDateTime.of(2025, 12, 1, 14, 0))
                .status(AppointmentStatus.SCHEDULED)
                .build();

        given(appointmentRepository.findByIdWithPatientAndDoctor(appointmentId))
                .willReturn(Optional.of(appointment));

        // when & then
        assertThatThrownBy(() ->
                prescriptionService.issuePrescription(appointmentId, doctorId, patient.getId(), diagnosis))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("진단명은 필수입니다.");
    }

    @Test
    @DisplayName("예약 ID로 처방전 Response 조회")
    void getPrescriptionByAppointmentId() {
        // given
        Long appointmentId = 1L;

        Appointment appointment = Appointment.builder()
                .id(appointmentId)
                .patient(patient)
                .doctor(doctor)
                .date(LocalDateTime.of(2025, 12, 1, 14, 0))
                .status(AppointmentStatus.COMPLETED)
                .build();

        Prescription prescriptionWithAppointment = Prescription.builder()
                .id(1L)
                .doctor(doctor)
                .patient(patient)
                .appointment(appointment)
                .date(LocalDate.of(2025, 12, 1))
                .diagnosis("감기")
                .medications("타이레놀 500mg")
                .build();

        given(prescriptionRepository.findByAppointmentId(appointmentId))
                .willReturn(Optional.of(prescriptionWithAppointment));

        // when
        var result = prescriptionService.getPrescriptionByAppointmentId(appointmentId);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getDiagnosis()).isEqualTo("감기");
    }

    @Test
    @DisplayName("처방전 약품 정보 업데이트")
    void updateMedications() {
        // given
        Prescription prescription = Prescription.builder()
                .id(1L)
                .doctor(doctor)
                .patient(patient)
                .date(LocalDate.now())
                .diagnosis("감기")
                .medications("타이레놀 500mg")
                .build();

        String newMedications = "아스피린 100mg";

        // when
        prescription.updateMedications(newMedications);

        // then
        assertThat(prescription.getMedications()).isEqualTo(newMedications);
    }

    @Test
    @DisplayName("createPrescription: 정상적인 처방전 생성")
    void createPrescription_Success() {
        // given
        Long appointmentId = 1L;
        Long doctorId = 1L;
        Long patientId = 3L;

        Appointment appointment = Appointment.builder()
                .id(appointmentId)
                .patient(patient)
                .doctor(doctor)
                .date(LocalDateTime.of(2025, 12, 1, 14, 0))
                .status(AppointmentStatus.IN_PROGRESS)
                .build();

        List<PrescriptionRequest.MedicineItem> medicines = Arrays.asList(
                PrescriptionRequest.MedicineItem.builder()
                        .medicineId(1L)
                        .medicineName("타이레놀")
                        .dosage("1정")
                        .frequency("1일 3회")
                        .durationDays(7)
                        .build()
        );

        PrescriptionRequest request = PrescriptionRequest.builder()
                .appointmentId(appointmentId)
                .patientId(patientId)
                .diagnosis("감기")
                .medicines(medicines)
                .additionalInstructions("식후 복용")
                .build();

        Prescription savedPrescription = Prescription.builder()
                .id(1L)
                .doctor(doctor)
                .patient(patient)
                .appointment(appointment)
                .diagnosis("감기")
                .medications("타이레놀 (1정, 1일 3회, 7일)")
                .instructions("식후 복용")
                .date(LocalDate.now())
                .build();

        given(appointmentRepository.findByIdWithPatientAndDoctor(appointmentId))
                .willReturn(Optional.of(appointment));
        given(userRepository.findById(doctorId))
                .willReturn(Optional.of(doctor));
        given(userRepository.findById(patientId))
                .willReturn(Optional.of(patient));
        given(prescriptionRepository.findByAppointmentId(appointmentId))
                .willReturn(Optional.empty());
        given(prescriptionRepository.save(any(Prescription.class)))
                .willReturn(savedPrescription);

        // when
        PrescriptionResponse result = prescriptionService.createPrescription(doctorId, request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getDiagnosis()).isEqualTo("감기");
        assertThat(result.getMedications()).contains("타이레놀");
        assertThat(result.getInstructions()).isEqualTo("식후 복용");
    }

    @Test
    @DisplayName("createPrescription: 약품 정보 포맷팅 - 모든 정보 포함")
    void createPrescription_MedicationFormatting_AllFields() {
        // given
        Long appointmentId = 1L;
        Appointment appointment = Appointment.builder()
                .id(appointmentId)
                .patient(patient)
                .doctor(doctor)
                .date(LocalDateTime.of(2025, 12, 1, 14, 0))
                .status(AppointmentStatus.IN_PROGRESS)
                .build();

        List<PrescriptionRequest.MedicineItem> medicines = Arrays.asList(
                PrescriptionRequest.MedicineItem.builder()
                        .medicineId(1L)
                        .medicineName("타이레놀")
                        .dosage("1정")
                        .frequency("1일 3회")
                        .durationDays(7)
                        .build()
        );

        PrescriptionRequest request = PrescriptionRequest.builder()
                .appointmentId(appointmentId)
                .patientId(patient.getId())
                .diagnosis("감기")
                .medicines(medicines)
                .build();

        given(appointmentRepository.findByIdWithPatientAndDoctor(appointmentId))
                .willReturn(Optional.of(appointment));
        given(userRepository.findById(doctor.getId()))
                .willReturn(Optional.of(doctor));
        given(userRepository.findById(patient.getId()))
                .willReturn(Optional.of(patient));
        given(prescriptionRepository.findByAppointmentId(appointmentId))
                .willReturn(Optional.empty());
        given(prescriptionRepository.save(any(Prescription.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        PrescriptionResponse result = prescriptionService.createPrescription(doctor.getId(), request);

        // then
        assertThat(result.getMedications()).isEqualTo("타이레놀 (1정, 1일 3회, 7일)");
    }

    @Test
    @DisplayName("createPrescription: 약품 정보 포맷팅 - 부분 정보만")
    void createPrescription_MedicationFormatting_PartialFields() {
        // given
        Long appointmentId = 1L;
        Appointment appointment = Appointment.builder()
                .id(appointmentId)
                .patient(patient)
                .doctor(doctor)
                .date(LocalDateTime.of(2025, 12, 1, 14, 0))
                .status(AppointmentStatus.IN_PROGRESS)
                .build();

        List<PrescriptionRequest.MedicineItem> medicines = Arrays.asList(
                PrescriptionRequest.MedicineItem.builder()
                        .medicineId(1L)
                        .medicineName("아스피린")
                        .frequency("1일 2회")
                        .build()
        );

        PrescriptionRequest request = PrescriptionRequest.builder()
                .appointmentId(appointmentId)
                .patientId(patient.getId())
                .diagnosis("통증")
                .medicines(medicines)
                .build();

        given(appointmentRepository.findByIdWithPatientAndDoctor(appointmentId))
                .willReturn(Optional.of(appointment));
        given(userRepository.findById(doctor.getId()))
                .willReturn(Optional.of(doctor));
        given(userRepository.findById(patient.getId()))
                .willReturn(Optional.of(patient));
        given(prescriptionRepository.findByAppointmentId(appointmentId))
                .willReturn(Optional.empty());
        given(prescriptionRepository.save(any(Prescription.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        PrescriptionResponse result = prescriptionService.createPrescription(doctor.getId(), request);

        // then
        assertThat(result.getMedications()).isEqualTo("아스피린 (1일 2회)");
    }

    @Test
    @DisplayName("createPrescription: 기존 처방전 업데이트")
    void createPrescription_UpdateExisting() {
        // given
        Long appointmentId = 1L;
        Appointment appointment = Appointment.builder()
                .id(appointmentId)
                .patient(patient)
                .doctor(doctor)
                .date(LocalDateTime.of(2025, 12, 1, 14, 0))
                .status(AppointmentStatus.IN_PROGRESS)
                .build();

        Prescription existingPrescription = Prescription.builder()
                .id(1L)
                .doctor(doctor)
                .patient(patient)
                .appointment(appointment)
                .diagnosis("기존 진단")
                .medications("기존 약품")
                .instructions("기존 지침")
                .date(LocalDate.now())
                .build();

        List<PrescriptionRequest.MedicineItem> medicines = Arrays.asList(
                PrescriptionRequest.MedicineItem.builder()
                        .medicineId(2L)
                        .medicineName("새로운 약")
                        .dosage("2정")
                        .frequency("1일 2회")
                        .durationDays(5)
                        .build()
        );

        PrescriptionRequest request = PrescriptionRequest.builder()
                .appointmentId(appointmentId)
                .patientId(patient.getId())
                .diagnosis("새로운 진단")
                .medicines(medicines)
                .additionalInstructions("새로운 지침")
                .build();

        given(appointmentRepository.findByIdWithPatientAndDoctor(appointmentId))
                .willReturn(Optional.of(appointment));
        given(userRepository.findById(doctor.getId()))
                .willReturn(Optional.of(doctor));
        given(userRepository.findById(patient.getId()))
                .willReturn(Optional.of(patient));
        given(prescriptionRepository.findByAppointmentId(appointmentId))
                .willReturn(Optional.of(existingPrescription));
        given(prescriptionRepository.save(any(Prescription.class)))
                .willReturn(existingPrescription);

        // when
        PrescriptionResponse result = prescriptionService.createPrescription(doctor.getId(), request);

        // then
        assertThat(existingPrescription.getDiagnosis()).isEqualTo("새로운 진단");
        assertThat(existingPrescription.getMedications()).isEqualTo("새로운 약 (2정, 1일 2회, 5일)");
        assertThat(existingPrescription.getInstructions()).isEqualTo("새로운 지침");
    }

    @Test
    @DisplayName("createPrescription: 담당 의사가 아닌 경우 예외")
    void createPrescription_UnauthorizedDoctor() {
        // given
        Long appointmentId = 1L;
        Long unauthorizedDoctorId = 2L;

        Appointment appointment = Appointment.builder()
                .id(appointmentId)
                .patient(patient)
                .doctor(doctor)  // doctor.id = 1
                .date(LocalDateTime.of(2025, 12, 1, 14, 0))
                .status(AppointmentStatus.IN_PROGRESS)
                .build();

        PrescriptionRequest request = PrescriptionRequest.builder()
                .appointmentId(appointmentId)
                .patientId(patient.getId())
                .diagnosis("감기")
                .medicines(Arrays.asList())
                .build();

        given(appointmentRepository.findByIdWithPatientAndDoctor(appointmentId))
                .willReturn(Optional.of(appointment));

        // when & then
        assertThatThrownBy(() ->
                prescriptionService.createPrescription(unauthorizedDoctorId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("해당 예약의 담당 의사만 처방전을 발행할 수 있습니다.");
    }

    @Test
    @DisplayName("createPrescription: 환자 정보 불일치 예외")
    void createPrescription_PatientMismatch() {
        // given
        Long appointmentId = 1L;
        Long wrongPatientId = 999L;

        Appointment appointment = Appointment.builder()
                .id(appointmentId)
                .patient(patient)  // patient.id = 3
                .doctor(doctor)
                .date(LocalDateTime.of(2025, 12, 1, 14, 0))
                .status(AppointmentStatus.IN_PROGRESS)
                .build();

        PrescriptionRequest request = PrescriptionRequest.builder()
                .appointmentId(appointmentId)
                .patientId(wrongPatientId)
                .diagnosis("감기")
                .medicines(Arrays.asList())
                .build();

        given(appointmentRepository.findByIdWithPatientAndDoctor(appointmentId))
                .willReturn(Optional.of(appointment));

        // when & then
        assertThatThrownBy(() ->
                prescriptionService.createPrescription(doctor.getId(), request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("해당 예약의 환자 정보가 일치하지 않습니다.");
    }

    @Test
    @DisplayName("createPrescription: 예약을 찾을 수 없는 경우")
    void createPrescription_AppointmentNotFound() {
        // given
        Long appointmentId = 999L;

        PrescriptionRequest request = PrescriptionRequest.builder()
                .appointmentId(appointmentId)
                .patientId(patient.getId())
                .diagnosis("감기")
                .medicines(Arrays.asList())
                .build();

        given(appointmentRepository.findByIdWithPatientAndDoctor(appointmentId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
                prescriptionService.createPrescription(doctor.getId(), request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("예약을 찾을 수 없습니다.");
    }
}
