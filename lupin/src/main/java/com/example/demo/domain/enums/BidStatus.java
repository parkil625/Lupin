package com.example.demo.domain.enums;

/**
 * 입찰 상태
 */
public enum BidStatus {
    ACTIVE,     // 현재 최고가 입찰
    OUTBID,     // 다른 사람에게 밀림 (환불 완료)
    WINNING,    // 경매 종료 시 최고가 (낙찰)
    LOST,       // 경매 종료 시 최고가 아님 (환불 완료)
    REFUNDED    // 환불 완료
}
