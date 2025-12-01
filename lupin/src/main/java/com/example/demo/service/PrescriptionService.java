package com.example.demo.service;

import com.example.demo.domain.entity.Prescription;
import com.example.demo.repository.PrescriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;

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
        if (roomId.startsWith("appointment_")) {
            Long appointmentId = Long.parseLong(roomId.substring("appointment_".length()));
            return findByAppointmentId(appointmentId);
        }
        return Optional.empty();
    }
}
