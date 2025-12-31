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
        System.out.println("========================================");
        System.out.println("[처방전 발급] 시작 - Service 진입");
        System.out.println("요청 데이터: 의사ID=" + doctorId + ", 예약ID=" + request.getAppointmentId() + ", 환자ID=" + request.getPatientId());

        try {
            // 1. 예약 조회
            Appointment appointment = appointmentRepository.findByIdWithPatientAndDoctor(request.getAppointmentId())
                    .orElseThrow(() -> new IllegalArgumentException("예약을 찾을 수 없습니다. ID: " + request.getAppointmentId()));

            // 2. 중복 발급 체크
            if (prescriptionRepository.findByAppointmentId(request.getAppointmentId()).isPresent()) {
                throw new IllegalStateException("이미 처방전이 발급된 예약입니다. AppointmentID: " + request.getAppointmentId());
            }

            // 3. 권한 및 일치 여부 검증 (추가된 로직)
            if (!appointment.getDoctor().getId().equals(doctorId)) {
                throw new IllegalArgumentException("해당 예약의 담당 의사가 아닙니다. (예약 의사 ID: " + appointment.getDoctor().getId() + ")");
            }
            if (!appointment.getPatient().getId().equals(request.getPatientId())) {
                throw new IllegalArgumentException("예약의 환자 정보와 요청된 환자 정보가 일치하지 않습니다. (예약 환자 ID: " + appointment.getPatient().getId() + ")");
            }

            // 4. 사용자(의사/환자) 조회
            User doctor = userRepository.findById(doctorId)
                    .orElseThrow(() -> new IllegalArgumentException("의사를 찾을 수 없습니다. ID: " + doctorId));
            User patient = userRepository.findById(request.getPatientId())
                    .orElseThrow(() -> new IllegalArgumentException("환자를 찾을 수 없습니다. ID: " + request.getPatientId()));

            // 5. 처방전 생성
            Prescription prescription = Prescription.builder()
                    .doctor(doctor)
                    .patient(patient)
                    .appointment(appointment)
                    .diagnosis(request.getDiagnosis())
                    .instructions(request.getAdditionalInstructions())
                    .date(LocalDate.now())
                    .build();

            // 6. 약품 추가
            if (request.getMedicines() == null || request.getMedicines().isEmpty()) {
                throw new IllegalArgumentException("처방할 약품이 없습니다.");
            }

            for (PrescriptionRequest.MedicineItem item : request.getMedicines()) {
                Medicine medicine;
                if (item.getMedicineId() != null) {
                    medicine = medicineRepository.findById(item.getMedicineId())
                            .orElseThrow(() -> new IllegalArgumentException("약품 ID를 찾을 수 없습니다: " + item.getMedicineId()));
                } else {
                    medicine = medicineRepository.findByName(item.getMedicineName())
                            .orElseThrow(() -> new IllegalArgumentException("약품명으로 찾을 수 없습니다: " + item.getMedicineName()));
                }

                // [수정] 자식 엔티티(PrescriptionMedicine)에 부모(Prescription)를 명시적으로 주입
                // 이것이 누락되면 FK가 null이 되어 500 에러가 발생합니다.
                PrescriptionMedicine pm = PrescriptionMedicine.builder()
                        .medicine(medicine)
                        .instructions(item.getInstructions())
                        .prescription(prescription) // ★ 핵심 수정: 부모 엔티티 참조 설정
                        .build();
                
                prescription.addMedicine(pm);
                System.out.println("  - 약품 추가: " + medicine.getName() + " (ID: " + medicine.getId() + ")");
            }

            // 7. 예약 상태 완료로 변경
            appointment.complete();
            System.out.println("✓ 예약 상태 '완료'로 변경됨");

            // 8. DB 저장 및 Flush (예외 포착을 위해 saveAndFlush 사용)
            Prescription savedPrescription = prescriptionRepository.saveAndFlush(prescription);
            System.out.println("✓ 처방전 DB 저장 성공 (ID: " + savedPrescription.getId() + ")");
            System.out.println("========================================");

            return PrescriptionResponse.from(savedPrescription);

        } catch (Exception e) {
            System.err.println("!!! [처방전 발급 중 에러 발생] !!!");
            System.err.println("에러 타입: " + e.getClass().getName());
            System.err.println("에러 메시지: " + e.getMessage());
            e.printStackTrace(); // 서버 로그에 상세 스택트레이스 출력
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
