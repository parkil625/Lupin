package com.example.demo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeJoinResponse {
    private boolean success;
    private String message;
    private int currentJoinCount;
}
