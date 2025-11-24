package com.example.demo.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "draw_prizes")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class DrawPrize {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private int totalQuantity;

    private int remainingQuantity;

    private double probability;

    @Version
    private Long version;

    public void decreaseQuantity() {
        if (this.remainingQuantity <= 0) {
            throw new IllegalStateException("재고 소진");
        }
        this.remainingQuantity--;
    }
}
