package com.example.demo.util;

import java.util.HashMap;
import java.util.Map;

/**
 * 운동 강도(MET) 관리 및 계산 유틸리티
 * - 모든 서비스에서 공통으로 사용
 */
public class MetCalculator {

    private static final Map<String, Double> MET_VALUES = new HashMap<>();

    // 정적 초기화 블록으로 MET 값 세팅
    static {
        // 기본 활동
        MET_VALUES.put("산책", 0.5);
        MET_VALUES.put("요가", 0.5);
        MET_VALUES.put("스트레칭", 0.6);
        MET_VALUES.put("필라테스", 0.6);
        MET_VALUES.put("골프", 0.7);

        // 유산소 & 스포츠
        MET_VALUES.put("빠른 걷기", 0.8);
        MET_VALUES.put("걷기", 0.8); // 호환성용
        MET_VALUES.put("헬스", 0.8);
        MET_VALUES.put("웨이트 트레이닝", 0.8);
        MET_VALUES.put("헬스(웨이트 트레이닝)", 0.8);
        MET_VALUES.put("배드민턴", 0.9);
        MET_VALUES.put("탁구", 0.9);
        MET_VALUES.put("자전거 타기", 1.0);
        MET_VALUES.put("자전거", 1.0); // 호환성용
        MET_VALUES.put("수영", 1.0);
        MET_VALUES.put("등산", 1.0);
        MET_VALUES.put("테니스", 1.1);
        MET_VALUES.put("조깅", 1.2);

        // 고강도
        MET_VALUES.put("축구", 1.4);
        MET_VALUES.put("농구", 1.4);
        MET_VALUES.put("복싱", 1.5);
        MET_VALUES.put("달리기", 1.5);
        MET_VALUES.put("러닝", 1.5); // 호환성용

        // 최고강도
        MET_VALUES.put("수영(빠르게)", 1.7);
        MET_VALUES.put("자전거(빠르게)", 1.7);
        MET_VALUES.put("HIIT", 1.7);
        MET_VALUES.put("크로스핏", 1.7);
        MET_VALUES.put("줄넘기", 1.8);
    }

    /**
     * 활동명으로 MET 값 조회 (기본값 0.5)
     */
    public static double get(String activityType) {
        return MET_VALUES.getOrDefault(activityType, 0.5);
    }
}