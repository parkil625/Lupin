package com.example.demo.domain.entity;

import com.example.demo.domain.enums.AppointmentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "appointments", indexes = {
    @Index(name = "idx_appointment_patient", columnList = "patient_id"),
    @Index(name = "idx_appointment_doctor", columnList = "doctor_id"),
    @Index(name = "idx_appointment_date", columnList = "date"),
    @Index(name = "idx_appointment_status", columnList = "status")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private User patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private User doctor;

    @Column(name = "date", nullable = false)
    private LocalDateTime date;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private AppointmentStatus status = AppointmentStatus.SCHEDULED;

    public void startConsultation() {
        if (this.status == AppointmentStatus.CANCELLED) {
            throw new IllegalStateException("취소된 예약은 시작할 수 없습니다.");
        }
        if (this.status == AppointmentStatus.IN_PROGRESS) {
            throw new IllegalStateException("이미 진행 중인 예약입니다.");
        }
        if (this.status == AppointmentStatus.COMPLETED) {
            throw new IllegalStateException("이미 완료된 예약입니다.");
        }
        this.status = AppointmentStatus.IN_PROGRESS;
    }

    public void complete() {
        if (this.status == AppointmentStatus.CANCELLED) {
            throw new IllegalStateException("취소된 예약은 완료할 수 없습니다.");
        }
        if (this.status == AppointmentStatus.COMPLETED) {
            throw new IllegalStateException("이미 완료된 예약입니다.");
        }
        this.status = AppointmentStatus.COMPLETED;
    }

    public void cancel() {
        if (this.status == AppointmentStatus.COMPLETED) {
            throw new IllegalStateException("완료된 예약은 취소할 수 없습니다.");
        }
        if (this.status == AppointmentStatus.CANCELLED) {
            throw new IllegalStateException("이미 취소된 예약입니다.");
        }
        this.status = AppointmentStatus.CANCELLED;
    }
}
