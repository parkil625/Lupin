package com.example.demo.domain.entity;

import com.example.demo.domain.enums.DrawResult;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "draws", indexes = {
    @Index(name = "idx_draw_user", columnList = "userId")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Draw {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long challengeId;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private DrawResult result = DrawResult.UNUSED;

    private Long prizeId;

    @Version
    private Long version;

    public void use(DrawResult result, Long prizeId) {
        this.result = result;
        this.prizeId = prizeId;
    }
}
