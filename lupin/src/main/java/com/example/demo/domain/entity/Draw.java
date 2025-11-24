package com.example.demo.domain.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class Draw {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    private Challenge challenge;

    private boolean used;

    private LocalDateTime issuedAt;

    public void use() {
        if (this.used) throw new IllegalStateException("이미 사용한 티켓입니다.");
        this.used = true;
    }
}
