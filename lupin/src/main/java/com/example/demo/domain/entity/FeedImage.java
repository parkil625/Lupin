package com.example.demo.domain.entity;

import com.example.demo.domain.enums.ImageType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "feed_image")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class FeedImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "s3_key", nullable = false, columnDefinition = "TEXT")
    private String s3Key;

    @Enumerated(EnumType.STRING)
    @Column(name = "img_type", nullable = false, length = 10)
    private ImageType imgType;

    // 연관관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feed_id", nullable = false)
    @Setter
    private Feed feed;

    // 편의 메서드
    public String getImageUrl() {
        return this.s3Key;
    }
}
