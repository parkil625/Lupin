package com.example.demo.domain.entity;

import com.example.demo.domain.enums.AppointmentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "appointment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Appointment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "appt_date", nullable = false)
    private LocalDateTime apptDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private AppointmentStatus status = AppointmentStatus.SCHEDULED;

    @Column(columnDefinition = "TEXT")
    private String reason;

    // 연관관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private User patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id")
    private User doctor;

    // 비즈니스 로직
    public void complete() {
        if (this.status == AppointmentStatus.CANCELLED) {
            throw new IllegalStateException("취소된 예약은 완료할 수 없습니다.");
        }
        this.status = AppointmentStatus.COMPLETED;
    }

    public void cancel() {
        if (this.status == AppointmentStatus.COMPLETED) {
            throw new IllegalStateException("완료된 예약은 취소할 수 없습니다.");
        }
        this.status = AppointmentStatus.CANCELLED;
    }
}
