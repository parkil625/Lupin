package com.example.demo.service;

import com.example.demo.domain.entity.Prescription;
import com.example.demo.domain.entity.User;
import com.example.demo.dto.request.PrescriptionCreateRequest;
import com.example.demo.dto.response.PrescriptionResponse;
import com.example.demo.exception.BusinessException;
import com.example.demo.repository.PrescriptionMedRepository;
import com.example.demo.repository.PrescriptionRepository;
import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PrescriptionService 테스트")
class PrescriptionServiceTest {

    @InjectMocks
    private PrescriptionService prescriptionService;

    @Mock
    private PrescriptionRepository prescriptionRepository;

    @Mock
    private PrescriptionMedRepository prescriptionMedRepository;

    @Mock
    private UserRepository userRepository;

    @Test
    @DisplayName("처방전 생성 성공")
    void createPrescription_Success() {
        // given
        User patient = User.builder().id(1L).userId("patient").build();
        User doctor = User.builder().id(2L).userId("doctor").build();

        PrescriptionCreateRequest request = PrescriptionCreateRequest.builder()
                .patientId(1L)
                .doctorId(2L)
                .prescriptionName("감기약 처방")
                .diagnosis("감기")
                .instructions("하루 3회 복용")
                .prescribedDate(LocalDate.now())
                .build();

        given(userRepository.findById(1L)).willReturn(Optional.of(patient));
        given(userRepository.findById(2L)).willReturn(Optional.of(doctor));
        given(prescriptionRepository.save(any(Prescription.class))).willAnswer(invocation -> {
            Prescription p = invocation.getArgument(0);
            return Prescription.builder()
                    .id(1L)
                    .prescriptionName(p.getPrescriptionName())
                    .diagnosis(p.getDiagnosis())
                    .instructions(p.getInstructions())
                    .patient(p.getPatient())
                    .doctor(p.getDoctor())
                    .build();
        });

        // when
        PrescriptionResponse result = prescriptionService.createPrescription(request);

        // then
        assertThat(result).isNotNull();
        then(prescriptionRepository).should().save(any(Prescription.class));
    }

