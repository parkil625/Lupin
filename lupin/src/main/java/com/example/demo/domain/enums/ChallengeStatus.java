package com.example.demo.domain.enums;

public enum ChallengeStatus {
    SCHEDULED("예정"),
    OPEN("오픈"),
    CLOSED("종료");

    private final String description;

    ChallengeStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
