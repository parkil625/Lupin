package com.example.demo.service;

import com.example.demo.domain.entity.*;
import com.example.demo.dto.prescription.PrescriptionRequest;
import com.example.demo.dto.prescription.PrescriptionResponse;
import com.example.demo.repository.AppointmentRepository;
import com.example.demo.repository.MedicineRepository;
import com.example.demo.repository.PrescriptionRepository;
import com.example.demo.repository.UserRepository;
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

        // 필수 필드 검증
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
        System.out.println("=== createPrescription 시작 ===");
        System.out.println("doctorId: " + doctorId);
        System.out.println("request.getAppointmentId(): " + request.getAppointmentId());
        System.out.println("request.getPatientId(): " + request.getPatientId());

        Appointment appointment = appointmentRepository.findByIdWithPatientAndDoctor(request.getAppointmentId())
                .orElseThrow(() -> {
                    System.err.println("예약을 찾을 수 없음: appointmentId=" + request.getAppointmentId());
                    return new IllegalArgumentException("예약을 찾을 수 없습니다.");
                });

        System.out.println("예약 찾음: " + appointment.getId());
        System.out.println("예약의 의사 ID: " + appointment.getDoctor().getId());
        System.out.println("예약의 환자 ID: " + appointment.getPatient().getId());

        // 예약 상태 검증 - 진료 중이거나 완료된 예약만 처방전 발급/수정 가능
        if (appointment.getStatus() != AppointmentStatus.IN_PROGRESS &&
            appointment.getStatus() != AppointmentStatus.COMPLETED) {
            System.err.println("예약 상태가 진료 중 또는 완료가 아님: status=" + appointment.getStatus());
            throw new IllegalArgumentException("진료 중이거나 완료된 예약만 처방전을 발급할 수 있습니다.");
        }

        // 담당 의사 검증
        if (!appointment.getDoctor().getId().equals(doctorId)) {
            System.err.println("의사 ID 불일치: expected=" + appointment.getDoctor().getId() + ", actual=" + doctorId);
            throw new IllegalArgumentException("해당 예약의 담당 의사만 처방전을 발행할 수 있습니다.");
        }

        // 환자 정보 검증
        if (!appointment.getPatient().getId().equals(request.getPatientId())) {
            System.err.println("환자 ID 불일치: expected=" + appointment.getPatient().getId() + ", actual=" + request.getPatientId());
            throw new IllegalArgumentException("해당 예약의 환자 정보가 일치하지 않습니다.");
        }

        User doctor = userRepository.findById(doctorId)
                .orElseThrow(() -> new IllegalArgumentException("의사를 찾을 수 없습니다."));

        User patient = userRepository.findById(request.getPatientId())
                .orElseThrow(() -> new IllegalArgumentException("환자를 찾을 수 없습니다."));

        String medicationString = request.getMedicines().stream()
                .map(item -> {
                    StringBuilder sb = new StringBuilder();
                    sb.append(item.getMedicineName());

                    // 용량, 복용 횟수, 복용 기간을 괄호 안에 포함
                    List<String> details = new java.util.ArrayList<>();

                    if (item.getDosage() != null && !item.getDosage().isEmpty()) {
                        details.add(item.getDosage());
                    }

                    if (item.getFrequency() != null && !item.getFrequency().isEmpty()) {
                        details.add(item.getFrequency());
                    }

                    if (item.getDurationDays() != null && item.getDurationDays() > 0) {
                        details.add(item.getDurationDays() + "일");
                    }

                    if (!details.isEmpty()) {
                        sb.append(" (").append(String.join(", ", details)).append(")");
                    }

                    return sb.toString();
                })
                .collect(Collectors.joining("\n"));

        // 기존 처방전 확인 - 있으면 업데이트, 없으면 새로 생성
        Optional<Prescription> existingPrescription = prescriptionRepository.findByAppointmentId(request.getAppointmentId());

        Prescription prescription;
        if (existingPrescription.isPresent()) {
            // 기존 처방전 업데이트
            prescription = existingPrescription.get();
            prescription.updateDiagnosis(request.getDiagnosis());
            prescription.updateMedications(medicationString);
            prescription.updateInstructions(request.getAdditionalInstructions());
        } else {
            // 새 처방전 생성
            prescription = Prescription.builder()
                    .doctor(doctor)
                    .patient(patient)
                    .appointment(appointment)
                    .diagnosis(request.getDiagnosis())
                    .medications(medicationString)
                    .instructions(request.getAdditionalInstructions())
                    .date(LocalDate.now())
                    .build();
        }

        Prescription savedPrescription = prescriptionRepository.save(prescription);

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
