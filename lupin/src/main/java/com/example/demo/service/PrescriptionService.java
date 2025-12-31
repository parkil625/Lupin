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
        System.out.println("[처방전 발급] 시작");
        System.out.println("요청 - 의사ID: " + doctorId + ", 예약ID: " + request.getAppointmentId() + ", 환자ID: " + request.getPatientId());
        System.out.println("진단: " + request.getDiagnosis());

        Appointment appointment = appointmentRepository.findByIdWithPatientAndDoctor(request.getAppointmentId())
                .orElseThrow(() -> new IllegalArgumentException("예약을 찾을 수 없습니다."));

        Optional<Prescription> existingPrescription = prescriptionRepository.findByAppointmentId(request.getAppointmentId());
        if (existingPrescription.isPresent()) {
            throw new IllegalStateException("이미 처방전이 발급된 예약입니다.");
        }

        System.out.println("✓ 예약 조회 성공 - ID: " + appointment.getId());

        User doctor = userRepository.findById(doctorId)
                .orElseThrow(() -> new IllegalArgumentException("의사를 찾을 수 없습니다."));
        System.out.println("✓ 의사 조회 성공 - ID: " + doctor.getId() + ", 이름: " + doctor.getName());

        User patient = userRepository.findById(request.getPatientId())
                .orElseThrow(() -> new IllegalArgumentException("환자를 찾을 수 없습니다."));
        System.out.println("✓ 환자 조회 성공 - ID: " + patient.getId() + ", 이름: " + patient.getName());

        // 처방전 엔티티 생성
        Prescription prescription = Prescription.builder()
                .doctor(doctor)
                .patient(patient)
                .appointment(appointment)
                .diagnosis(request.getDiagnosis())
                .instructions(request.getAdditionalInstructions())
                .date(LocalDate.now())
                .build();
        System.out.println("✓ 처방전 엔티티 생성 완료");

        // 약품 정보를 PrescriptionMedicine 엔티티로 변환 후 추가
        request.getMedicines().forEach(medicineItem -> {
            Medicine medicine = medicineRepository.findByName(medicineItem.getMedicineName())
                    .orElseThrow(() -> new IllegalArgumentException("약품을 찾을 수 없습니다: " + medicineItem.getMedicineName()));

            PrescriptionMedicine pm = PrescriptionMedicine.builder()
                    .medicine(medicine)
                    .build();

            prescription.addMedicine(pm);
        });
        System.out.println("✓ 약품 정보 추가 완료 - 약품수: " + request.getMedicines().size());

        Prescription savedPrescription = prescriptionRepository.save(prescription);
        System.out.println("✓ 처방전 DB 저장 완료 - 처방전 ID: " + savedPrescription.getId());
        System.out.println("[처방전 발급] 성공");
        System.out.println("========================================");

        return PrescriptionResponse.from(savedPrescription);
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
