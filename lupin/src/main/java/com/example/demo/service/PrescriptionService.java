package com.example.demo.service;

import com.example.demo.domain.entity.Prescription;
import com.example.demo.domain.entity.PrescriptionMed;
import com.example.demo.domain.entity.User;
import com.example.demo.dto.request.PrescriptionCreateRequest;
import com.example.demo.dto.response.PrescriptionResponse;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.PrescriptionMedRepository;
import com.example.demo.repository.PrescriptionRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 처방전 관련 비즈니스 로직을 처리하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;
    private final PrescriptionMedRepository prescriptionMedRepository;
    private final UserRepository userRepository;

    /**
     * 처방전 생성
     */
    @Transactional
    public PrescriptionResponse createPrescription(PrescriptionCreateRequest request) {
        User patient = findUserById(request.getPatientId());
        User doctor = findUserById(request.getDoctorId());

        Prescription prescription = Prescription.builder()
                .prescriptionName(request.getPrescriptionName())
                .diagnosis(request.getDiagnosis())
                .instructions(request.getInstructions())
                .prescribedDate(request.getPrescribedDate())
                .patient(patient)
                .doctor(doctor)
                .build();

        // 약품 추가
        if (request.getMedicines() != null && !request.getMedicines().isEmpty()) {
            for (PrescriptionCreateRequest.MedicineDto medicineDto : request.getMedicines()) {
                PrescriptionMed medicine = PrescriptionMed.builder()
                        .medicineName(medicineDto.getMedicineName())
                        .dosage(medicineDto.getDosage())
                        .frequency(medicineDto.getFrequency())
                        .build();

                prescription.addMedicine(medicine);
            }
        }

        Prescription savedPrescription = prescriptionRepository.save(prescription);

        log.info("처방전 생성 완료 - prescriptionId: {}, patientId: {}, doctorId: {}",
                savedPrescription.getId(), request.getPatientId(), request.getDoctorId());

        return PrescriptionResponse.from(savedPrescription);
    }

    /**
     * 특정 환자의 처방전 목록 조회 (페이징)
     */
    public Page<PrescriptionResponse> getPrescriptionsByPatientId(Long patientId, Pageable pageable) {
        return prescriptionRepository.findByPatientId(patientId, pageable)
                .map(PrescriptionResponse::from);
    }

    /**
     * 특정 환자의 처방전 목록 조회 (전체)
     */
    public List<PrescriptionResponse> getAllPrescriptionsByPatientId(Long patientId) {
        return prescriptionRepository.findByPatientId(patientId)
                .stream()
                .map(PrescriptionResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 특정 의사가 발행한 처방전 목록 조회 (페이징)
     */
    public Page<PrescriptionResponse> getPrescriptionsByDoctorId(Long doctorId, Pageable pageable) {
        return prescriptionRepository.findByDoctorId(doctorId, pageable)
                .map(PrescriptionResponse::from);
    }

    /**
     * 특정 의사가 발행한 처방전 목록 조회 (전체)
     */
    public List<PrescriptionResponse> getAllPrescriptionsByDoctorId(Long doctorId) {
        return prescriptionRepository.findByDoctorId(doctorId)
                .stream()
                .map(PrescriptionResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 처방전 상세 조회
     */
    public PrescriptionResponse getPrescriptionDetail(Long prescriptionId) {
        Prescription prescription = findPrescriptionById(prescriptionId);
        return PrescriptionResponse.from(prescription);
    }

    /**
     * 특정 환자의 최근 처방전 조회
     */
    public List<PrescriptionResponse> getRecentPrescriptionsByPatientId(Long patientId, int limit) {
        return prescriptionRepository.findRecentPrescriptionsByPatientId(patientId, PageRequest.of(0, limit))
                .stream()
                .map(PrescriptionResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 특정 기간 내 처방전 조회
     */
    public List<PrescriptionResponse> getPrescriptionsByDateRange(LocalDate startDate, LocalDate endDate) {
        return prescriptionRepository.findByDateRange(startDate, endDate)
                .stream()
                .map(PrescriptionResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 처방전 이름으로 검색
     */
    public Page<PrescriptionResponse> searchPrescriptionsByName(String keyword, Pageable pageable) {
        return prescriptionRepository.searchByPrescriptionName(keyword, pageable)
                .map(PrescriptionResponse::from);
    }

    /**
     * 처방전 삭제
     */
    @Transactional
    public void deletePrescription(Long prescriptionId, Long doctorId) {
        Prescription prescription = findPrescriptionById(prescriptionId);

        // 발행 의사 확인
        if (!prescription.getDoctor().getId().equals(doctorId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "처방전을 삭제할 권한이 없습니다.");
        }

        prescriptionRepository.delete(prescription);

        log.info("처방전 삭제 완료 - prescriptionId: {}, doctorId: {}", prescriptionId, doctorId);
    }

    /**
     * 특정 환자의 처방전 수 조회
     */
    public Long getPrescriptionCountByPatientId(Long patientId) {
        return prescriptionRepository.countByPatientId(patientId);
    }

    // === 헬퍼 메서드 ===

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private Prescription findPrescriptionById(Long prescriptionId) {
        return prescriptionRepository.findById(prescriptionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRESCRIPTION_NOT_FOUND));
    }
}
