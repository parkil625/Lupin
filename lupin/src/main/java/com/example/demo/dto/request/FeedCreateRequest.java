package com.example.demo.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class FeedCreateRequest {
    private String activity;
    private String duration;
    private Integer calories;
    private String stats;
    private String content;
}
