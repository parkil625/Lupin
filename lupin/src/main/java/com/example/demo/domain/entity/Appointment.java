package com.example.demo.domain.entity;

import com.example.demo.domain.enums.AppointmentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "appointments", indexes = {
    @Index(name = "idx_appointment_patient", columnList = "patientId"),
    @Index(name = "idx_appointment_doctor", columnList = "doctorId"),
    @Index(name = "idx_appointment_date", columnList = "apptDate"),
    @Index(name = "idx_appointment_status", columnList = "status")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Appointment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, insertable = false, updatable = false)
    private Long patientId;

    @Column(nullable = false, insertable = false, updatable = false)
    private Long doctorId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patientId", nullable = false)
    private User patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctorId", nullable = false)
    private User doctor;

    @Column(name = "appt_date", nullable = false)
    private LocalDateTime apptDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private AppointmentStatus status = AppointmentStatus.SCHEDULED;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Version
    private Long version;

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
