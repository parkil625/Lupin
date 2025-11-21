package com.example.demo.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PrizeType {
    FIRST_PLACE("1등", 1000000),
    SECOND_PLACE("2등", 500000);

    private final String displayName;
    private final int amount;
}
