package com.example.demo.domain.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "draw_prize")
public class DrawPrize {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Challenge challenge; // 어떤 챌린지의 상품인지

    @Column(nullable = false, length = 100)
    private String name; // 예: "스타벅스 쿠폰", "10포인트", "꽝"

    @Column(nullable = false)
    private int totalQuantity; // 전체 준비 수량 (무제한이면 -1 같은 규칙도 가능)

    @Column(nullable = false)
    private int remainingQuantity; // 남은 수량

    @Column(nullable = false)
    private int weight; // 당첨 확률 가중치 (10, 30, 60 등)

    public boolean isSoldOut() {
        return totalQuantity > 0 && remainingQuantity <= 0;
    }

    public void decrease() {
        if (isSoldOut()) {
            throw new IllegalStateException("더 이상 남은 수량이 없습니다.");
        }
        this.remainingQuantity--;
    }
}