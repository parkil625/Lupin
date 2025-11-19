package com.example.demo.domain.enums;

public enum ChallengeStatus {
    SCHEDULED("예정"),
    ACTIVE("진행중"),
    CLOSED("종료"),
    COMPLETED("완료");

    private final String description;

    ChallengeStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
