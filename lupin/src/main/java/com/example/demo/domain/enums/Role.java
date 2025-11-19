package com.example.demo.domain.enums;

public enum Role {
    MEMBER("회원"),
    DOCTOR("의사");

    private final String description;

    Role(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