    @Test
    @DisplayName("처방전 생성 실패 - 환자 없음")
    void createPrescription_PatientNotFound() {
        // given
        PrescriptionCreateRequest request = PrescriptionCreateRequest.builder()
                .patientId(1L)
                .doctorId(2L)
                .build();

        given(userRepository.findById(1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> prescriptionService.createPrescription(request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("환자 처방전 목록 조회 - 페이징")
    void getPrescriptionsByPatientId_Paged_Success() {
        // given
        User patient = User.builder().id(1L).userId("patient").build();
        User doctor = User.builder().id(2L).userId("doctor").build();
        Prescription prescription = Prescription.builder()
                .id(1L)
                .prescriptionName("처방전1")
                .patient(patient)
                .doctor(doctor)
                .build();
        Page<Prescription> page = new PageImpl<>(Arrays.asList(prescription));
        Pageable pageable = PageRequest.of(0, 10);

        given(prescriptionRepository.findByPatientId(1L, pageable)).willReturn(page);

        // when
        Page<PrescriptionResponse> result = prescriptionService.getPrescriptionsByPatientId(1L, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("환자 처방전 목록 조회 - 전체")
    void getAllPrescriptionsByPatientId_Success() {
        // given
        User patient = User.builder().id(1L).userId("patient").build();
        User doctor = User.builder().id(2L).userId("doctor").build();
        Prescription prescription = Prescription.builder()
                .id(1L)
                .prescriptionName("처방전1")
                .patient(patient)
                .doctor(doctor)
                .build();

        given(prescriptionRepository.findByPatientId(1L)).willReturn(Arrays.asList(prescription));

        // when
        List<PrescriptionResponse> result = prescriptionService.getAllPrescriptionsByPatientId(1L);

        // then
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("의사 처방전 목록 조회 - 페이징")
    void getPrescriptionsByDoctorId_Paged_Success() {
        // given
        User patient = User.builder().id(1L).userId("patient").build();
        User doctor = User.builder().id(2L).userId("doctor").build();
        Prescription prescription = Prescription.builder()
                .id(1L)
                .prescriptionName("처방전1")
                .patient(patient)
                .doctor(doctor)
                .build();
        Page<Prescription> page = new PageImpl<>(Arrays.asList(prescription));
        Pageable pageable = PageRequest.of(0, 10);

        given(prescriptionRepository.findByDoctorId(2L, pageable)).willReturn(page);

        // when
        Page<PrescriptionResponse> result = prescriptionService.getPrescriptionsByDoctorId(2L, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("의사 처방전 목록 조회 - 전체")
    void getAllPrescriptionsByDoctorId_Success() {
        // given
        User patient = User.builder().id(1L).userId("patient").build();
        User doctor = User.builder().id(2L).userId("doctor").build();
        Prescription prescription = Prescription.builder()
                .id(1L)
                .prescriptionName("처방전1")
                .patient(patient)
                .doctor(doctor)
                .build();

        given(prescriptionRepository.findByDoctorId(2L)).willReturn(Arrays.asList(prescription));

        // when
        List<PrescriptionResponse> result = prescriptionService.getAllPrescriptionsByDoctorId(2L);

        // then
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("처방전 상세 조회 성공")
    void getPrescriptionDetail_Success() {
        // given
        User patient = User.builder().id(1L).userId("patient").build();
        User doctor = User.builder().id(2L).userId("doctor").build();
        Prescription prescription = Prescription.builder()
                .id(1L)
                .prescriptionName("처방전1")
                .patient(patient)
                .doctor(doctor)
                .build();

        given(prescriptionRepository.findById(1L)).willReturn(Optional.of(prescription));

        // when
        PrescriptionResponse result = prescriptionService.getPrescriptionDetail(1L);

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("처방전 상세 조회 실패 - 처방전 없음")
    void getPrescriptionDetail_NotFound() {
        // given
        given(prescriptionRepository.findById(1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> prescriptionService.getPrescriptionDetail(1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("최근 처방전 조회")
    void getRecentPrescriptionsByPatientId_Success() {
        // given
        User patient = User.builder().id(1L).userId("patient").build();
        User doctor = User.builder().id(2L).userId("doctor").build();
        Prescription prescription = Prescription.builder()
                .id(1L)
                .prescriptionName("처방전1")
                .patient(patient)
                .doctor(doctor)
                .build();

        given(prescriptionRepository.findRecentPrescriptionsByPatientId(eq(1L), any(Pageable.class)))
                .willReturn(Arrays.asList(prescription));

        // when
        List<PrescriptionResponse> result = prescriptionService.getRecentPrescriptionsByPatientId(1L, 5);

        // then
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("기간별 처방전 조회")
    void getPrescriptionsByDateRange_Success() {
        // given
        User patient = User.builder().id(1L).userId("patient").build();
        User doctor = User.builder().id(2L).userId("doctor").build();
        Prescription prescription = Prescription.builder()
                .id(1L)
                .prescriptionName("처방전1")
                .patient(patient)
                .doctor(doctor)
                .build();

        LocalDate start = LocalDate.now().minusDays(7);
        LocalDate end = LocalDate.now();

        given(prescriptionRepository.findByDateRange(start, end))
                .willReturn(Arrays.asList(prescription));

        // when
        List<PrescriptionResponse> result = prescriptionService.getPrescriptionsByDateRange(start, end);

        // then
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("처방전 이름 검색")
    void searchPrescriptionsByName_Success() {
        // given
        User patient = User.builder().id(1L).userId("patient").build();
        User doctor = User.builder().id(2L).userId("doctor").build();
        Prescription prescription = Prescription.builder()
                .id(1L)
                .prescriptionName("감기약")
                .patient(patient)
                .doctor(doctor)
                .build();
        Page<Prescription> page = new PageImpl<>(Arrays.asList(prescription));
        Pageable pageable = PageRequest.of(0, 10);

        given(prescriptionRepository.searchByPrescriptionName("감기", pageable)).willReturn(page);

        // when
        Page<PrescriptionResponse> result = prescriptionService.searchPrescriptionsByName("감기", pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("처방전 삭제 성공")
    void deletePrescription_Success() {
        // given
        User doctor = User.builder().id(2L).userId("doctor").build();
        Prescription prescription = Prescription.builder()
                .id(1L)
                .doctor(doctor)
                .build();

        given(prescriptionRepository.findById(1L)).willReturn(Optional.of(prescription));

        // when
        prescriptionService.deletePrescription(1L, 2L);

        // then
        then(prescriptionRepository).should().delete(prescription);
    }

    @Test
    @DisplayName("처방전 삭제 실패 - 권한 없음")
    void deletePrescription_Forbidden() {
        // given
        User doctor = User.builder().id(2L).userId("doctor").build();
        Prescription prescription = Prescription.builder()
                .id(1L)
                .doctor(doctor)
                .build();

        given(prescriptionRepository.findById(1L)).willReturn(Optional.of(prescription));

        // when & then
        assertThatThrownBy(() -> prescriptionService.deletePrescription(1L, 3L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("환자 처방전 수 조회")
    void getPrescriptionCountByPatientId_Success() {
        // given
        given(prescriptionRepository.countByPatientId(1L)).willReturn(5L);

        // when
        Long result = prescriptionService.getPrescriptionCountByPatientId(1L);

        // then
        assertThat(result).isEqualTo(5L);
    }
}
