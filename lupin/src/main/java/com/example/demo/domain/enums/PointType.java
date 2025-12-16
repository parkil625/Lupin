package com.example.demo.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PointType {
    // 랭킹 집계에 포함되는 유형 (활동 점수)
    EARN("획득"),    // 피드 작성, 좋아요 받음 등
    DEDUCT("차감"),  // 피드 삭제, 어뷰징 패널티 등 (획득했던 포인트 회수)

    // 랭킹 집계에서 제외되는 유형 (단순 소비)
    USE("사용");     // 경매 낙찰, 아이템 구매 등

    private final String description;
}