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
    public Prescription issuePrescription(Long appointmentId, Long doctorId, Long patientId, String diagnosis) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
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

        return prescriptionRepository.save(prescription);
    }

    @Transactional
    public PrescriptionResponse createPrescription(Long doctorId, PrescriptionRequest request) {
        Appointment appointment = appointmentRepository.findById(request.getAppointmentId())
                .orElseThrow(() -> new IllegalArgumentException("예약을 찾을 수 없습니다."));

        // 담당 의사 검증
        if (!appointment.getDoctor().getId().equals(doctorId)) {
            throw new IllegalArgumentException("해당 예약의 담당 의사만 처방전을 발행할 수 있습니다.");
        }

        // 환자 정보 검증
        if (!appointment.getPatient().getId().equals(request.getPatientId())) {
            throw new IllegalArgumentException("해당 예약의 환자 정보가 일치하지 않습니다.");
        }

        User doctor = userRepository.findById(doctorId)
                .orElseThrow(() -> new IllegalArgumentException("의사를 찾을 수 없습니다."));

        User patient = userRepository.findById(request.getPatientId())
                .orElseThrow(() -> new IllegalArgumentException("환자를 찾을 수 없습니다."));

        String medicationString = request.getMedicines().stream()
                .map(item -> String.format("%s (%s, %s)",
                        item.getMedicineName(), item.getDosage(), item.getFrequency()))
                .collect(Collectors.joining("\n"));

        // 기존 처방전 확인 - 있으면 업데이트, 없으면 새로 생성
        Optional<Prescription> existingPrescription = prescriptionRepository.findByAppointmentId(request.getAppointmentId());

        Prescription prescription;
        if (existingPrescription.isPresent()) {
            // 기존 처방전 업데이트
            prescription = existingPrescription.get();
            prescription.updateDiagnosis(request.getDiagnosis());
            prescription.updateMedications(medicationString);
        } else {
            // 새 처방전 생성
            prescription = Prescription.builder()
                    .doctor(doctor)
                    .patient(patient)
                    .appointment(appointment)
                    .diagnosis(request.getDiagnosis())
                    .medications(medicationString)
                    .date(LocalDate.now())
                    .build();

            appointment.complete();
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
