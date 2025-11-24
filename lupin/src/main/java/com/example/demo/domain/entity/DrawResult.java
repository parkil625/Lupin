package com.example.demo.domain.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class DrawResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    private Draw ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    private String reward; // WIN, LOSE, ITEM_A, COUPON 등

    private LocalDateTime drawnAt;
}
