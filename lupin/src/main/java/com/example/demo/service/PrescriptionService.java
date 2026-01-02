package com.example.demo.service;

import com.example.demo.domain.entity.*;
import com.example.demo.domain.enums.AppointmentStatus;
import com.example.demo.dto.prescription.PrescriptionRequest;
import com.example.demo.dto.prescription.PrescriptionResponse;
import com.example.demo.repository.AppointmentRepository;
import com.example.demo.repository.MedicineRepository;
import com.example.demo.repository.PrescriptionRepository;
import com.example.demo.repository.UserRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;
    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final MedicineRepository medicineRepository;
    private final EntityManager entityManager;

    @Transactional
    public void updateDiagnosis(Long prescriptionId, Long doctorId, String newDiagnosis) {
        Prescription prescription = prescriptionRepository.findById(prescriptionId)
                .orElseThrow(() -> new IllegalArgumentException("처방전을 찾을 수 없습니다."));

        if (!prescription.getDoctor().getId().equals(doctorId)) {
            throw new IllegalArgumentException("처방전을 수정할 권한이 없습니다.");
        }

        prescription.updateDiagnosis(newDiagnosis);
    }

    @Transactional
    public void deletePrescription(Long prescriptionId, Long doctorId) {
        Prescription prescription = prescriptionRepository.findById(prescriptionId)
                .orElseThrow(() -> new IllegalArgumentException("처방전을 찾을 수 없습니다."));

        if (!prescription.getDoctor().getId().equals(doctorId)) {
            throw new IllegalArgumentException("처방전을 삭제할 권한이 없습니다.");
        }

        prescriptionRepository.delete(prescription);
    }

    public Optional<Prescription> findByAppointmentId(Long appointmentId) {
        return prescriptionRepository.findByAppointmentId(appointmentId);
    }

    public Optional<Prescription> findByRoomId(String roomId) {
        if (roomId == null || !roomId.startsWith("appointment_")) {
            return Optional.empty();
        }

        try {
            String idPart = roomId.substring("appointment_".length());
            Long appointmentId = Long.parseLong(idPart);

            return findByAppointmentId(appointmentId);
        }catch (NumberFormatException e) {
            return Optional.empty();
        }

    }

    @Transactional
    public PrescriptionResponse issuePrescription(Long appointmentId, Long doctorId, Long patientId, String diagnosis) {
        Appointment appointment = appointmentRepository.findByIdWithPatientAndDoctor(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("예약을 찾을 수 없습니다."));

        // 담당 의사 검증
        if (!appointment.getDoctor().getId().equals(doctorId)) {
            throw new IllegalArgumentException("해당 예약의 담당 의사만 처방전을 발행할 수 있습니다.");
        }

        // 환자 정보 검증
        if (!appointment.getPatient().getId().equals(patientId)) {
            throw new IllegalArgumentException("해당 예약의 환자 정보가 일치하지 않습니다.");
        }

        // 필수 필드 검증.
        if (diagnosis == null || diagnosis.trim().isEmpty()) {
            throw new IllegalArgumentException("진단명은 필수입니다.");
        }

        // 중복 처방전 발행 방지
        Optional<Prescription> existingPrescription = prescriptionRepository.findByAppointmentId(appointmentId);
        if (existingPrescription.isPresent()) {
            throw new IllegalStateException("이미 처방전이 발행된 예약입니다.");
        }

        User doctor = userRepository.findById(doctorId)
                .orElseThrow(() -> new IllegalArgumentException("의사를 찾을 수 없습니다."));

        User patient = userRepository.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("환자를 찾을 수 없습니다."));

        Prescription prescription = Prescription.builder()
                .doctor(doctor)
                .patient(patient)
                .appointment(appointment)
                .diagnosis(diagnosis)
                .date(LocalDate.now())
                .build();

        appointment.complete();

        Prescription saved = prescriptionRepository.save(prescription);

        return PrescriptionResponse.from(saved);
    }

    @Transactional
    public PrescriptionResponse createPrescription(Long doctorId, PrescriptionRequest request) {
        // [디버깅 로그 강화]
        System.out.println("========================================");
        System.out.println("[처방전 발급] 프로세스 시작 - JPA Cascade 방식");
        System.out.println("요청 데이터: 의사ID=" + doctorId + ", 예약ID=" + request.getAppointmentId());

        try {
            // 1. 예약 조회
            Appointment appointment = appointmentRepository.findByIdWithPatientAndDoctor(request.getAppointmentId())
                    .orElseThrow(() -> new IllegalArgumentException("예약을 찾을 수 없습니다. ID: " + request.getAppointmentId()));

            // 2. 중복 발급 체크
            if (prescriptionRepository.findByAppointmentId(request.getAppointmentId()).isPresent()) {
                throw new IllegalStateException("이미 처방전이 발급된 예약입니다.");
            }

            // 3. 권한 및 일치 여부 검증
            if (!appointment.getDoctor().getId().equals(doctorId)) {
                throw new IllegalArgumentException("해당 예약의 담당 의사가 아닙니다.");
            }
            if (!appointment.getPatient().getId().equals(request.getPatientId())) {
                throw new IllegalArgumentException("예약의 환자 정보와 요청된 환자 정보가 일치하지 않습니다.");
            }

            // 4. 사용자(의사/환자) 조회
            User doctor = userRepository.findById(doctorId)
                    .orElseThrow(() -> new IllegalArgumentException("의사를 찾을 수 없습니다. ID: " + doctorId));
            User patient = userRepository.findById(request.getPatientId())
                    .orElseThrow(() -> new IllegalArgumentException("환자를 찾을 수 없습니다. ID: " + request.getPatientId()));

            // 5. 처방전(부모) 객체 생성 (아직 저장 전)
            Prescription prescription = Prescription.builder()
                    .doctor(doctor)
                    .patient(patient)
                    .appointment(appointment)
                    .diagnosis(request.getDiagnosis())
                    .instructions(request.getAdditionalInstructions())
                    .date(LocalDate.now())
                    .medicines(new ArrayList<>()) // 리스트 초기화
                    .build();

            // 6. 약품(자식) 객체 생성 및 관계 설정
            if (request.getMedicines() == null || request.getMedicines().isEmpty()) {
                throw new IllegalArgumentException("처방할 약품이 없습니다.");
            }

            for (PrescriptionRequest.MedicineItem item : request.getMedicines()) {
                Medicine medicine;
                // 약품 조회
                if (item.getMedicineId() != null) {
                    medicine = medicineRepository.findById(item.getMedicineId())
                            .orElseThrow(() -> new IllegalArgumentException("약품 ID 오류: " + item.getMedicineId()));
                } else {
                    medicine = medicineRepository.findByName(item.getMedicineName())
                            .orElseThrow(() -> new IllegalArgumentException("약품명 오류: " + item.getMedicineName()));
                }

                // 처방 약품 객체 생성
                PrescriptionMedicine pm = PrescriptionMedicine.builder()
                        .medicine(medicine)
                        .instructions(item.getInstructions())
                        .prescription(prescription) // 부모 참조 설정
                        .build();

                // [핵심] 부모 엔티티의 리스트에 추가 (Cascade 설정에 의해 함께 저장됨)
                prescription.addMedicine(pm);
                System.out.println("   - 약품 추가 대기: " + medicine.getName());
            }

            // 7. 통합 저장 (부모만 저장하면 자식들도 자동 저장됨)
            // EntityManager 관련 코드(persist, flush, clear) 제거하여 트랜잭션 안전성 확보
            Prescription savedPrescription = prescriptionRepository.save(prescription);

            System.out.println("✓ 처방전 및 약품 저장 완료 (ID: " + savedPrescription.getId() + ")");
            System.out.println("========================================");

            return PrescriptionResponse.from(savedPrescription);

        } catch (Exception e) {
            System.err.println("!!! [처방전 발급 중 에러 발생] !!!");
            System.err.println("에러 타입: " + e.getClass().getName());
            System.err.println("에러 메시지: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    public List<PrescriptionResponse> getPatientPrescriptions(Long patientId) {
        List<Prescription> prescriptions = prescriptionRepository.findByPatientIdOrderByDateDesc(patientId);
        return prescriptions.stream()
                .map(PrescriptionResponse::from)
                .collect(Collectors.toList());
    }

    public List<PrescriptionResponse> getDoctorPrescriptions(Long doctorId) {
        List<Prescription> prescriptions = prescriptionRepository.findByDoctorIdOrderByDateDesc(doctorId);
        return prescriptions.stream()
                .map(PrescriptionResponse::from)
                .collect(Collectors.toList());
    }

    public Optional<PrescriptionResponse> getPrescriptionById(Long id) {
        return prescriptionRepository.findById(id)
                .map(PrescriptionResponse::from);
    }

    public Optional<PrescriptionResponse> getPrescriptionByAppointmentId(Long appointmentId) {
        return prescriptionRepository.findByAppointmentId(appointmentId)
                .map(PrescriptionResponse::from);
    }
}
