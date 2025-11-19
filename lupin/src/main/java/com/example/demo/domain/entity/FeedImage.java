package com.example.demo.domain.entity;

import com.example.demo.domain.enums.ImageType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "feed_image")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class FeedImage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "image_url", nullable = false, columnDefinition = "TEXT")
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "img_type", nullable = false, length = 10)
    private ImageType imgType;

    @Column(name = "taken_at")
    private LocalDateTime takenAt;

    @Column(name = "sort_order")
    private Integer sortOrder;

    // 연관관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feed_id", nullable = false)
    @Setter
    private Feed feed;
}
