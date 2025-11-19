package com.example.demo.domain.enums;

public enum AppointmentStatus {
    SCHEDULED("예약됨"),
    COMPLETED("완료"),
    CANCELLED("취소됨");

    private final String description;

    AppointmentStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
