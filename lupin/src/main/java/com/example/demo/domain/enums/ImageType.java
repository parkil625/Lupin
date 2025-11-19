package com.example.demo.domain.enums;

public enum ImageType {
    START("시작"),
    END("끝"),
    OTHER("기타");

    private final String description;

    ImageType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
